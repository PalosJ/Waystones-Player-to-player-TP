package com.palosj.waystonesplayer.neoforge.client;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.WaystonesPlayerClientModule;

import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = WaystonesPlayer.MODID, dist = Dist.CLIENT)
public final class NeoForgeWaystonesPlayerClient {
    public NeoForgeWaystonesPlayerClient(ModContainer modContainer, IEventBus modEventBus) {
        BalmClient.initializeMod(
                WaystonesPlayer.MODID,
                new NeoForgeLoadContext(modContainer, modEventBus),
                new WaystonesPlayerClientModule());
    }
}
