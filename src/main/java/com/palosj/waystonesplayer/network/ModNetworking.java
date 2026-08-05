package com.palosj.waystonesplayer.network;

import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;
import com.palosj.waystonesplayer.teleport.PlayerTeleportService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION).executesOn(HandlerThread.MAIN);
        registrar.playToServer(
                RequestPlayerTeleportPayload.TYPE,
                RequestPlayerTeleportPayload.STREAM_CODEC,
                ModNetworking::handlePlayerTeleportRequest);
    }

    private static void handlePlayerTeleportRequest(RequestPlayerTeleportPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            PlayerTeleportService.handleRequest(serverPlayer, payload.targetPlayerId());
        }
    }
}
