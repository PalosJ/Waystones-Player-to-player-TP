package com.palosj.waystonesptpt.teleport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Stores only opt-outs; new players, including server-registered fake players, allow incoming teleportation. */
public final class PlayerReceivingPreferences {
    private final Set<UUID> disabled;

    public PlayerReceivingPreferences(Collection<UUID> disabled) {
        this.disabled = new HashSet<>(disabled);
    }

    public boolean allows(UUID playerId) {
        return !disabled.contains(playerId);
    }

    public boolean setAllowed(UUID playerId, boolean allowed) {
        return allowed ? disabled.remove(playerId) : disabled.add(playerId);
    }

    public Set<UUID> disabledPlayers() {
        return Set.copyOf(disabled);
    }
}
