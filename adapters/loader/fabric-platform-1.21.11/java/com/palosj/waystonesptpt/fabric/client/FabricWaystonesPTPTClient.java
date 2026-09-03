package com.palosj.waystonesptpt.fabric.client;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.WaystonesPTPTClientModule;

import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;

public final class FabricWaystonesPTPTClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(
                WaystonesPTPT.MODID,
                FabricLoadContext.INSTANCE,
                new WaystonesPTPTClientModule());
    }
}
