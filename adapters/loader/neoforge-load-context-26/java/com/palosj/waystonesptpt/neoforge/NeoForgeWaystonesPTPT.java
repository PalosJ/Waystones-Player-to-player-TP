package com.palosj.waystonesptpt.neoforge;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.WaystonesPTPTModule;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(WaystonesPTPT.MODID)
public final class NeoForgeWaystonesPTPT {
    public NeoForgeWaystonesPTPT(ModContainer modContainer, IEventBus modEventBus) {
        NeoForgeConfig.register(modContainer);
        Balm.initializeMod(
                WaystonesPTPT.MODID,
                new NeoForgeLoadContext(modContainer, modEventBus),
                new WaystonesPTPTModule(NeoForgeConfig::experienceMode));
    }
}
