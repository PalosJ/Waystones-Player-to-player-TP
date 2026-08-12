package com.palosj.waystonesplayer.client;

import com.palosj.waystonesplayer.WaystonesPlayer;

import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.resources.Identifier;

public final class WaystonesPlayerClientModule implements BalmClientModule {
    @Override
    public Identifier getId() {
        return WaystonesPlayer.id("client");
    }

    @Override
    public void initialize() {
        WaystoneClientSetup.register();
    }
}
