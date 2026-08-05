package com.palosj.waystonesplayer;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_HUNGER_COST = BUILDER
            .comment("Enable hunger cost for teleportation. Default: true")
            .define("enableHungerCost", true);

    public static final ModConfigSpec.IntValue FOOD_COST_PER_500_BLOCKS = BUILDER
            .comment("Food points consumed per 500 blocks of distance. Default: 1")
            .defineInRange("foodCostPer500Blocks", 1, 1, 20);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
