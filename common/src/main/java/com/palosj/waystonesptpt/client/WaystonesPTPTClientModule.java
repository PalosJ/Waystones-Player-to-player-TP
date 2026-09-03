package com.palosj.waystonesptpt.client;

import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.resources.Identifier;

import com.palosj.waystonesptpt.WaystonesPTPT;

public final class WaystonesPTPTClientModule implements BalmClientModule {
    @Override
    public Identifier getId() {
        return WaystonesPTPT.id("client");
    }

    @Override
    public void initialize() {
        WaystoneClientSetup.register();
    }
}
