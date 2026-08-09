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

    @Test
    void exemptedCostNeverChecksConsumesOrRollsBackTheDelegate() {
        TrackingCost delegate = new TrackingCost(false);
        TeleportCost cost = TeleportCost.exemptWhen(true, delegate);

        TeleportTransaction.Result result = TeleportTransaction.execute(cost, () -> false);

        assertEquals(TeleportTransaction.Result.FAILED, result);
        assertEquals(0, delegate.affordabilityChecks);
        assertEquals(0, delegate.consumed);
        assertEquals(0, delegate.rolledBack);
    }

    @Test
    void nonExemptCostDelegatesNormally() {
        TrackingCost delegate = new TrackingCost(true);
        TeleportCost cost = TeleportCost.exemptWhen(false, delegate);

        TeleportTransaction.Result result = TeleportTransaction.execute(cost, () -> true);

        assertEquals(TeleportTransaction.Result.SUCCESS, result);
        assertEquals(1, delegate.affordabilityChecks);
        assertEquals(1, delegate.consumed);
        assertEquals(0, delegate.rolledBack);
    }

    private static final class TrackingCost implements TeleportCost {
        private final boolean affordable;
        private int affordabilityChecks;
        private int consumed;
        private int rolledBack;

        private TrackingCost(boolean affordable) {
            this.affordable = affordable;
        }

        @Override
        public boolean canAfford() {
            affordabilityChecks++;
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
