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
    private static final AtomicBoolean TICK_FAILURE_LOGGED = new AtomicBoolean();
    private static Method initMethod;
    private static Method tickMethod;
    private static boolean tickInjectionDisabled;

    private WaystoneClientSetup() {
    }

    public static void register(BalmEvents events) {
        events.onEvent(ScreenInitEvent.Post.class, WaystoneClientSetup::onScreenInit);
        events.onEvent(OpenScreenEvent.class, WaystoneClientSetup::onScreenOpening);
        events.onTickEvent(TickType.Client, TickPhase.End, WaystoneClientSetup::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        if (tickInjectionDisabled) {
            return;
        }
        Screen screen = minecraft.screen;
        if (!isWaystoneSelectionScreen(screen)) {
            return;
        }

        try {
            injectorMethod(false).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            tickInjectionDisabled = true;
            if (TICK_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error(
                        "Failed to refresh Waystones player buttons; live refresh is disabled for this session.",
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
}
