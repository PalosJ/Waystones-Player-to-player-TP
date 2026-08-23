package com.palosj.waystonesplayer.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class DurabilityCompat {
    private DurabilityCompat() {
    }

    public static void hurtAndBreak(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        stack.hurtAndBreak(1, player, hand);
    }
}
