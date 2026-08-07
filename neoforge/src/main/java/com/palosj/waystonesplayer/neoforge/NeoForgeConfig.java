package com.palosj.waystonesplayer.neoforge;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;

import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<PlayerTeleportExperienceMode> PLAYER_TELEPORT_EXPERIENCE_MODE = BUILDER
            .comment(
                    "Controls experience costs for player teleportation.",
                    "NEVER: Never consume experience (default).",
                    "FOLLOW_WAYSTONES: Follow Waystones' global cost setting and configured experience rules.",
                    "ALWAYS: Apply Waystones' configured experience rules even when its global costs are disabled.")
            .translation("config.waystonesplayer.player_teleport_experience_mode")
            .defineEnum("playerTeleportExperienceMode", PlayerTeleportExperienceMode.NEVER);

    private static final ModConfigSpec SPEC = BUILDER.build();

    private NeoForgeConfig() {
    }

    public static void register() {
        ModList.get().getModContainerById(WaystonesPlayer.MODID)
                .orElseThrow(() -> new IllegalStateException("Missing Waystones Player mod container."))
                .registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    public static PlayerTeleportExperienceMode experienceMode() {
        return PLAYER_TELEPORT_EXPERIENCE_MODE.get();
    }
}
