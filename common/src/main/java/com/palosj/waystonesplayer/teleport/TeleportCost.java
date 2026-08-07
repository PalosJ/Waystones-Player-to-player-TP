package com.palosj.waystonesplayer.teleport;

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

    boolean canAfford();

    void consume();

    void rollback();
}
