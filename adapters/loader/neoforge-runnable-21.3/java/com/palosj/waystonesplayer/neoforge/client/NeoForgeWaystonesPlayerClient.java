package com.palosj.waystonesplayer.neoforge.client;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.WaystoneClientSetup;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = WaystonesPlayer.MODID, dist = Dist.CLIENT)
public final class NeoForgeWaystonesPlayerClient {
    public NeoForgeWaystonesPlayerClient(IEventBus modEventBus) {
        BalmClient.initialize(
                WaystonesPlayer.MODID,
                new NeoForgeLoadContext(modEventBus),
                () -> WaystoneClientSetup.register(Balm.getEvents()));
    }
}
