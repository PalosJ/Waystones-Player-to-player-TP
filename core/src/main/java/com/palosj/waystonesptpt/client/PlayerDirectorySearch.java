package com.palosj.waystonesptpt.client;

import java.util.List;
import java.util.Locale;

public final class PlayerDirectorySearch {
    private PlayerDirectorySearch() {
    }

    public static List<PlayerDirectoryEntry> filter(List<PlayerDirectoryEntry> players, String query) {
        if (query.isEmpty()) {
            return players;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return players.stream()
                .filter(player -> player.name().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }
}
