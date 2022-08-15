package com.zazow.shipmentbox.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ShipmentBoxBlockItem extends BlockItem {

    public ShipmentBoxBlockItem(Block block) {
        super(block, (new Properties()).tab(CreativeModeTab.TAB_DECORATIONS));
    }

//    @Override
//    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
//        boolean out = super.placeBlock(context, state);
//        if (out) {
//            state.getBlock().setPlacedBy(context.getLevel(), context.getClickedPos(), state, context.getPlayer(), context.getItemInHand());
//        }
//        return out;
//    }
}
