package com.palosj.waystonesptpt.teleport;

import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;

public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    public static void register() {
        ServerPlayerCallback.Leave.EVENT.register(
                player -> PlayerTeleportService.clearCooldown(player.getUUID()));
    }
}
