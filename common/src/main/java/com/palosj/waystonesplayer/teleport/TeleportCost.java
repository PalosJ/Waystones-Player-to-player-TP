package com.palosj.waystonesplayer.teleport;

import java.util.Objects;

public interface TeleportCost {
    TeleportCost NONE = new TeleportCost() {
        @Override
        public boolean canAfford() {
            return true;
        }

        @Override
        public void consume() {
        }

        @Override
        public void rollback() {
        }
    };

    static TeleportCost exemptWhen(boolean exempt, TeleportCost delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return exempt ? NONE : delegate;
    }

    boolean canAfford();

    void consume();

    void rollback();
}
