package com.palosj.waystonesplayer.client;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.balm.api.event.client.OpenScreenEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenInitEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";
    private static final AtomicBoolean INIT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean REFRESH_FAILURE_LOGGED = new AtomicBoolean();
    private static Method initMethod;
    private static Method renderMethod;
    private static Method tickMethod;
    private static boolean refreshDisabled;

    private WaystoneClientSetup() {
    }

    public static void register(BalmEvents events) {
        events.onEvent(ScreenInitEvent.Post.class, WaystoneClientSetup::onScreenInit);
        events.onEvent(ScreenDrawEvent.Pre.class, WaystoneClientSetup::onScreenDraw);
        events.onEvent(OpenScreenEvent.class, WaystoneClientSetup::onScreenOpening);
        events.onTickEvent(TickType.Client, TickPhase.End, WaystoneClientSetup::onClientTick);
    }

    private static void onScreenInit(ScreenInitEvent.Post event) {
        Screen screen = event.getScreen();
        if (!isWaystoneSelectionScreen(screen)) {
            return;
        }

        try {
            injectorMethod(Action.INIT).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (INIT_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error("Failed to inject Waystones player buttons", error);
            }
        }
    }

    private static void onScreenDraw(ScreenDrawEvent.Pre event) {
        if (refreshDisabled || !isWaystoneSelectionScreen(event.getScreen())) {
            return;
        }
        invokeRefresh(Action.RENDER, event.getScreen());
    }

    private static void onClientTick(Minecraft minecraft) {
        if (refreshDisabled || !isWaystoneSelectionScreen(minecraft.screen)) {
            return;
        }
        invokeRefresh(Action.TICK, minecraft.screen);
    }

    private static void invokeRefresh(Action action, Screen screen) {
        try {
            injectorMethod(action).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            refreshDisabled = true;
            if (REFRESH_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error(
                        "Failed to synchronize Waystones player panel layout and live directory",
                        error);
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
        } catch (RuntimeException error) {
            WaystonesPlayer.LOGGER.error("Failed to clean up Waystones screen state", error);
        }
    }

    private static boolean isWaystoneSelectionScreen(Screen screen) {
        return screen != null && screen.getClass().getName().contains(WAYSTONE_SELECTION_SCREEN);
    }

    private static Method injectorMethod(Action action) throws ReflectiveOperationException {
        Method cached = switch (action) {
            case INIT -> initMethod;
            case RENDER -> renderMethod;
            case TICK -> tickMethod;
        };
        if (cached != null) {
            return cached;
        }

        Class<?> injector = Class.forName("com.palosj.waystonesplayer.client.WaystonePlayerScreenInjector");
        Method resolved = injector.getMethod(action.methodName, Screen.class);
        switch (action) {
            case INIT -> initMethod = resolved;
            case RENDER -> renderMethod = resolved;
            case TICK -> tickMethod = resolved;
        }
        return resolved;
    }

    private enum Action {
        INIT("onScreenInit"),
        RENDER("onScreenRender"),
        TICK("onClientTick");

        private final String methodName;

        Action(String methodName) {
            this.methodName = methodName;
        }
    }
}
