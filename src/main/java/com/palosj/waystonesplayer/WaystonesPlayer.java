package com.palosj.waystonesplayer;

import com.mojang.logging.LogUtils;
import com.palosj.waystonesplayer.network.ModNetworking;
import com.palosj.waystonesplayer.teleport.PlayerLifecycleHandler;
import org.slf4j.Logger;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WaystonesPlayer.MODID)
public final class WaystonesPlayer {
    public static final String MODID = "waystonesplayer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WaystonesPlayer(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.register(PlayerLifecycleHandler.class);

        ModList.get().getModContainerById(MODID)
                .ifPresent(c -> c.registerConfig(ModConfig.Type.SERVER, Config.SPEC));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class.forName("com.palosj.waystonesplayer.client.WaystoneClientSetup")
                        .getMethod("register", IEventBus.class)
                        .invoke(null, modEventBus);
            } catch (Exception e) {
                LOGGER.error("Failed to setup waystonesplayer client", e);
            }
        }
    }
}
