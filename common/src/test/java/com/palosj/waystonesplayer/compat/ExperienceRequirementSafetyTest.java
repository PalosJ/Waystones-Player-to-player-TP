package com.palosj.waystonesplayer.compat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.blay09.mods.waystones.requirement.CombinedRequirement;
import net.blay09.mods.waystones.requirement.ExperienceLevelRequirement;
import net.blay09.mods.waystones.requirement.ExperiencePointsRequirement;
import net.blay09.mods.waystones.requirement.NoRequirement;
import net.blay09.mods.waystones.requirement.RequirementRegistry;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;

class ExperienceRequirementSafetyTest {
    @Test
    void rejectsRulesThatParseToNoModifiers() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.parseRequiredRule("not a valid requirement"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.normalizeParsedRule("empty optional", Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.normalizeParsedRule("empty iterable", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.normalizeParsedRule("unknown optional", Optional.of("unknown")));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.normalizeParsedRule("unknown shape", "unknown"));
    }

    @Test
    void rejectsUnsafeModifierParameters() {
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_points",
                "waystones:add_xp_cost",
                0,
                new RequirementRegistry.IntParameter(-1),
                0));
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_levels",
                "waystones:multiply_level_cost",
                1,
                new RequirementRegistry.FloatParameter(Float.NaN),
                0));
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_levels",
                "other:multiply_level_cost",
                1,
                new RequirementRegistry.FloatParameter(1),
                0));
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_levels",
                "waystones:scaled_add_level_cost",
                1,
                new RequirementRegistry.VariableScaledParameter(
                        new RequirementRegistry.WaystonesIdParameter(null),
                        new RequirementRegistry.FloatParameter(-1)),
                2));
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_points",
                "waystones:multiply_xp_cost",
                1,
                new RequirementRegistry.FloatParameter(Float.POSITIVE_INFINITY),
                0));
        assertThrows(IllegalArgumentException.class, () -> ExperienceRequirementSafety.expectedCost(
                "waystones:experience_points",
                "waystones:scaled_multiply_xp_cost",
                Integer.MAX_VALUE,
                new RequirementRegistry.VariableScaledParameter(
                        new RequirementRegistry.WaystonesIdParameter(null),
                        new RequirementRegistry.FloatParameter(2)),
                2));
    }

    @Test
    void rejectsUnknownExperienceModifierEvenBeforeItCanBeApplied() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateModifierIdentity(
                        "waystones:experience_points", "example:free_xp"));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateModifierIdentity(
                        "waystones:experience_levels", "waystones:unknown_level_cost"));
        assertDoesNotThrow(() -> ExperienceRequirementSafety.validateModifierIdentity(
                "waystones:item", "example:custom_item_cost"));
    }

    @Test
    void computesCheckedBuiltInModifierResults() {
        assertEquals(7, ExperienceRequirementSafety.expectedCost(
                "waystones:experience_points",
                "waystones:add_xp_cost",
                2,
                new RequirementRegistry.IntParameter(5),
                0));
        assertEquals(12, ExperienceRequirementSafety.expectedCost(
                "waystones:experience_levels",
                "waystones:scaled_add_level_cost",
                2,
                new RequirementRegistry.VariableScaledParameter(
                        new RequirementRegistry.WaystonesIdParameter(null),
                        new RequirementRegistry.FloatParameter(2)),
                5));
    }

    @Test
    void validatesOnlyKnownNonNegativeRequirementTrees() {
        assertDoesNotThrow(() -> ExperienceRequirementSafety.validateRequirementTree(NoRequirement.INSTANCE));
        assertDoesNotThrow(() -> ExperienceRequirementSafety.validateRequirementTree(
                new CombinedRequirement(List.of(
                        new ExperiencePointsRequirement(3),
                        new ExperienceLevelRequirement(2)))));

        ExperiencePointsRequirement negative = new ExperiencePointsRequirement(0);
        negative.setPoints(-1);
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateRequirementTree(negative));

        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateRequirementTree(null));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateRequirementTree(new CombinedRequirement(List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateRequirementTree(new CombinedRequirement(List.of(
                        new ExperiencePointsRequirement(1),
                        new ExperiencePointsRequirement(2)))));

        WarpRequirement unknown = (WarpRequirement) Proxy.newProxyInstance(
                WarpRequirement.class.getClassLoader(),
                new Class<?>[] { WarpRequirement.class },
                (proxy, method, arguments) -> defaultValue(method.getReturnType()));
        assertThrows(IllegalArgumentException.class,
                () -> ExperienceRequirementSafety.validateRequirementTree(unknown));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
