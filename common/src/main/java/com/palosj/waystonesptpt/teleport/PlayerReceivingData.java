package com.palosj.waystonesptpt.teleport;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned data; deliberately separate from loader-specific experience configuration. */
public final class PlayerReceivingData extends SavedData {
    private static final Factory<PlayerReceivingData> FACTORY = new Factory<>(
            () -> new PlayerReceivingData(Set.of()), PlayerReceivingData::load, null);
    private final PlayerReceivingPreferences preferences;

    PlayerReceivingData(Set<UUID> disabled) {
        preferences = new PlayerReceivingPreferences(disabled);
    }

    public static PlayerReceivingData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "waystonesptpt_receiving");
    }

    public boolean allows(UUID playerId) {
        return preferences.allows(playerId);
    }

    public boolean setAllowed(UUID playerId, boolean allowed) {
        boolean changed = preferences.setAllowed(playerId, allowed);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    static PlayerReceivingData load(CompoundTag tag, HolderLookup.Provider registries) {
        Set<UUID> disabled = new HashSet<>();
        ListTag entries = tag.getList("disabled", Tag.TAG_STRING);
        for (int index = 0; index < entries.size(); index++) {
            disabled.add(UUID.fromString(entries.getString(index)));
        }
        return new PlayerReceivingData(disabled);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        preferences.disabledPlayers().stream().sorted().forEach(id -> entries.add(StringTag.valueOf(id.toString())));
        tag.put("disabled", entries);
        return tag;
    }
}
