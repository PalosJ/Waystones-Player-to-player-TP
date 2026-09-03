package com.palosj.waystonesptpt.client;

import com.palosj.waystonesptpt.WaystonesPTPT;

import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.resources.Identifier;

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
