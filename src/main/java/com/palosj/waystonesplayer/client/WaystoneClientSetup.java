package com.palosj.waystonesplayer.client;

import com.palosj.waystonesplayer.WaystonesPlayer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class WaystoneClientSetup {

    private WaystoneClientSetup() {
    }

    public static void register(IEventBus ignored) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            if (!event.getScreen().getClass().getName().contains("WaystoneSelectionScreen")) {
                return;
            }
            try {
                Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
                injector.getMethod("onScreenInit", ScreenEvent.Init.Post.class).invoke(null, event);
            } catch (ReflectiveOperationException | LinkageError e) {
                WaystonesPlayer.LOGGER.error("Failed to inject Waystones player buttons", e);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing event) -> {
            if (!event.getScreen().getClass().getName().contains("WaystoneSelectionScreen")) {
                return;
            }
            try {
                Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
                injector.getMethod("onScreenClosing", ScreenEvent.Closing.class).invoke(null, event);
            } catch (ReflectiveOperationException | LinkageError e) {
                WaystonesPlayer.LOGGER.error("Failed to clean up Waystones screen state", e);
            }
        });
    }
}
