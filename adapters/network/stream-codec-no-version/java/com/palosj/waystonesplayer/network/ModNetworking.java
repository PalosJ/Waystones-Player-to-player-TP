package com.palosj.waystonesplayer.network;

import java.util.function.Supplier;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;
import com.palosj.waystonesplayer.teleport.PlayerTeleportService;

import net.blay09.mods.balm.api.network.BalmNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(
            BalmNetworking networking,
            Supplier<PlayerTeleportExperienceMode> experienceMode) {
        networking.allowClientAndServerOnly(WaystonesPlayer.MODID);
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
