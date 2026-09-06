package com.palosj.waystonesptpt.network.payload;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.WaystonesPTPT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The sender can update only their own preference; there is no target UUID. */
public record UpdateReceivingPayload(UUID session, long changeId, boolean allowed) implements CustomPacketPayload {
    public static final Type<UpdateReceivingPayload> TYPE = new Type<>(WaystonesPTPT.id("update_receiving"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateReceivingPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.session());
                buffer.writeLong(payload.changeId());
                buffer.writeBoolean(payload.allowed());
            }, buffer -> new UpdateReceivingPayload(buffer.readUUID(), buffer.readLong(), buffer.readBoolean()));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
