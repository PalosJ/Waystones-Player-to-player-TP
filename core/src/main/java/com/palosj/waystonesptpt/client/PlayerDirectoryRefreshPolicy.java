package com.palosj.waystonesptpt.client;

public final class PlayerDirectoryRefreshPolicy {
    public static final int REFRESH_INTERVAL_TICKS = 5;

    private PlayerDirectoryRefreshPolicy() {
    }

    public static boolean shouldRefresh(
            boolean initialized,
            boolean connectionChanged,
            long currentTick,
            long lastRefreshTick) {
        if (!initialized || connectionChanged) {
            return true;
        }
        long elapsed = currentTick - lastRefreshTick;
        return elapsed < 0 || elapsed >= REFRESH_INTERVAL_TICKS;
    }
}
