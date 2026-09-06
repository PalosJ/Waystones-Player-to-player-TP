package com.palosj.waystonesptpt.network;

import java.util.function.Supplier;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.network.payload.RequestPlayerTeleportPayload;
import com.palosj.waystonesptpt.teleport.PlayerTeleportService;
import com.palosj.waystonesptpt.teleport.PlayerReceivingService;
import com.palosj.waystonesptpt.network.payload.ReceivingDirectoryPayload;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;
import com.palosj.waystonesptpt.network.payload.UpdateReceivingPayload;

import net.blay09.mods.balm.api.network.BalmNetworking;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "2";

    private static BalmNetworking activeNetworking;

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        activeNetworking.sendToServer(payload);
    }

    public static void sendToClient(net.minecraft.server.level.ServerPlayer player,
            net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        activeNetworking.sendTo(player, payload);
    }

    private ModNetworking() {
    }

    public static void register(
            BalmNetworking networking,
            Supplier<PlayerTeleportExperienceMode> experienceMode) {
        activeNetworking = networking;
        networking.allowClientAndServerOnly(WaystonesPTPT.MODID);
        networking.registerServerboundPacket(ReceivingDirectoryPayload.TYPE, ReceivingDirectoryPayload.class,
                (buffer, payload) -> ReceivingDirectoryPayload.STREAM_CODEC.encode(buffer, payload),
                ReceivingDirectoryPayload.STREAM_CODEC::decode, PlayerReceivingService::requestDirectory);
        networking.registerServerboundPacket(UpdateReceivingPayload.TYPE, UpdateReceivingPayload.class,
                (buffer, payload) -> UpdateReceivingPayload.STREAM_CODEC.encode(buffer, payload),
                UpdateReceivingPayload.STREAM_CODEC::decode, PlayerReceivingService::updateOwnPreference);
        networking.registerClientboundPacket(ReceivingStatePayload.TYPE, ReceivingStatePayload.class,
                (buffer, payload) -> ReceivingStatePayload.STREAM_CODEC.encode(buffer, payload),
                ReceivingStatePayload.STREAM_CODEC::decode, (player, payload) -> ReceivingClientState.accept(payload));
        networking.registerServerboundPacket(
                RequestPlayerTeleportPayload.TYPE,
                RequestPlayerTeleportPayload.class,
                (buffer, payload) -> RequestPlayerTeleportPayload.STREAM_CODEC.encode(buffer, payload),
                RequestPlayerTeleportPayload.STREAM_CODEC::decode,
                (player, payload) -> PlayerTeleportService.handleRequest(
                        player,
                        payload.targetPlayerId(),
                        experienceMode.get()));
    }
}
