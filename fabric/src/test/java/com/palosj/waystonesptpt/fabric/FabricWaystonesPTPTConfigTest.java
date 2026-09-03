package com.palosj.waystonesptpt.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;

class FabricWaystonesPTPTConfigTest {
    @Test
    void parsesQuotedModeAndTrailingComment() {
        assertEquals(
                PlayerTeleportExperienceMode.ALWAYS,
                FabricWaystonesPTPTConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = \"ALWAYS\" # explicit override")));
    }

    @Test
    void parsesUnquotedModeCaseInsensitively() {
        assertEquals(
                PlayerTeleportExperienceMode.FOLLOW_WAYSTONES,
                FabricWaystonesPTPTConfig.parseMode(List.of(
                        "playerTeleportExperienceMode=follow_waystones")));
    }

    @Test
    void malformedOrMissingValuesFailSafeToNever() {
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPTPTConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = unknown")));
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPTPTConfig.parseMode(List.of("unrelated = true")));
    }

    @Test
    void duplicateModeFailsSafeToNever() {
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPTPTConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = NEVER",
                        "playerTeleportExperienceMode = ALWAYS")));
    }
}
