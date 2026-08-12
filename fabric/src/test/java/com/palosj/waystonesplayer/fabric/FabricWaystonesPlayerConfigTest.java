package com.palosj.waystonesplayer.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;

class FabricWaystonesPlayerConfigTest {
    @Test
    void parsesQuotedModeAndTrailingComment() {
        assertEquals(
                PlayerTeleportExperienceMode.ALWAYS,
                FabricWaystonesPlayerConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = \"ALWAYS\" # explicit override")));
    }

    @Test
    void parsesUnquotedModeCaseInsensitively() {
        assertEquals(
                PlayerTeleportExperienceMode.FOLLOW_WAYSTONES,
                FabricWaystonesPlayerConfig.parseMode(List.of(
                        "playerTeleportExperienceMode=follow_waystones")));
    }

    @Test
    void malformedOrMissingValuesFailSafeToNever() {
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPlayerConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = unknown")));
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPlayerConfig.parseMode(List.of("unrelated = true")));
    }

    @Test
    void duplicateModeFailsSafeToNever() {
        assertEquals(
                PlayerTeleportExperienceMode.NEVER,
                FabricWaystonesPlayerConfig.parseMode(List.of(
                        "playerTeleportExperienceMode = NEVER",
                        "playerTeleportExperienceMode = ALWAYS")));
    }
}
