package com.palosj.waystonesptpt.mixin;

import java.util.concurrent.CompletableFuture;
import com.palosj.waystonesptpt.compat.WaystonesAsyncCompat;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.blay09.mods.waystones.core.WaystoneTeleportManager", remap = false)
public abstract class PlayerTeleportPreparationMixin {
    // Earlier minimum versions have no async preparation API. Their synchronous path is guarded separately.
    @Inject(method = "loadDestinationChunks(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lnet/blay09/mods/waystones/api/WaystoneTeleportContext;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void waystonesptpt$serverContinuation(MinecraftServer server, ServerLevel level,
            WaystoneTeleportContext context, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (WaystonesAsyncCompat.isPlayerTeleport(context)) {
            cir.setReturnValue(WaystonesAsyncCompat.onServerThread(context, cir.getReturnValue()));
        }
    }

    @Inject(method = "crashOnUnexpectedAsyncFailure(Lnet/blay09/mods/waystones/api/WaystoneTeleportContext;Ljava/lang/Throwable;)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void waystonesptpt$ownFailure(WaystoneTeleportContext context, Throwable error, CallbackInfo ci) {
        if (WaystonesAsyncCompat.isPlayerTeleport(context)) {
            // The service observes this same exceptional future, restores its charge and reports the error.
            ci.cancel();
        }
    }
}
