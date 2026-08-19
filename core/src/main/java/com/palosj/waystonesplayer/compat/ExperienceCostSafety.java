package com.palosj.waystonesplayer.compat;

public final class ExperienceCostSafety {
    private ExperienceCostSafety() {
    }

    public static int requireNonNegative(int value, String label) {
        if (value < 0) {
            throw invalid(label, value);
        }
        return value;
    }

    public static float requireFiniteNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0) {
            throw invalid(label, value);
        }
        return value;
    }

    public static int checkedAdd(int current, int amount, String label) {
        requireNonNegative(current, label);
        requireNonNegative(amount, label);
        try {
            return Math.addExact(current, amount);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(label + " exceeds the supported integer range", error);
        }
    }

    public static int checkedAdd(int current, float amount, String label) {
        requireNonNegative(current, label);
        requireFiniteNonNegative(amount, label);
        return checkedTruncated((double) current + amount, label);
    }

    public static int checkedMultiply(int current, float factor, String label) {
        requireNonNegative(current, label);
        requireFiniteNonNegative(factor, label);
        return checkedTruncated((double) current * factor, label);
    }

    public static int checkedScaledAdd(int current, float source, float scale, String label) {
        requireNonNegative(current, label);
        requireFiniteNonNegative(source, label + " source");
        requireFiniteNonNegative(scale, label + " scale");
        return checkedTruncated((double) current + (double) source * scale, label);
    }

    public static int checkedScaledMultiply(int current, float source, float scale, String label) {
        requireNonNegative(current, label);
        requireFiniteNonNegative(source, label + " source");
        requireFiniteNonNegative(scale, label + " scale");
        return checkedTruncated((double) current * source * scale, label);
    }

    public static int checkedMinimum(int current, int minimum, String label) {
        requireNonNegative(current, label);
        requireNonNegative(minimum, label);
        return Math.max(current, minimum);
    }

    public static int checkedMaximum(int current, int maximum, String label) {
        requireNonNegative(current, label);
        requireNonNegative(maximum, label);
        return Math.min(current, maximum);
    }

    private static int checkedTruncated(double value, String label) {
        if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE) {
            throw invalid(label, value);
        }
        return (int) value;
    }

    private static IllegalArgumentException invalid(String label, Object value) {
        return new IllegalArgumentException(label + " must be finite, non-negative, and within the integer range: "
                + value);
    }
}
