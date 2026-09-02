package com.palosj.waystonesptpt.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyConfigMigrationTest {
    private static final String LEGACY_NAME = "waystonesplayer-server.toml";
    private static final String CURRENT_NAME = "waystonesptpt-server.toml";

    @TempDir
    Path temporaryDirectory;

    @Test
    void copiesLegacyConfigWithoutDeletingIt() throws Exception {
        Path legacyFile = temporaryDirectory.resolve(LEGACY_NAME);
        Files.writeString(legacyFile, "playerTeleportExperienceMode = \"ALWAYS\"\n");

        assertTrue(LegacyConfigMigration.copyIfCurrentMissing(
                temporaryDirectory, LEGACY_NAME, CURRENT_NAME));
        assertEquals(Files.readString(legacyFile), Files.readString(temporaryDirectory.resolve(CURRENT_NAME)));
        assertTrue(Files.exists(legacyFile));
    }

    @Test
    void existingCurrentConfigAlwaysWins() throws Exception {
        Files.writeString(temporaryDirectory.resolve(LEGACY_NAME), "legacy\n");
        Files.writeString(temporaryDirectory.resolve(CURRENT_NAME), "current\n");

        assertFalse(LegacyConfigMigration.copyIfCurrentMissing(
                temporaryDirectory, LEGACY_NAME, CURRENT_NAME));
        assertEquals("current\n", Files.readString(temporaryDirectory.resolve(CURRENT_NAME)));
    }

    @Test
    void missingLegacyConfigDoesNothing() throws Exception {
        assertFalse(LegacyConfigMigration.copyIfCurrentMissing(
                temporaryDirectory, LEGACY_NAME, CURRENT_NAME));
        assertFalse(Files.exists(temporaryDirectory.resolve(CURRENT_NAME)));
    }
}
