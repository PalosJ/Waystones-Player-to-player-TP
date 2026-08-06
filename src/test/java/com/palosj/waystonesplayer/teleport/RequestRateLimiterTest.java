package com.palosj.waystonesplayer.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void acceptsTheFirstRequestAndRejectsRequestsInsideTheWindow() {
        RequestRateLimiter limiter = new RequestRateLimiter(10);

        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 100));
        assertEquals(RequestRateLimiter.Result.REJECTED_NOTIFY, limiter.acquire(PLAYER, 101));
        assertEquals(RequestRateLimiter.Result.REJECTED_SILENT, limiter.acquire(PLAYER, 109));
        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 110));
    }

    @Test
    void tracksPlayersIndependently() {
        RequestRateLimiter limiter = new RequestRateLimiter(10);
        UUID otherPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 100));
        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(otherPlayer, 100));
    }

    @Test
    void clearAllowsAnImmediateRequest() {
        RequestRateLimiter limiter = new RequestRateLimiter(10);

        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 100));
        limiter.clear(PLAYER);
        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 101));
    }

    @Test
    void aResetServerTickDoesNotLeaveAStaleLockout() {
        RequestRateLimiter limiter = new RequestRateLimiter(10);

        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 10_000));
        assertEquals(RequestRateLimiter.Result.ACCEPTED, limiter.acquire(PLAYER, 0));
    }
}
