package com.palosj.waystonesplayer.teleport;

import java.util.Objects;

public final class TeleportArrivalVerifier {
    private static final double MOVEMENT_EPSILON_SQUARED = 1.0E-8;

    private TeleportArrivalVerifier() {
    }

    public static boolean succeeded(
            boolean apiReportedSender,
            Position before,
            Position after,
            BlockTarget requestedTarget) {
        if (!apiReportedSender) {
            return false;
        }

        boolean reachedRequestedArea = after.dimension().equals(requestedTarget.dimension())
                && floor(after.y()) == requestedTarget.y()
                && Math.abs(floor(after.x()) - requestedTarget.x())
                        + Math.abs(floor(after.z()) - requestedTarget.z()) <= 1;
        if (reachedRequestedArea) {
            return true;
        }

        return hasMoved(before, after);
    }

    public static boolean hasMoved(Position before, Position after) {
        if (!before.dimension().equals(after.dimension())) {
            return true;
        }
        double deltaX = after.x() - before.x();
        double deltaY = after.y() - before.y();
        double deltaZ = after.z() - before.z();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > MOVEMENT_EPSILON_SQUARED;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    public record Position(String dimension, double x, double y, double z) {
        public Position {
            Objects.requireNonNull(dimension, "dimension");
        }
    }

    public record BlockTarget(String dimension, int x, int y, int z) {
        public BlockTarget {
            Objects.requireNonNull(dimension, "dimension");
        }
    }
}
