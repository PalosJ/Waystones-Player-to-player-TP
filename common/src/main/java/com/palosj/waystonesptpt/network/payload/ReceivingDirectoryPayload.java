package com.palosj.waystonesptpt.network.payload;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.WaystonesPTPT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Requests states only for UUIDs already present in the client's existing directory. */
public record ReceivingDirectoryPayload(UUID session, boolean replace, List<UUID> playerIds) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;
    public static final Type<ReceivingDirectoryPayload> TYPE = new Type<>(WaystonesPTPT.id("receiving_directory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReceivingDirectoryPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.session());
                buffer.writeBoolean(payload.replace());
                buffer.writeVarInt(payload.playerIds().size());
                payload.playerIds().forEach(buffer::writeUUID);
            }, buffer -> {
                UUID session = buffer.readUUID();
                boolean replace = buffer.readBoolean();
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_ENTRIES) {
                    throw new IllegalArgumentException("Invalid receiving directory batch size");
                }
                java.util.ArrayList<UUID> ids = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) { ids.add(buffer.readUUID()); }
                return new ReceivingDirectoryPayload(session, replace, ids);
            });
    public ReceivingDirectoryPayload {
        playerIds = List.copyOf(playerIds);
        if (playerIds.size() > MAX_ENTRIES) { throw new IllegalArgumentException("Receiving directory batch too large"); }
    }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
