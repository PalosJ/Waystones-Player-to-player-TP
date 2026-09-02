package com.palosj.waystonesptpt;

import java.util.Objects;
import java.util.function.Supplier;

import com.palosj.waystonesptpt.network.ModNetworking;
import com.palosj.waystonesptpt.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.network.BalmNetworking;
import net.blay09.mods.balm.platform.module.BalmModule;
import net.minecraft.resources.Identifier;

public final class WaystonesPTPTModule implements BalmModule {
    private final Supplier<PlayerTeleportExperienceMode> experienceMode;

    public WaystonesPTPTModule(Supplier<PlayerTeleportExperienceMode> experienceMode) {
        this.experienceMode = Objects.requireNonNull(experienceMode, "experienceMode");
    }

    @Override
    public Identifier getId() {
        return WaystonesPTPT.id("main");
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
