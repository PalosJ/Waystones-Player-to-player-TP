package com.palosj.waystonesplayer.client;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.client.OpenScreenEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenInitEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";

    private WaystoneClientSetup() {
    }

    public static void register(BalmEvents events) {
        events.onEvent(ScreenInitEvent.Post.class, WaystoneClientSetup::onScreenInit);
        events.onEvent(OpenScreenEvent.class, WaystoneClientSetup::onScreenOpening);
    }

    private static void onScreenInit(ScreenInitEvent.Post event) {
        Screen screen = event.getScreen();
        if (!isWaystoneSelectionScreen(screen)) {
            return;
        }

        try {
            Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
            injector.getMethod("onScreenInit", Screen.class).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            WaystonesPlayer.LOGGER.error("Failed to inject Waystones player buttons", e);
        }
    }

    private static void onScreenOpening(OpenScreenEvent event) {
        if (isWaystoneSelectionScreen(event.getNewScreen())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.player != null) {
                WaystonesCompat.stopUsingWarpStone(minecraft.player);
            }
        } catch (RuntimeException e) {
            WaystonesPlayer.LOGGER.error("Failed to clean up Waystones screen state", e);
        }
    }

    private static boolean isWaystoneSelectionScreen(Screen screen) {
        return screen != null && screen.getClass().getName().contains(WAYSTONE_SELECTION_SCREEN);
    }
}
