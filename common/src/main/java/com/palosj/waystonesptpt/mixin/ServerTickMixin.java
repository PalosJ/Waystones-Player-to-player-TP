package com.palosj.waystonesptpt.mixin;

import com.palosj.waystonesptpt.teleport.PlayerReceivingService;
import com.palosj.waystonesptpt.teleport.PlayerTeleportService;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps lifecycle validation on the server thread across legacy Balm event bridges. */
@Mixin(MinecraftServer.class)
public abstract class ServerTickMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void waystonesptpt$validatePlayerRequests(CallbackInfo callback) {
        PlayerTeleportService.tickPendingRequests();
        PlayerReceivingService.tick((MinecraftServer) (Object) this);
    }
}
