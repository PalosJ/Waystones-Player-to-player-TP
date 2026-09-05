package com.palosj.waystonesptpt.teleport;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** One server-thread-owned request. Cancellation permanently fences late preparation callbacks. */
public final class TeleportAttempt {
    public static final int PREPARATION_TIMEOUT_TICKS = 200;

    public enum State { PREPARING, COMMITTING, SETTLED, CANCELLED }

    private final int startedAt;
    private final BooleanSupplier contextValid;
    private State state = State.PREPARING;
    private int durabilityCost = 1;

    public TeleportAttempt(int startedAt, BooleanSupplier contextValid) {
        this.startedAt = startedAt;
        this.contextValid = Objects.requireNonNull(contextValid, "contextValid");
    }

    public State state() {
        return state;
    }

    public boolean validatePreparation(int currentTick) {
        if (state != State.PREPARING) {
            return false;
        }
        int elapsed = currentTick - startedAt;
        if (elapsed < 0 || elapsed >= PREPARATION_TIMEOUT_TICKS || !contextValid.getAsBoolean()) {
            state = State.CANCELLED;
            return false;
        }
        return true;
    }

    public void beginCommit(int currentTick) {
        if (!validatePreparation(currentTick)) {
            throw new IllegalStateException("Player teleport is no longer eligible to commit");
        }
        state = State.COMMITTING;
    }

    public void cancel() {
        if (state == State.PREPARING) {
            state = State.CANCELLED;
        }
    }

    public boolean settle() {
        if (state == State.CANCELLED || state == State.SETTLED) {
            return false;
        }
        state = State.SETTLED;
        return true;
    }

    public void setDurabilityCost(int cost) {
        if (state != State.PREPARING || cost < 0) {
            throw new IllegalStateException("Durability must be fixed before committing");
        }
        durabilityCost = cost;
    }

    public int durabilityCost() {
        return durabilityCost;
    }
}
