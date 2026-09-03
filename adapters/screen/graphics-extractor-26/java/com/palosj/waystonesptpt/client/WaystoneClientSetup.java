package com.palosj.waystonesptpt.client;

import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";
    private static Screen activeWaystoneScreen;

    private WaystoneClientSetup() {
    }

    public static void register() {
        ScreenCallback.Init.After.EVENT.register(WaystoneClientSetup::onScreenInit);
        ScreenCallback.Opening.EVENT.register(WaystoneClientSetup::onScreenOpening);
        ClientTickCallback.AFTER.register(WaystoneClientSetup::onClientTick);
    }

    private static void onScreenInit(Screen screen) {
        if (isWaystoneSelectionScreen(screen)) {
            if (activeWaystoneScreen != null && activeWaystoneScreen != screen) {
                WaystonePlayerScreenInjector.onScreenClosed(activeWaystoneScreen);
            }
            activeWaystoneScreen = screen;
            WaystonePlayerScreenInjector.onScreenInit(screen);
        }
    }

    private static Screen onScreenOpening(Screen newScreen) {
        if (activeWaystoneScreen != null && activeWaystoneScreen != newScreen) {
            WaystonePlayerScreenInjector.onScreenClosed(activeWaystoneScreen);
            activeWaystoneScreen = null;
        }
        return newScreen;
    }

    private static void onClientTick(net.minecraft.client.Minecraft minecraft) {
        if (activeWaystoneScreen != null) {
            WaystonePlayerScreenInjector.onClientTick(activeWaystoneScreen);
        }
    }

    private static boolean isWaystoneSelectionScreen(Screen screen) {
        return screen != null && screen.getClass().getName().contains(WAYSTONE_SELECTION_SCREEN);
    }
}
