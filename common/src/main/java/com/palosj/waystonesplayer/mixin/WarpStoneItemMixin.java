package com.palosj.waystonesplayer.mixin;

import com.palosj.waystonesplayer.compat.WarpStoneUseCarrier;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.waystones.item.WarpStoneItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WarpStoneItem.class)
public abstract class WarpStoneItemMixin {
    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void waystonesplayer$bindOpenedMenu(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            CallbackInfoReturnable<ItemStack> callback) {
        if (level.isClientSide()
                || !(entity instanceof ServerPlayer player)
                || !WaystonesCompat.isWarpStoneMenu(player.containerMenu)
                || !(player.containerMenu instanceof WarpStoneUseCarrier carrier)) {
            return;
        }

        InteractionHand hand = resolveHand(player, stack);
        if (hand != null) {
            carrier.waystonesplayer$bindWarpStone(stack, hand);
        }
    }

    private static InteractionHand resolveHand(ServerPlayer player, ItemStack stack) {
        InteractionHand usedHand = player.getUsedItemHand();
        if (player.getItemInHand(usedHand) == stack) {
            return usedHand;
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND) == stack) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND) == stack) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
