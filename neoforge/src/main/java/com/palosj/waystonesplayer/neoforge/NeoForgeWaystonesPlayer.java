package com.palosj.waystonesplayer.neoforge;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.WaystonesPlayerModule;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(WaystonesPlayer.MODID)
public final class NeoForgeWaystonesPlayer {
    public NeoForgeWaystonesPlayer(IEventBus modEventBus) {
        NeoForgeConfig.register();
        Balm.initializeMod(
                WaystonesPlayer.MODID,
                new NeoForgeLoadContext(modEventBus),
                new WaystonesPlayerModule(NeoForgeConfig::experienceMode));
    }
}
