package com.palosj.waystonesptpt.neoforge.client;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.WaystonesPTPTClientModule;

import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = WaystonesPTPT.MODID, dist = Dist.CLIENT)
public final class NeoForgeWaystonesPTPTClient {
    public NeoForgeWaystonesPTPTClient(ModContainer modContainer, IEventBus modEventBus) {
        BalmClient.initializeMod(
                WaystonesPTPT.MODID,
                new NeoForgeLoadContext(modContainer, modEventBus),
                new WaystonesPTPTClientModule());
    }
}
