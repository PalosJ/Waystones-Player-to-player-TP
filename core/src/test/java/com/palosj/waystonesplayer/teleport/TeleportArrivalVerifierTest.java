package com.palosj.waystonesplayer.teleport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeleportArrivalVerifierTest {
    private static final TeleportArrivalVerifier.BlockTarget TARGET =
            new TeleportArrivalVerifier.BlockTarget("minecraft:overworld", 10, 64, 10);

    @Test
    void acceptsTheTargetBlockAndHorizontalAdjacentBlocks() {
        var before = position("minecraft:overworld", 0.5, 64.5, 0.5);

        assertTrue(TeleportArrivalVerifier.succeeded(true, before, position("minecraft:overworld", 10.5, 64.5, 10.5), TARGET));
        assertTrue(TeleportArrivalVerifier.succeeded(true, before, position("minecraft:overworld", 11.5, 64.5, 10.5), TARGET));
    }

    @Test
    void rejectsAnOldApiFalsePositiveWhenThePlayerDidNotMove() {
        var unchanged = position("minecraft:overworld", 0.5, 64.5, 0.5);

        assertFalse(TeleportArrivalVerifier.succeeded(true, unchanged, unchanged, TARGET));
    }

    @Test
    void rejectsAnApiSuccessWhenThePlayerWasAlreadyAtTheRequestedArea() {
        var alreadyThere = position("minecraft:overworld", 10.5, 64.5, 10.5);

        assertFalse(TeleportArrivalVerifier.succeeded(true, alreadyThere, alreadyThere, TARGET));
    }

    @Test
    void acceptsAnEventRedirectThatActuallyMovedThePlayer() {
        var before = position("minecraft:overworld", 0.5, 64.5, 0.5);
        var redirected = position("minecraft:the_nether", 100.5, 70.5, 100.5);

        assertTrue(TeleportArrivalVerifier.succeeded(true, before, redirected, TARGET));
    }

    @Test
    void neverTrustsMovementWithoutAnApiReportedSender() {
        var before = position("minecraft:overworld", 0.5, 64.5, 0.5);
        var after = position("minecraft:overworld", 10.5, 64.5, 10.5);

        assertFalse(TeleportArrivalVerifier.succeeded(false, before, after, TARGET));
    }

    @Test
    void detectsMovementIndependentlyForPostTeleportExceptions() {
        var before = position("minecraft:overworld", 0.5, 64.5, 0.5);

        assertFalse(TeleportArrivalVerifier.hasMoved(before, before));
        assertTrue(TeleportArrivalVerifier.hasMoved(
                before,
                position("minecraft:the_nether", 0.5, 64.5, 0.5)));
    }

    private static TeleportArrivalVerifier.Position position(String dimension, double x, double y, double z) {
        return new TeleportArrivalVerifier.Position(dimension, x, y, z);
    }
}
