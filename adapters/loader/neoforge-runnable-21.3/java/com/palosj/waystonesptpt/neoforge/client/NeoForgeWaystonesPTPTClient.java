package com.palosj.waystonesptpt.neoforge.client;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.WaystoneClientSetup;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = WaystonesPTPT.MODID, dist = Dist.CLIENT)
public final class NeoForgeWaystonesPTPTClient {
    public NeoForgeWaystonesPTPTClient(IEventBus modEventBus) {
        BalmClient.initialize(
                WaystonesPTPT.MODID,
                new NeoForgeLoadContext(modEventBus),
                () -> WaystoneClientSetup.register(Balm.getEvents()));
    }
}
