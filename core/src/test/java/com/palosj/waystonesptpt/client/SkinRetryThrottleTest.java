package com.palosj.waystonesptpt.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkinRetryThrottleTest {
    @Test
    void retriesNoMoreThanOncePerTwentyTicksAfterFailure() {
        SkinRetryThrottle throttle = new SkinRetryThrottle(20);
        assertTrue(throttle.advanceAndIsReady());
        throttle.delayAfterFailure();

        for (int tick = 1; tick < 20; tick++) {
            assertFalse(throttle.advanceAndIsReady(), "unexpected retry at tick " + tick);
        }
        assertTrue(throttle.advanceAndIsReady());
    }

    @Test
    void resetAllowsImmediateIdentityRefresh() {
        SkinRetryThrottle throttle = new SkinRetryThrottle(20);
        throttle.delayAfterFailure();
        throttle.reset();
        assertTrue(throttle.advanceAndIsReady());
    }

    @Test
    void rejectsAnInvalidRetryInterval() {
        assertThrows(IllegalArgumentException.class, () -> new SkinRetryThrottle(0));
    }
}
