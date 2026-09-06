package com.palosj.waystonesptpt.mixin;

import com.palosj.waystonesptpt.compat.PlayerTeleportShogiContext;
import net.blay09.mods.shogi.context.ShogiContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.blay09.mods.shogi.context.internal.ShogiContextImpl", remap = false)
public abstract class PlayerTeleportShogiContextMixin implements PlayerTeleportShogiContext {
    @Shadow @Final private ShogiContext parent;
    @Shadow private ItemStack itemStack;

    @Override
    public boolean waystonesptpt$isPlayerTeleport() {
        return parent instanceof PlayerTeleportShogiContext context && context.waystonesptpt$isPlayerTeleport();
    }

    @Inject(method = "itemStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void waystonesptpt$inheritBoundItem(CallbackInfoReturnable<ItemStack> cir) {
        // Early Shogi otherwise chooses the entity's main hand before consulting the parent.
        // Respect explicit nested item contexts and leave all ordinary Waystones contexts unchanged.
        if (itemStack == null && waystonesptpt$isPlayerTeleport()) {
            cir.setReturnValue(parent.itemStack());
        }
    }
}
