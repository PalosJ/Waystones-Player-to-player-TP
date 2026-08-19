package com.palosj.waystonesplayer.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExperienceCostSafetyTest {
    @Test
    void acceptsZeroAndTruncatesLikeWaystones() {
        assertEquals(0, ExperienceCostSafety.checkedAdd(0, 0, "xp"));
        assertEquals(1, ExperienceCostSafety.checkedAdd(1, 0.9f, "levels"));
        assertEquals(0, ExperienceCostSafety.checkedMaximum(5, 0, "xp"));
    }

    @Test
    void rejectsNegativeInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedAdd(0, -1, "xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedMultiply(1, -0.1f, "levels"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedScaledAdd(1, -1, 1, "xp"));
    }

    @Test
    void rejectsNonFiniteInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedMultiply(1, Float.NaN, "xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedMultiply(1, Float.POSITIVE_INFINITY, "xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedScaledMultiply(1, Float.NEGATIVE_INFINITY, 1, "xp"));
    }

    @Test
    void rejectsIntegerAndFloatingPointOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedAdd(Integer.MAX_VALUE, 1, "xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedMultiply(Integer.MAX_VALUE, 2, "xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceCostSafety.checkedScaledAdd(Integer.MAX_VALUE, 1, 1, "xp"));
    }

    @Test
    void keepsBoundaryValues() {
        assertEquals(Integer.MAX_VALUE,
                ExperienceCostSafety.checkedAdd(Integer.MAX_VALUE, 0, "xp"));
        assertEquals(12, ExperienceCostSafety.checkedScaledAdd(2, 5, 2, "xp"));
        assertEquals(30, ExperienceCostSafety.checkedScaledMultiply(3, 5, 2, "xp"));
    }
}
