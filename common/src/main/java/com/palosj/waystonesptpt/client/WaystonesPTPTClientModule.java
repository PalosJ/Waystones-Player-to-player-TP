package com.palosj.waystonesptpt.client;

import net.blay09.mods.balm.api.client.module.BalmClientModule;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.minecraft.resources.ResourceLocation;

import com.palosj.waystonesptpt.WaystonesPTPT;

public final class WaystonesPTPTClientModule implements BalmClientModule {
    @Override
    public ResourceLocation getId() {
        return WaystonesPTPT.id("client");
    }

    @Override
    public void registerEvents(BalmEvents events) {
        WaystoneClientSetup.register(events);
    }
}
