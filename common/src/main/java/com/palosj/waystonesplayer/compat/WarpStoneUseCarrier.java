package com.palosj.waystonesplayer.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface WarpStoneUseCarrier {
    void waystonesplayer$bindWarpStone(ItemStack stack, InteractionHand hand);

    ItemStack waystonesplayer$getWarpStone();

    InteractionHand waystonesplayer$getWarpStoneHand();
}
