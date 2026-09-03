package com.palosj.waystonesptpt.compat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class ExperienceRequirementSafetyTest {
    @Test
    void acceptsZeroBoundaryAndOfficialDefaultArithmetic() {
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "$xp_points_cost = if(condition = is_interdimensional, then = 27, else = $distance * 0.01)"));
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "source(is_warp_plate()), target(is_global()) -> $xp_points_cost = 0"));
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "$xp_points_cost = clamp($xp_points_cost, 0, 2147483647)"));
    }

    @Test
    void rejectsNegativeNonFiniteOverflowAndSubtractionInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = -1"));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = Infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = 2147483648"));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = $distance - 1"));
    }

    @Test
    void ignoresUnsafeWordsInsideQuotedNamesButStillRejectsNumericPayloads() {
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "name_equals(\"Infinity-NaN\") -> $xp_points_cost = 0"));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals(
                        "name_equals(\"safe\") -> $xp_points_cost = -0.01"));
    }

    @Test
    void classifiesOnlyKnownExperiencePureAndExcludedEffects() {
        Identifier points = id("shogi", "xp_points_cost");
        Identifier levels = id("shogi", "xp_level_cost");
        assertTrue(ShogiExperienceRuleSafety.isExperienceEffect(points));
        assertTrue(ShogiExperienceRuleSafety.isExperienceEffect(levels));
        assertFalse(ShogiExperienceRuleSafety.isExcludedCostEffect(points));
        assertTrue(ShogiExperienceRuleSafety.isExcludedCostEffect(id("shogi", "damage_item")));
        assertTrue(ShogiExperienceRuleSafety.isKnownPureEffect(id("waystones", "is_warp_stone")));
        assertFalse(ShogiExperienceRuleSafety.isKnownPureEffect(id("example", "free_teleport")));
    }

    @Test
    void acceptsOnlyMonotonicNonNegativeIntegerAggregates() {
        assertDoesNotThrow(() -> LockedShogiExecutor.requireCost(0, "zero"));
        assertDoesNotThrow(() -> LockedShogiExecutor.requireCost(Integer.MAX_VALUE, "maximum"));
        assertDoesNotThrow(() -> LockedShogiExecutor.validateAggregateProgress(5, 5));
        assertDoesNotThrow(() -> LockedShogiExecutor.validateAggregateProgress(5, 6));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(-1, "negative"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(Double.NaN, "nan"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(Double.POSITIVE_INFINITY, "infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.validateAggregateProgress(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
