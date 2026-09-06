package com.palosj.waystonesptpt.network.payload;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.WaystonesPTPT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReceivingStatePayload(UUID session, long acknowledgedChange, boolean ownAllowed,
        List<Entry> entries) implements CustomPacketPayload {
    public record Entry(UUID playerId, boolean allowed) { }
    public static final Type<ReceivingStatePayload> TYPE = new Type<>(WaystonesPTPT.id("receiving_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReceivingStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.session());
                buffer.writeLong(payload.acknowledgedChange());
                buffer.writeBoolean(payload.ownAllowed());
                buffer.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    buffer.writeUUID(entry.playerId());
                    buffer.writeBoolean(entry.allowed());
                }
            }, buffer -> {
                UUID session = buffer.readUUID();
                long acknowledgedChange = buffer.readLong();
                boolean ownAllowed = buffer.readBoolean();
                int count = buffer.readVarInt();
                if (count < 0 || count > ReceivingDirectoryPayload.MAX_ENTRIES) {
                    throw new IllegalArgumentException("Invalid receiving state batch size");
                }
                java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) { entries.add(new Entry(buffer.readUUID(), buffer.readBoolean())); }
                return new ReceivingStatePayload(session, acknowledgedChange, ownAllowed, entries);
            });
    public ReceivingStatePayload { entries = List.copyOf(entries); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
