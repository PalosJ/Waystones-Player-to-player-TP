package com.palosj.waystonesptpt.teleport;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerReceivingDataTest {
    @SuppressWarnings("unchecked")
    private static PlayerReceivingData roundTrip(PlayerReceivingData data) {
        try {
            try {
                var codec = (com.mojang.serialization.Codec<PlayerReceivingData>)
                        PlayerReceivingData.class.getDeclaredField("CODEC").get(null);
                var tag = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, data).getOrThrow();
                return codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
            } catch (NoSuchFieldException legacyStorage) {
                var save = PlayerReceivingData.class.getDeclaredMethod("save", CompoundTag.class,
                        net.minecraft.core.HolderLookup.Provider.class);
                var load = PlayerReceivingData.class.getDeclaredMethod("load", CompoundTag.class,
                        net.minecraft.core.HolderLookup.Provider.class);
                return (PlayerReceivingData) load.invoke(null, save.invoke(data, new CompoundTag(), null), null);
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not exercise the selected world storage adapter", error);
        }
    }
    @Test
    void worldSavePreservesOnlyOptOutsAndDoesNotAffectAnotherWorld() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerReceivingData world = new PlayerReceivingData(Set.of());
        assertTrue(world.allows(player));
        assertTrue(world.setAllowed(player, false));
        assertTrue(world.isDirty());
        PlayerReceivingData restored = roundTrip(world);
        assertFalse(restored.allows(player));
        assertTrue(restored.allows(other));
        assertTrue(new PlayerReceivingData(Set.of()).allows(player));
        assertTrue(restored.setAllowed(player, true));
        assertTrue(roundTrip(restored).allows(player));
    }

    @Test
    void duplicatePreferenceDoesNotDirtyAnUnchangedWorld() {
        UUID player = UUID.randomUUID();
        PlayerReceivingData data = new PlayerReceivingData(Set.of(player));
        assertFalse(data.setAllowed(player, false));
        assertFalse(data.isDirty());
    }
}
