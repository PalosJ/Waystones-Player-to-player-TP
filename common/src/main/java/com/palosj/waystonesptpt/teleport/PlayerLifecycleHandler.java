package com.palosj.waystonesptpt.teleport;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.balm.api.event.TickPhase;

public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    public static void register(BalmEvents events) {
        events.onTickEvent(TickType.Server, TickPhase.End, server -> PlayerTeleportService.tickPendingRequests());
        events.onEvent(PlayerLogoutEvent.class,
                event -> PlayerTeleportService.clearCooldown(event.getPlayer().getUUID()));
    }
}
