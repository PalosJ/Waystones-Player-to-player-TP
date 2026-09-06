package com.palosj.waystonesptpt.teleport;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerReceivingDataTest {
    @Test
    void worldSavePreservesOnlyOptOutsAndDoesNotAffectAnotherWorld() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerReceivingData world = new PlayerReceivingData(Set.of());
        assertTrue(world.allows(player));
        assertTrue(world.setAllowed(player, false));
        assertTrue(world.isDirty());
        PlayerReceivingData restored = PlayerReceivingData.load(world.save(new CompoundTag(), null), null);
        assertFalse(restored.allows(player));
        assertTrue(restored.allows(other));
        assertTrue(new PlayerReceivingData(Set.of()).allows(player));
        assertTrue(restored.setAllowed(player, true));
        assertTrue(PlayerReceivingData.load(restored.save(new CompoundTag(), null), null).allows(player));
    }

    @Test
    void duplicatePreferenceDoesNotDirtyAnUnchangedWorld() {
        UUID player = UUID.randomUUID();
        PlayerReceivingData data = new PlayerReceivingData(Set.of(player));
        assertFalse(data.setAllowed(player, false));
        assertFalse(data.isDirty());
    }
}
