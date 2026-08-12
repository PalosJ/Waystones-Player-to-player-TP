package com.palosj.waystonesplayer.client;

import java.util.List;
import java.util.UUID;

public final class PlayerListRefresh {
    private PlayerListRefresh() {
    }

    public static boolean hasChanged(List<PlayerDirectoryEntry> previous, List<PlayerDirectoryEntry> current) {
        return !previous.equals(current);
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
}
