package com.palosj.waystonesptpt;

import java.util.Objects;
import java.util.function.Supplier;

import com.palosj.waystonesptpt.network.ModNetworking;
import com.palosj.waystonesptpt.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.module.BalmModule;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public final class WaystonesPTPTModule implements BalmModule {
    private final Supplier<PlayerTeleportExperienceMode> experienceMode;

    public WaystonesPTPTModule(Supplier<PlayerTeleportExperienceMode> experienceMode) {
        this.experienceMode = Objects.requireNonNull(experienceMode, "experienceMode");
    }

    @Override
    public ResourceLocation getId() {
        return WaystonesPTPT.id("main");
    }

    @Override
    public void registerNetworking(BalmNetworking networking) {
        ModNetworking.register(networking, experienceMode);
    }

    @Override
    public void registerEvents(BalmEvents events) {
        PlayerLifecycleHandler.register(events);
    }
}
