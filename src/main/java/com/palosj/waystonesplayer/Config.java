package com.palosj.waystonesplayer;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<PlayerTeleportExperienceMode> PLAYER_TELEPORT_EXPERIENCE_MODE = BUILDER
            .comment(
                    "Controls experience costs for player teleportation.",
                    "NEVER: Never consume experience (default).",
                    "FOLLOW_WAYSTONES: Follow Waystones' global cost setting and configured experience rules.",
                    "ALWAYS: Apply Waystones' configured experience rules even when its global costs are disabled.")
            .translation("config.waystonesplayer.player_teleport_experience_mode")
            .defineEnum("playerTeleportExperienceMode", PlayerTeleportExperienceMode.NEVER);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
