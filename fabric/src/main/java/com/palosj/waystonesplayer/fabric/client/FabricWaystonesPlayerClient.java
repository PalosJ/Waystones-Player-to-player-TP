package com.palosj.waystonesplayer.fabric.client;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.WaystonesPlayerClientModule;

import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;

public final class FabricWaystonesPlayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(
                WaystonesPlayer.MODID,
                EmptyLoadContext.INSTANCE,
                new WaystonesPlayerClientModule());
    }
}
