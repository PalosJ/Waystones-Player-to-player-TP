package com.palosj.waystonesplayer.client;

import net.blay09.mods.balm.api.client.module.BalmClientModule;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.minecraft.resources.ResourceLocation;

import com.palosj.waystonesplayer.WaystonesPlayer;

public final class WaystonesPlayerClientModule implements BalmClientModule {
    @Override
    public ResourceLocation getId() {
        return WaystonesPlayer.id("client");
    }

    @Override
    public void registerEvents(BalmEvents events) {
        WaystoneClientSetup.register(events);
    }
}
