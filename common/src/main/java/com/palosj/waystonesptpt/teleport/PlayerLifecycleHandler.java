package com.palosj.waystonesptpt.teleport;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;

public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    public static void register(BalmEvents events) {
        events.onEvent(PlayerLogoutEvent.class,
                event -> {
                    PlayerTeleportService.clearCooldown(event.getPlayer().getUUID());
                    PlayerReceivingService.logout(event.getPlayer().getUUID());
                });
    }
}
