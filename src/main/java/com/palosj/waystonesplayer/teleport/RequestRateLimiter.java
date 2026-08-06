package com.palosj.waystonesplayer.teleport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RequestRateLimiter {
    private final int cooldownTicks;
    private final Map<UUID, RequestState> requestStates = new HashMap<>();

    RequestRateLimiter(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    Result acquire(UUID playerId, int currentTick) {
        RequestState state = requestStates.get(playerId);
        if (state == null || !isInsideWindow(currentTick, state.lastAcceptedTick())) {
            requestStates.put(playerId, new RequestState(currentTick, null));
            return Result.ACCEPTED;
        }

        if (state.lastNotificationTick() == null
                || !isInsideWindow(currentTick, state.lastNotificationTick())) {
            requestStates.put(playerId, new RequestState(state.lastAcceptedTick(), currentTick));
            return Result.REJECTED_NOTIFY;
        }
        return Result.REJECTED_SILENT;
    }

    void clear(UUID playerId) {
        requestStates.remove(playerId);
    }

    private boolean isInsideWindow(int currentTick, int previousTick) {
        int elapsedTicks = currentTick - previousTick;
        return elapsedTicks >= 0 && elapsedTicks < cooldownTicks;
    }

    enum Result {
        ACCEPTED,
        REJECTED_NOTIFY,
        REJECTED_SILENT
    }

    private record RequestState(int lastAcceptedTick, Integer lastNotificationTick) {
    }
}
