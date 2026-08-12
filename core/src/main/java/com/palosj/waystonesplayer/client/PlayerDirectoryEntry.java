package com.palosj.waystonesplayer.client;

import java.util.Objects;
import java.util.UUID;

public record PlayerDirectoryEntry(UUID id, String name) {
    public PlayerDirectoryEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
