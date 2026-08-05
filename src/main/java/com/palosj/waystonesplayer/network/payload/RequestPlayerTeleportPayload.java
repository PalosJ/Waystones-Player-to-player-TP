package com.palosj.waystonesplayer.network.payload;

import java.util.UUID;

import com.palosj.waystonesplayer.WaystonesPlayer;
import io.netty.buffer.ByteBuf;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestPlayerTeleportPayload(UUID targetPlayerId) implements CustomPacketPayload {
    public static final Type<RequestPlayerTeleportPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WaystonesPlayer.MODID, "request_player_teleport"));
    public static final StreamCodec<ByteBuf, RequestPlayerTeleportPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            RequestPlayerTeleportPayload::targetPlayerId,
            RequestPlayerTeleportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
