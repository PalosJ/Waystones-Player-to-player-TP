package com.palosj.waystonesplayer.client;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerListRefresh {
    private PlayerListRefresh() {
    }

    public static boolean hasChanged(List<PlayerDirectoryEntry> previous, List<PlayerDirectoryEntry> current) {
        return !previous.equals(current);
    }

    public static boolean hasSamePlayersIgnoringOrder(
            List<PlayerDirectoryEntry> previous,
            List<PlayerDirectoryEntry> current) {
        if (previous.size() != current.size()) {
            return false;
        }

        Map<UUID, String> previousNames = new HashMap<>();
        for (PlayerDirectoryEntry entry : previous) {
            if (previousNames.put(entry.id(), entry.name()) != null) {
                return false;
            }
        }
        for (PlayerDirectoryEntry entry : current) {
            String previousName = previousNames.remove(entry.id());
            if (previousName == null || !previousName.equals(entry.name())) {
                return false;
            }
        }
        return previousNames.isEmpty();
    }

    public static double restoreScrollAmount(
            List<PlayerDirectoryEntry> previous,
            List<PlayerDirectoryEntry> current,
            double previousScrollAmount,
            int rowHeight) {
        if (current.isEmpty() || rowHeight <= 0) {
            return 0;
        }

        int previousIndex = Math.max(0, (int) Math.floor(previousScrollAmount / rowHeight));
        double rowOffset = Math.max(0, previousScrollAmount - (double) previousIndex * rowHeight);
        int targetIndex = Math.min(previousIndex, current.size() - 1);

        if (previousIndex < previous.size()) {
            UUID anchorId = previous.get(previousIndex).id();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id().equals(anchorId)) {
                    targetIndex = i;
                    break;
                }
            }
        }

        return (double) targetIndex * rowHeight + Math.min(rowOffset, Math.nextDown((double) rowHeight));
    }

    public static UUID restoreFocusedPlayer(UUID previousFocus, List<PlayerDirectoryEntry> current) {
        if (previousFocus == null) {
            return null;
        }
        return current.stream().anyMatch(entry -> entry.id().equals(previousFocus)) ? previousFocus : null;
    }

    public static UUID restoreFocusedPlayer(
            UUID previousFocus,
            List<PlayerDirectoryEntry> previous,
            List<PlayerDirectoryEntry> current) {
        UUID survivingFocus = restoreFocusedPlayer(previousFocus, current);
        if (survivingFocus != null || previousFocus == null || current.isEmpty()) {
            return survivingFocus;
        }

        for (int index = 0; index < previous.size(); index++) {
            if (previous.get(index).id().equals(previousFocus)) {
                return current.get(Math.min(index, current.size() - 1)).id();
            }
        }
        return null;
    }
}
