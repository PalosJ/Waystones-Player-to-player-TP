package com.palosj.waystonesplayer.fabric.client;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.WaystonesPlayerClientModule;

import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;

public final class FabricWaystonesPlayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(
                WaystonesPlayer.MODID,
                FabricLoadContext.INSTANCE,
                new WaystonesPlayerClientModule());
    }
}
