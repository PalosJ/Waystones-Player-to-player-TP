package com.palosj.waystonesptpt.network;

import java.util.function.Supplier;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.network.payload.RequestPlayerTeleportPayload;
import com.palosj.waystonesptpt.teleport.PlayerTeleportService;

import net.blay09.mods.balm.api.network.BalmNetworking;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(
            BalmNetworking networking,
            Supplier<PlayerTeleportExperienceMode> experienceMode) {
        networking.defineNetworkVersion(WaystonesPTPT.MODID, NETWORK_VERSION);
        networking.registerServerboundPacket(
                RequestPlayerTeleportPayload.TYPE,
                RequestPlayerTeleportPayload.class,
                RequestPlayerTeleportPayload.STREAM_CODEC,
                (player, payload) -> PlayerTeleportService.handleRequest(
                        player,
                        payload.targetPlayerId(),
                        experienceMode.get()));
    }
}
