package com.palosj.waystonesplayer.fabric;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.network.ModNetworking;
import com.palosj.waystonesplayer.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;

public final class FabricWaystonesPlayer implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricWaystonesPlayerConfig.load();
        Balm.initialize(WaystonesPlayer.MODID, EmptyLoadContext.INSTANCE, () -> {
            ModNetworking.register(
                    Balm.getNetworking(),
                    FabricWaystonesPlayerConfig::experienceMode);
            PlayerLifecycleHandler.register(Balm.getEvents());
        });
    }
}
