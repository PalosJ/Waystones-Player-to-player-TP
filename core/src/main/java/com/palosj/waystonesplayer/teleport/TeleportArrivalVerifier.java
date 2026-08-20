package com.palosj.waystonesplayer.teleport;

import java.util.Objects;

public final class TeleportArrivalVerifier {
    private static final double MOVEMENT_EPSILON_SQUARED = 1.0E-8;

    private TeleportArrivalVerifier() {
    }

    public static boolean succeeded(boolean apiReportedSender, Position before, Position after) {
        // Waystones events may redirect the final destination.  A reported sender is only
        // successful after the player's position actually changes.
        return apiReportedSender && hasMoved(before, after);
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

    public static boolean matchesTargetOrHorizontalAdjacent(Position actual, Target target) {
        if (!actual.dimension().equals(target.dimension())
                || !Double.isFinite(actual.x())
                || !Double.isFinite(actual.y())
                || !Double.isFinite(actual.z())) {
            return false;
        }

        int actualX = (int) Math.floor(actual.x());
        int actualY = (int) Math.floor(actual.y());
        int actualZ = (int) Math.floor(actual.z());
        if (actualY != target.y()) {
            return false;
        }
        int horizontalDistance = Math.abs(actualX - target.x()) + Math.abs(actualZ - target.z());
        return horizontalDistance <= 1;
    }

    public record Position(String dimension, double x, double y, double z) {
        public Position {
            Objects.requireNonNull(dimension, "dimension");
        }
    }

    public record Target(String dimension, int x, int y, int z) {
        public Target {
            Objects.requireNonNull(dimension, "dimension");
        }
    }

}
