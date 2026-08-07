package com.palosj.waystonesplayer.teleport;

import java.util.function.BooleanSupplier;

final class TeleportTransaction {
    private TeleportTransaction() {
    }

    static Result execute(TeleportCost cost, BooleanSupplier teleportAction) {
        if (!cost.canAfford()) {
            return Result.UNAFFORDABLE;
        }

        cost.consume();
        final boolean teleported;
        try {
            teleported = teleportAction.getAsBoolean();
        } catch (RuntimeException e) {
            rollbackAfterFailure(cost, e);
            throw e;
        }

        if (!teleported) {
            cost.rollback();
            return Result.FAILED;
        }
        return Result.SUCCESS;
    }

    private static void rollbackAfterFailure(TeleportCost cost, RuntimeException teleportError) {
        try {
            cost.rollback();
        } catch (RuntimeException rollbackError) {
            teleportError.addSuppressed(rollbackError);
        }
    }

    enum Result {
        SUCCESS,
        UNAFFORDABLE,
        FAILED
    }
}
