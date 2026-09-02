package com.palosj.waystonesptpt.fabric;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.WaystonesPTPTModule;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;

public final class FabricWaystonesPTPT implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricWaystonesPTPTConfig.load();
        Balm.initializeMod(
                WaystonesPTPT.MODID,
                EmptyLoadContext.INSTANCE,
                new WaystonesPTPTModule(FabricWaystonesPTPTConfig::experienceMode));
    }
}
