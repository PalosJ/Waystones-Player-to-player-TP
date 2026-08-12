package com.palosj.waystonesplayer.client;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();
    private static Method initMethod;
    private static Method renderMethod;
    private static Method tickMethod;
    private static boolean injectorUnavailable;

    private WaystoneClientSetup() {
    }

    public static void register() {
        ScreenCallback.Init.After.EVENT.register(WaystoneClientSetup::onScreenInit);
        ScreenCallback.Render.BEFORE.register(WaystoneClientSetup::onScreenRender);
        ScreenCallback.Opening.EVENT.register(WaystoneClientSetup::onScreenOpening);
        ClientTickCallback.AFTER.register(WaystoneClientSetup::onClientTick);
    }

    private static void onScreenInit(Screen screen) {
        invoke(Action.INIT, screen);
    }

    private static void onScreenRender(
            Screen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float tickDelta) {
        invoke(Action.RENDER, screen);
    }

    private static void onClientTick(Minecraft minecraft) {
        invoke(Action.TICK, minecraft.screen);
    }

    private static void invoke(Action action, Screen screen) {
        if (injectorUnavailable || !isWaystoneSelectionScreen(screen)) {
            return;
        }
        try {
            injectorMethod(action).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            injectorUnavailable = true;
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error("Failed to initialize or refresh Waystones player buttons", error);
            }
        }
    }

    private static Screen onScreenOpening(Screen screen) {
        if (isWaystoneSelectionScreen(screen)) {
            return screen;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            WaystonesCompat.stopUsingWarpStone(minecraft.player);
        }
        return screen;
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
