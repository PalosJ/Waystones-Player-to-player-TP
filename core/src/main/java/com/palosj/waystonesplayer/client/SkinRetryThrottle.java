package com.palosj.waystonesplayer.client;

public final class SkinRetryThrottle {
    private final int retryIntervalTicks;
    private int ticksUntilRetry;

    public SkinRetryThrottle(int retryIntervalTicks) {
        if (retryIntervalTicks <= 0) {
            throw new IllegalArgumentException("retryIntervalTicks must be positive");
        }
        this.retryIntervalTicks = retryIntervalTicks;
    }

    public boolean advanceAndIsReady() {
        if (ticksUntilRetry > 0) {
            ticksUntilRetry--;
        }
        return ticksUntilRetry == 0;
    }

    public void delayAfterFailure() {
        ticksUntilRetry = retryIntervalTicks;
    }

    public void reset() {
        ticksUntilRetry = 0;
    }
}
