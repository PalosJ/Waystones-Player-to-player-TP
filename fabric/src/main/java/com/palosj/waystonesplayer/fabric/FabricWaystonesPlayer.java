package com.palosj.waystonesplayer.fabric;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.WaystonesPlayerModule;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;

public final class FabricWaystonesPlayer implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricWaystonesPlayerConfig.load();
        Balm.initializeMod(
                WaystonesPlayer.MODID,
                EmptyLoadContext.INSTANCE,
                new WaystonesPlayerModule(FabricWaystonesPlayerConfig::experienceMode));
    }
}
