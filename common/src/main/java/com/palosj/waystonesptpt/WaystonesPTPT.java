package com.palosj.waystonesptpt;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;

public final class WaystonesPTPT {
    public static final String MODID = "waystonesptpt";
    public static final Logger LOGGER = LogUtils.getLogger();

    private WaystonesPTPT() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
