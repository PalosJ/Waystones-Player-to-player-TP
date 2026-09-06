package com.palosj.waystonesptpt.neoforge.mixin;

import com.palosj.waystonesptpt.neoforge.NeoForgeConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerLifecycleHooks.class, remap = false)
public abstract class ServerConfigMigrationMixin {
    @Inject(method = "handleServerAboutToStart", at = @At("HEAD"), remap = false)
    private static void waystonesptpt$migrateBeforeConfigLoad(MinecraftServer server, CallbackInfo callback) {
        NeoForgeConfig.migrateWorldConfig(server);
    }
}
