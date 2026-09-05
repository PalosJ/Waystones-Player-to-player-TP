package com.palosj.waystonesptpt.teleport;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeleportAttemptTest {
    @Test
    void timeoutFencesLateCallbacksAtTheExactDeadline() {
        TeleportAttempt attempt = new TeleportAttempt(100, () -> true);
        assertTrue(attempt.validatePreparation(299));
        assertFalse(attempt.validatePreparation(300));
        assertThrows(IllegalStateException.class, () -> attempt.beginCommit(301));
        assertFalse(attempt.settle());
    }

    @Test
    void closedOrReplacedSessionCannotBecomeValidAgain() {
        AtomicBoolean valid = new AtomicBoolean(true);
        TeleportAttempt attempt = new TeleportAttempt(0, valid::get);
        valid.set(false);
        assertFalse(attempt.validatePreparation(1));
        valid.set(true);
        assertThrows(IllegalStateException.class, () -> attempt.beginCommit(2));
    }

    @Test
    void committedMovementSettlesOnlyOnceAndCannotBeCancelledAfterward() {
        TeleportAttempt attempt = new TeleportAttempt(0, () -> true);
        attempt.setDurabilityCost(80);
        attempt.beginCommit(1);
        attempt.cancel();
        assertEquals(TeleportAttempt.State.COMMITTING, attempt.state());
        assertEquals(80, attempt.durabilityCost());
        assertThrows(IllegalStateException.class, () -> attempt.setDurabilityCost(1));
        assertThrows(IllegalStateException.class, () -> attempt.beginCommit(2));
        assertTrue(attempt.settle());
        assertFalse(attempt.settle());
    }

    @Test
    void cancellationDoesNotAffectANewRequestForTheSamePlayer() {
        TeleportAttempt old = new TeleportAttempt(0, () -> true);
        old.cancel();
        TeleportAttempt next = new TeleportAttempt(1, () -> true);
        next.beginCommit(2);
        assertFalse(old.settle());
        assertTrue(next.settle());
    }

    @Test
    void tickCounterWrapIsAllowedButAResetInvalidatesPreparation() {
        TeleportAttempt wrapping = new TeleportAttempt(Integer.MAX_VALUE - 5, () -> true);
        assertTrue(wrapping.validatePreparation(Integer.MIN_VALUE + 5));
        assertFalse(new TeleportAttempt(100, () -> true).validatePreparation(0));
    }
}
