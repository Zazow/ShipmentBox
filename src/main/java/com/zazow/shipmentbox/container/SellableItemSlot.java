package com.zazow.shipmentbox.container;

import com.zazow.shipmentbox.config.SBConfig;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SellableItemSlot extends SlotItemHandler {

    public SellableItemSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() &&
                getItemHandler().isItemValid(getSlotIndex(), stack) &&
                SBConfig.GENERAL.getPriceMap().containsKey(stack.getItem().getRegistryName().toString());
    }
}
