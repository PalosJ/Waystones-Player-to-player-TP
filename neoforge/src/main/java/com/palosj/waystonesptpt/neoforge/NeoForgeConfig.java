package com.palosj.waystonesptpt.neoforge;

import java.io.IOException;
import java.nio.file.Path;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.config.LegacyConfigMigration;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeConfig {
    private static final String LEGACY_FILE_NAME = "waystonesplayer-server.toml";
    private static final String FILE_NAME = "waystonesptpt-server.toml";
    private static final LevelResource SERVER_CONFIG_DIRECTORY = new LevelResource("serverconfig");
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<PlayerTeleportExperienceMode> PLAYER_TELEPORT_EXPERIENCE_MODE = BUILDER
            .comment(
                    "Controls experience costs for player teleportation.",
                    "NEVER: Never consume experience (default).",
                    "FOLLOW_WAYSTONES: Follow Waystones' global cost setting and configured experience rules.",
                    "ALWAYS: Apply Waystones' configured experience rules even when its global costs are disabled.")
            .translation("config.waystonesptpt.player_teleport_experience_mode")
            .defineEnum("playerTeleportExperienceMode", PlayerTeleportExperienceMode.NEVER);

    private static final ModConfigSpec SPEC = BUILDER.build();

    private NeoForgeConfig() {
    }

    public static void register(ModContainer modContainer) {
        migrateDirectory(FMLPaths.CONFIGDIR.get());
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, FILE_NAME);
    }

    public static PlayerTeleportExperienceMode experienceMode() {
        return PLAYER_TELEPORT_EXPERIENCE_MODE.get();
    }

    public static void migrateWorldConfig(MinecraftServer server) {
        // Called before NeoForge chooses the world/global config path. Its normal loader owns
        // initialization, watchers and reload events; migrating this file must not reload other mods.
        migrateDirectory(server.getWorldPath(SERVER_CONFIG_DIRECTORY));
    }

    private static boolean migrateDirectory(Path directory) {
        try {
            if (LegacyConfigMigration.copyIfCurrentMissing(directory, LEGACY_FILE_NAME, FILE_NAME)) {
                WaystonesPTPT.LOGGER.info("Migrated legacy config from {} to {}.", LEGACY_FILE_NAME, FILE_NAME);
                return true;
            }
        } catch (IOException exception) {
            WaystonesPTPT.LOGGER.error(
                    "Could not migrate legacy config in {}; the existing WaystonesPTPT config remains authoritative.",
                    directory,
                    exception);
        }
        return false;
    }
}
