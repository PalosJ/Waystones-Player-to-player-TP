package com.palosj.waystonesptpt.teleport;

import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.blay09.mods.balm.platform.event.callback.ServerTickCallback;

public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    public static void register() {
        ServerTickCallback.AFTER.register(server -> {
            PlayerTeleportService.tickPendingRequests();
            PlayerReceivingService.tick(server);
        });
        ServerPlayerCallback.Leave.EVENT.register(
                player -> {
                    PlayerTeleportService.clearCooldown(player.getUUID());
                    PlayerReceivingService.logout(player.getUUID());
                });
    }
}
