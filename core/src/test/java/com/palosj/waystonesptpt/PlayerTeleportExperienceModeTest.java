package com.palosj.waystonesptpt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerTeleportExperienceModeTest {
    @Test
    void neverDoesNotEvaluateWaystonesExperience() {
        assertFalse(PlayerTeleportExperienceMode.NEVER.shouldEvaluateWaystonesExperience(false));
        assertFalse(PlayerTeleportExperienceMode.NEVER.shouldEvaluateWaystonesExperience(true));
    }

    @Test
    void followWaystonesUsesTheGlobalCostSwitch() {
        assertFalse(PlayerTeleportExperienceMode.FOLLOW_WAYSTONES.shouldEvaluateWaystonesExperience(false));
        assertTrue(PlayerTeleportExperienceMode.FOLLOW_WAYSTONES.shouldEvaluateWaystonesExperience(true));
    }

    @Test
    void alwaysIgnoresTheGlobalCostSwitch() {
        assertTrue(PlayerTeleportExperienceMode.ALWAYS.shouldEvaluateWaystonesExperience(false));
        assertTrue(PlayerTeleportExperienceMode.ALWAYS.shouldEvaluateWaystonesExperience(true));
    }
}
