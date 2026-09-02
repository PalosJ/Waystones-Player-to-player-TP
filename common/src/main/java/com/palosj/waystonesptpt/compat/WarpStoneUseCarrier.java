package com.palosj.waystonesptpt.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface WarpStoneUseCarrier {
    void waystonesptpt$bindWarpStone(ItemStack stack, InteractionHand hand);

    ItemStack waystonesptpt$getWarpStone();

    InteractionHand waystonesptpt$getWarpStoneHand();
}
