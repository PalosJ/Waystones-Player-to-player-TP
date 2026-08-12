package com.palosj.waystonesplayer.neoforge;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.network.ModNetworking;
import com.palosj.waystonesplayer.teleport.PlayerLifecycleHandler;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(WaystonesPlayer.MODID)
public final class NeoForgeWaystonesPlayer {
    public NeoForgeWaystonesPlayer(IEventBus modEventBus) {
        NeoForgeConfig.register();
        Balm.initialize(WaystonesPlayer.MODID, new NeoForgeLoadContext(modEventBus), () -> {
            ModNetworking.register(Balm.getNetworking(), NeoForgeConfig::experienceMode);
            PlayerLifecycleHandler.register(Balm.getEvents());
        });
    }
}
