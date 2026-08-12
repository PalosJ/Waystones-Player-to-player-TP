package com.palosj.waystonesplayer;

import java.util.Objects;
import java.util.function.Supplier;

import com.palosj.waystonesplayer.network.ModNetworking;
import com.palosj.waystonesplayer.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.network.BalmNetworking;
import net.blay09.mods.balm.platform.module.BalmModule;
import net.minecraft.resources.Identifier;

public final class WaystonesPlayerModule implements BalmModule {
    private final Supplier<PlayerTeleportExperienceMode> experienceMode;

    public WaystonesPlayerModule(Supplier<PlayerTeleportExperienceMode> experienceMode) {
        this.experienceMode = Objects.requireNonNull(experienceMode, "experienceMode");
    }

    @Override
    public Identifier getId() {
        return WaystonesPlayer.id("main");
    }

    @Override
    public void registerNetworking(BalmNetworking networking) {
        ModNetworking.register(networking, experienceMode);
    }

    @Override
    public void initialize() {
        PlayerLifecycleHandler.register();
    }
}
