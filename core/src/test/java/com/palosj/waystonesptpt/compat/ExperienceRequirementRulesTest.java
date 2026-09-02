package com.palosj.waystonesptpt.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExperienceRequirementRulesTest {
    @Test
    void acceptsExperiencePointRequirements() {
        assertTrue(ExperienceRequirementRules.isExperienceRequirementType("waystones:experience_points"));
    }

    @Test
    void acceptsExperienceLevelRequirements() {
        assertTrue(ExperienceRequirementRules.isExperienceRequirementType("waystones:experience_levels"));
    }

    @Test
    void rejectsNonExperienceAndCustomRequirementTypes() {
        assertFalse(ExperienceRequirementRules.isExperienceRequirementType("waystones:item"));
        assertFalse(ExperienceRequirementRules.isExperienceRequirementType("example:custom_cost"));
        assertFalse(ExperienceRequirementRules.isExperienceRequirementType(""));
    }

    @Test
    void followModeAppliesOnlyEnabledExperienceFunctions() {
        assertTrue(ExperienceRequirementRules.shouldApply(
                "waystones:experience_points", "waystones:add_xp_cost", true, false));
        assertFalse(ExperienceRequirementRules.shouldApply(
                "waystones:experience_points", "waystones:add_xp_cost", false, false));
    }

    @Test
    void alwaysModeOnlyForcesWaystonesOwnedExperienceFunctions() {
        assertTrue(ExperienceRequirementRules.shouldApply(
                "waystones:experience_levels", "waystones:add_level_cost", false, true));
        assertFalse(ExperienceRequirementRules.shouldApply(
                "waystones:experience_levels", "example:custom_level_cost", false, true));
    }

    @Test
    void nonExperienceFunctionsRemainIgnoredWhenEnabledOrForced() {
        assertFalse(ExperienceRequirementRules.shouldApply(
                "waystones:item", "waystones:consume_item", true, false));
        assertFalse(ExperienceRequirementRules.shouldApply(
                "waystones:cooldown", "waystones:add_cooldown", false, true));
    }
}
