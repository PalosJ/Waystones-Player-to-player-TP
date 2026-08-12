package com.palosj.waystonesplayer.fabric;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricWaystonesPlayerConfig {
    private static final String FILE_NAME = "waystonesplayer-server.toml";
    private static final String MODE_KEY = "playerTeleportExperienceMode";
    private static final String DEFAULT_CONTENT = """
            # Controls experience costs for player destinations.
            # NEVER is free. FOLLOW_WAYSTONES follows the Waystones experience switch.
            # ALWAYS evaluates the Waystones experience rules regardless of that switch.
            playerTeleportExperienceMode = "NEVER"
            """;

    private static volatile PlayerTeleportExperienceMode configuredMode =
            PlayerTeleportExperienceMode.NEVER;

    private FabricWaystonesPlayerConfig() {
    }

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.createDirectories(configFile.getParent());
            if (Files.notExists(configFile)) {
                try {
                    Files.writeString(
                            configFile,
                            DEFAULT_CONTENT,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE);
                } catch (FileAlreadyExistsException ignored) {
                    // Another startup path created the same deterministic default first.
                }
            }
            configuredMode = parseMode(Files.readAllLines(configFile));
        } catch (IOException | RuntimeException exception) {
            configuredMode = PlayerTeleportExperienceMode.NEVER;
            WaystonesPlayer.LOGGER.error(
                    "Could not read {}; using {}.",
                    configFile,
                    configuredMode,
                    exception);
        }
    }

    public static PlayerTeleportExperienceMode experienceMode() {
        return configuredMode;
    }

    static PlayerTeleportExperienceMode parseMode(List<String> lines) {
        String configuredValue = null;
        for (String line : lines) {
            String content = stripComment(line).trim();
            if (content.isEmpty()) {
                continue;
            }
            int separator = content.indexOf('=');
            if (separator < 0 || !content.substring(0, separator).trim().equals(MODE_KEY)) {
                continue;
            }
            if (configuredValue != null) {
                WaystonesPlayer.LOGGER.warn(
                        "Duplicate {} in {}; using {}.",
                        MODE_KEY,
                        FILE_NAME,
                        PlayerTeleportExperienceMode.NEVER);
                return PlayerTeleportExperienceMode.NEVER;
            }
            configuredValue = unquote(content.substring(separator + 1).trim());
        }

        if (configuredValue == null || configuredValue.isBlank()) {
            WaystonesPlayer.LOGGER.warn(
                    "Missing {} in {}; using {}.",
                    MODE_KEY,
                    FILE_NAME,
                    PlayerTeleportExperienceMode.NEVER);
            return PlayerTeleportExperienceMode.NEVER;
        }

        try {
            return PlayerTeleportExperienceMode.valueOf(configuredValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            WaystonesPlayer.LOGGER.warn(
                    "Unknown {} value '{}' in {}; using {}.",
                    MODE_KEY,
                    configuredValue,
                    FILE_NAME,
                    PlayerTeleportExperienceMode.NEVER);
            return PlayerTeleportExperienceMode.NEVER;
        }
    }

    private static String stripComment(String line) {
        int comment = line.indexOf('#');
        return comment >= 0 ? line.substring(0, comment) : line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}
