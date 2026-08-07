package com.palosj.waystonesplayer.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TeleportTransactionTest {
    @Test
    void successfulTeleportConsumesWithoutRollback() {
        TrackingCost cost = new TrackingCost(true);

        TeleportTransaction.Result result = TeleportTransaction.execute(cost, () -> true);

        assertEquals(TeleportTransaction.Result.SUCCESS, result);
        assertEquals(1, cost.consumed);
        assertEquals(0, cost.rolledBack);
    }

    @Test
    void unaffordableTeleportDoesNotRunOrConsume() {
        TrackingCost cost = new TrackingCost(false);
        int[] teleportCalls = { 0 };

        TeleportTransaction.Result result = TeleportTransaction.execute(cost, () -> {
            teleportCalls[0]++;
            return true;
        });

        assertEquals(TeleportTransaction.Result.UNAFFORDABLE, result);
        assertEquals(0, teleportCalls[0]);
        assertEquals(0, cost.consumed);
        assertEquals(0, cost.rolledBack);
    }

    @Test
    void failedTeleportRollsBackTheCost() {
        TrackingCost cost = new TrackingCost(true);

        TeleportTransaction.Result result = TeleportTransaction.execute(cost, () -> false);

        assertEquals(TeleportTransaction.Result.FAILED, result);
        assertEquals(1, cost.consumed);
        assertEquals(1, cost.rolledBack);
    }

    @Test
    void exceptionalTeleportRollsBackAndPreservesTheError() {
        TrackingCost cost = new TrackingCost(true);
        RuntimeException expected = new RuntimeException("teleport failed");

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> TeleportTransaction.execute(cost, () -> {
                    throw expected;
                }));

        assertSame(expected, actual);
        assertEquals(1, cost.consumed);
        assertEquals(1, cost.rolledBack);
    }

    @Test
    void rollbackFailureIsSuppressedOnTheTeleportError() {
        RuntimeException teleportError = new RuntimeException("teleport failed");
        RuntimeException rollbackError = new RuntimeException("rollback failed");
        TeleportCost cost = new TeleportCost() {
            @Override
            public boolean canAfford() {
                return true;
            }

            @Override
            public void consume() {
            }

            @Override
            public void rollback() {
                throw rollbackError;
            }
        };

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> TeleportTransaction.execute(cost, () -> {
                    throw teleportError;
                }));

        assertSame(teleportError, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(rollbackError, actual.getSuppressed()[0]);
    }

    @Test
    void theNoCostImplementationSupportsZeroCostTeleports() {
        assertEquals(TeleportTransaction.Result.SUCCESS,
                TeleportTransaction.execute(TeleportCost.NONE, () -> true));
    }

    private static final class TrackingCost implements TeleportCost {
        private final boolean affordable;
        private int consumed;
        private int rolledBack;

        private TrackingCost(boolean affordable) {
            this.affordable = affordable;
        }

        @Override
        public boolean canAfford() {
            return affordable;
        }

        @Override
        public void consume() {
            consumed++;
        }

        @Override
        public void rollback() {
            rolledBack++;
        }
    }
}
