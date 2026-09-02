package com.palosj.waystonesptpt.fabric;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.network.ModNetworking;
import com.palosj.waystonesptpt.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;

public final class FabricWaystonesPTPT implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricWaystonesPTPTConfig.load();
        Balm.initialize(WaystonesPTPT.MODID, EmptyLoadContext.INSTANCE, () -> {
            ModNetworking.register(
                    Balm.getNetworking(),
                    FabricWaystonesPTPTConfig::experienceMode);
            PlayerLifecycleHandler.register(Balm.getEvents());
        });
    }
}
