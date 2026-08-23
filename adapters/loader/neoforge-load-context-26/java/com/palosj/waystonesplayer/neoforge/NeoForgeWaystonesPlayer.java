package com.palosj.waystonesplayer.neoforge;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.WaystonesPlayerModule;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(WaystonesPlayer.MODID)
public final class NeoForgeWaystonesPlayer {
    public NeoForgeWaystonesPlayer(ModContainer modContainer, IEventBus modEventBus) {
        NeoForgeConfig.register(modContainer);
        Balm.initializeMod(
                WaystonesPlayer.MODID,
                new NeoForgeLoadContext(modContainer, modEventBus),
                new WaystonesPlayerModule(NeoForgeConfig::experienceMode));
    }
}
