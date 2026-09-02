package com.palosj.waystonesptpt;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.Identifier;

public final class WaystonesPTPT {
    public static final String MODID = "waystonesptpt";
    public static final Logger LOGGER = LogUtils.getLogger();

    private WaystonesPTPT() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
