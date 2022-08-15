package com.zazow.shipmentbox.block.entity;

import com.mojang.datafixers.util.Pair;
import com.zazow.shipmentbox.Economy;
import com.zazow.shipmentbox.SBMod;
import com.zazow.shipmentbox.config.SBConfig;
import com.zazow.shipmentbox.container.ShipmentBoxContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShipmentBoxBlockEntity extends BaseContainerBlockEntity {

    public static int CONTAINER_SIZE = 27;

    private final ItemStackHandler items;
    private boolean requiresUpdate;
    private long timer;
    private long lastRewardedDay = -1;
    public UUID placedByUUID;

    public ShipmentBoxBlockEntity(BlockPos pos, BlockState state) {
        super(SBMod.SHIPMENT_BOX_BLOCK_ENTITY.get(), pos, state);
        this.items = new ItemStackHandler(CONTAINER_SIZE) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ShipmentBoxBlockEntity.this.update();
                return super.extractItem(slot, amount, simulate);
            }
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                ShipmentBoxBlockEntity.this.update();
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("shipmentbox:shipmentbox");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return ShipmentBoxContainer.getServerContainer(
                this, this.getBlockPos()).createMenu(containerId, playerInventory, null);
    }

    void updateBlockState(BlockState p_58607_, boolean p_58608_) {
        this.level.setBlock(this.getBlockPos(), p_58607_.setValue(BarrelBlock.OPEN, Boolean.valueOf(p_58608_)), 3);
    }

    void playSound(BlockState state, SoundEvent soundEvent) {
        Vec3i vec3i = state.getValue(BarrelBlock.FACING).getNormal();
        double d0 = (double)this.worldPosition.getX() + 0.5D + (double)vec3i.getX() / 2.0D;
        double d1 = (double)this.worldPosition.getY() + 0.5D + (double)vec3i.getY() / 2.0D;
        double d2 = (double)this.worldPosition.getZ() + 0.5D + (double)vec3i.getZ() / 2.0D;
        this.level.playSound((Player)null, d0, d1, d2, soundEvent, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag());
    }

    public static <T> void ticker(Level level, BlockPos pos, BlockState state, T blockEntity) {
        ((ShipmentBoxBlockEntity)blockEntity).tick();
    }
    public void tick() {
        ++timer;
        if (this.requiresUpdate && this.level != null) {
            update();
            this.requiresUpdate = false;
        }

        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (timer % 20 != 0) {
            return;
        }

        long currentDay = serverLevel.getDayTime() / 24000;
        if (lastRewardedDay == -1) {
            lastRewardedDay = currentDay;
        }

        if (lastRewardedDay < currentDay) {
            System.out.println("SHOULD GET REWARD!");
            processRewards(currentDay);
            lastRewardedDay = currentDay;
        }
    }

//    public int getSlotForItemCount(String item, int count) {
//        for (int i = 0; i < items.getSlots(); ++i) {
//
//        }
//    }

    public int getEmptySlot() {
        for (int i = 0; i < items.getSlots(); ++i) {
            if (items.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int tryAddReward(SBConfig.General.PriceConfig reward, ItemStack stack, int maxAllowedToSell) {
        // Try to add the reward to the current inventory. If not enough space, add as much as possible.
        Item rewardItem = Registry.ITEM.get(new ResourceLocation(reward.reward));
        if (rewardItem.getRegistryName().equals(Registry.ITEM.getDefaultKey())) {
            return 0;
        }

        int maxStackSize = rewardItem.getMaxStackSize();
        int availableCount = 0;
        for (int i = 0; i < items.getSlots(); ++i) {
            ItemStack containerStack = items.getStackInSlot(i);
            if (containerStack.isEmpty()) {
                availableCount += maxStackSize;
            } else if (containerStack.is(rewardItem)) {
                availableCount += maxStackSize - containerStack.getCount();
            }
        }

        int numToConsume = Math.min(
                maxAllowedToSell,
                Math.min(
                    (availableCount / reward.price),
                    stack.getCount()
                )
        );

        int numBundlesToConsume = numToConsume / reward.count;

        if (numBundlesToConsume == 0) {
            return 0;
        }

        int remainingReward = numBundlesToConsume * reward.price;
        for (int i = 0; i < items.getSlots(); ++i) {
            if (remainingReward <= 0) {
                break;
            }
            ItemStack containerStack = items.getStackInSlot(i);
            if (containerStack.isEmpty()) {
                ItemStack rewardedStack = new ItemStack(rewardItem, Math.min(remainingReward, maxStackSize));
                items.insertItem(i, rewardedStack, false);
                remainingReward -= rewardedStack.getCount();
                continue;
            }
            if (containerStack.is(rewardItem)) {
                int amountToAdd = Math.min(remainingReward, maxStackSize - containerStack.getCount());
                containerStack.setCount(containerStack.getCount() + amountToAdd);
                remainingReward -= amountToAdd;
            }
        }

        stack.shrink(numBundlesToConsume * reward.count);
        requiresUpdate = true;
        return numBundlesToConsume * reward.count;
    }
    public void processRewards(long day) {
        Map<String, Integer> numRemainingItemsToConsume = new HashMap<>();

        for (int i = 0; i < items.getSlots(); ++i) {
            ItemStack stack = items.getStackInSlot(i);
            String stackKey = stack.getItem().getRegistryName().toString();
            if (!SBConfig.GENERAL.getPriceMap().containsKey(stackKey)) {
                continue;
            }
            SBConfig.General.PriceConfig reward = SBConfig.GENERAL.getPriceMap().get(stackKey);

            if (stack.getCount() < reward.count) {
                continue;
            }

            int maxAllowedToSell;
            if (numRemainingItemsToConsume.containsKey(stackKey)) {
                maxAllowedToSell = numRemainingItemsToConsume.get(stackKey);
            } else {
                maxAllowedToSell = Economy.get().getMaxAllowedToSell(placedByUUID, stackKey, day);
                numRemainingItemsToConsume.put(stackKey, maxAllowedToSell);
            }

            if (maxAllowedToSell <= 0) {
                continue;
            }

            int itemsConsumed = tryAddReward(reward, stack, maxAllowedToSell);
            Economy.get().onItemSold(placedByUUID, stackKey, itemsConsumed, day);
            numRemainingItemsToConsume.put(stackKey, maxAllowedToSell - itemsConsumed);
        }
    }

    public void update() {
        requestModelDataUpdate();
        setChanged();
        if (this.level != null) {
            this.level.setBlockAndUpdate(this.worldPosition, getBlockState());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return serializeNBT();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < items.getSlots(); ++i) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        this.requiresUpdate = true;
        return this.items.extractItem(index, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_18951_) {
        return null;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        final ItemStack copy = stack.copy();
        stack.shrink(copy.getCount());
        this.requiresUpdate = true;
        this.items.insertItem(index, copy, false);
    }

    @Override
    public boolean stillValid(Player p_18946_) {
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.deserializeNBT(tag.getCompound("Inventory"));
        this.lastRewardedDay = tag.getLong("LastRewardedDay");
        this.placedByUUID = tag.getUUID("PlacedBy");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.items.serializeNBT());
        tag.putLong("LastRewardedDay", this.lastRewardedDay);
        tag.putUUID("PlacedBy", this.placedByUUID);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.getSlots(); ++i) {
            items.getStackInSlot(i).setCount(0);
        }
    }
}
