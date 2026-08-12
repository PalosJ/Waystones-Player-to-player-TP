package com.palosj.waystonesplayer.network.payload;

import java.util.UUID;

import com.palosj.waystonesplayer.WaystonesPlayer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestPlayerTeleportPayload(UUID targetPlayerId) implements CustomPacketPayload {
    public static final Type<RequestPlayerTeleportPayload> TYPE = new Type<>(
            WaystonesPlayer.id("request_player_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPlayerTeleportPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> UUIDUtil.STREAM_CODEC.encode(buffer, payload.targetPlayerId()),
                    buffer -> new RequestPlayerTeleportPayload(UUIDUtil.STREAM_CODEC.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
