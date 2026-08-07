package com.palosj.waystonesplayer;

import java.util.Objects;
import java.util.function.Supplier;

import com.palosj.waystonesplayer.network.ModNetworking;
import com.palosj.waystonesplayer.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.module.BalmModule;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public final class WaystonesPlayerModule implements BalmModule {
    private final Supplier<PlayerTeleportExperienceMode> experienceMode;

    public WaystonesPlayerModule(Supplier<PlayerTeleportExperienceMode> experienceMode) {
        this.experienceMode = Objects.requireNonNull(experienceMode, "experienceMode");
    }

    @Override
    public ResourceLocation getId() {
        return WaystonesPlayer.id("main");
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
