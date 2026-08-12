package com.palosj.waystonesplayer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.Identifier;

public final class WaystonesPlayer {
    public static final String MODID = "waystonesplayer";
    public static final Logger LOGGER = LogUtils.getLogger();

    private WaystonesPlayer() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
