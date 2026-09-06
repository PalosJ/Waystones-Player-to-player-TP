package com.palosj.waystonesptpt.teleport;

import java.util.Set;
import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Codec-based world data for the 1.21.5–1.21.11 storage API. */
public final class PlayerReceivingData extends SavedData {
    static final Codec<PlayerReceivingData> CODEC = Codec.STRING.listOf().xmap(
            values -> new PlayerReceivingData(values.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet())),
            data -> data.preferences.disabledPlayers().stream().sorted().map(UUID::toString).toList())
            .fieldOf("disabled").codec();
    private static final SavedDataType<PlayerReceivingData> TYPE = new SavedDataType<>(
            "waystonesptpt_receiving",
            () -> new PlayerReceivingData(Set.of()), CODEC, null);
    private final PlayerReceivingPreferences preferences;

    PlayerReceivingData(Set<UUID> disabled) { preferences = new PlayerReceivingPreferences(disabled); }
    public static PlayerReceivingData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean allows(UUID playerId) { return preferences.allows(playerId); }
    public boolean setAllowed(UUID playerId, boolean allowed) {
        boolean changed = preferences.setAllowed(playerId, allowed);
        if (changed) { setDirty(); }
        return changed;
    }
}
