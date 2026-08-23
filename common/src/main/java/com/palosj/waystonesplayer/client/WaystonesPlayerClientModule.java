package com.palosj.waystonesplayer.client;

import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.resources.Identifier;

import com.palosj.waystonesplayer.WaystonesPlayer;

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
