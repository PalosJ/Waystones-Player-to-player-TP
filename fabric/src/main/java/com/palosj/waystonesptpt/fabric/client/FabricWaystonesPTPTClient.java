package com.palosj.waystonesptpt.fabric.client;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.WaystonesPTPTClientModule;

import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;

public final class FabricWaystonesPTPTClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(
                WaystonesPTPT.MODID,
                EmptyLoadContext.INSTANCE,
                new WaystonesPTPTClientModule());
    }
}
