package com.palosj.waystonesplayer.client;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.balm.api.event.client.OpenScreenEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenInitEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";
    private static final AtomicBoolean INIT_FAILURE_LOGGED = new AtomicBoolean();
    private static Method initMethod;
    private static Method tickMethod;
    private static Method cleanupMethod;
    private static Screen tickInjectionDisabledScreen;
    private static Screen tickFailureLoggedScreen;

    private WaystoneClientSetup() {
    }

    public static void register(BalmEvents events) {
        events.onEvent(ScreenInitEvent.Post.class, WaystoneClientSetup::onScreenInit);
        events.onEvent(OpenScreenEvent.class, WaystoneClientSetup::onScreenOpening);
        events.onTickEvent(TickType.Client, TickPhase.End, WaystoneClientSetup::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        Screen screen = minecraft.screen;
        if (screen == tickInjectionDisabledScreen) {
            return;
        }
        if (!isWaystoneSelectionScreen(screen)) {
            return;
        }

        try {
            injectorMethod(false).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            tickInjectionDisabledScreen = screen;
            if (tickFailureLoggedScreen != screen) {
                tickFailureLoggedScreen = screen;
                WaystonesPlayer.LOGGER.error(
                        "Failed to refresh Waystones player buttons; live refresh is disabled for this screen.",
                        e);
            }
        }
    }

    private static void onScreenInit(ScreenInitEvent.Post event) {
        Screen screen = event.getScreen();
        if (!isWaystoneSelectionScreen(screen)) {
            return;
        }

        try {
            injectorMethod(true).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            if (INIT_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error("Failed to inject Waystones player buttons", e);
            }
        }
    }

    private static void onScreenOpening(OpenScreenEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Screen current = minecraft.screen;
            if (isWaystoneSelectionScreen(current) && current != event.getNewScreen()) {
                cleanupMethod().invoke(null, current);
                if (tickInjectionDisabledScreen == current) {
                    tickInjectionDisabledScreen = null;
                }
                if (tickFailureLoggedScreen == current) {
                    tickFailureLoggedScreen = null;
                }
            }
            if (isWaystoneSelectionScreen(event.getNewScreen())) {
                return;
            }
            if (minecraft.player != null) {
                WaystonesCompat.stopUsingWarpStone(minecraft.player);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            WaystonesPlayer.LOGGER.error("Failed to clean up Waystones screen state", e);
        }
    }

    private static boolean isWaystoneSelectionScreen(Screen screen) {
        return screen != null && screen.getClass().getName().contains(WAYSTONE_SELECTION_SCREEN);
    }

    private static Method injectorMethod(boolean init) throws ReflectiveOperationException {
        Method cached = init ? initMethod : tickMethod;
        if (cached != null) {
            return cached;
        }

        Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
        Method resolved = injector.getMethod(init ? "onScreenInit" : "onClientTick", Screen.class);
        if (init) {
            initMethod = resolved;
        } else {
            tickMethod = resolved;
        }
        return resolved;
    }

    private static Method cleanupMethod() throws ReflectiveOperationException {
        if (cleanupMethod != null) {
            return cleanupMethod;
        }
        Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
        cleanupMethod = injector.getMethod("onScreenClosed", Screen.class);
        return cleanupMethod;
    }
}
