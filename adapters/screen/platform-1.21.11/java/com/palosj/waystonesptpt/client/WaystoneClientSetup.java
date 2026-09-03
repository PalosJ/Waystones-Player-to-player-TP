package com.palosj.waystonesptpt.client;

import java.lang.reflect.Method;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.compat.WaystonesCompat;

import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class WaystoneClientSetup {
    private static final String WAYSTONE_SELECTION_SCREEN = "WaystoneSelectionScreen";
    private static Method initMethod;
    private static Method renderMethod;
    private static Method tickMethod;
    private static Method cleanupMethod;
    private static Screen injectorUnavailableScreen;
    private static Screen failureLoggedScreen;

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
        if (screen == injectorUnavailableScreen || !isWaystoneSelectionScreen(screen)) {
            return;
        }
        try {
            injectorMethod(action).invoke(null, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            injectorUnavailableScreen = screen;
            if (failureLoggedScreen != screen) {
                failureLoggedScreen = screen;
                WaystonesPTPT.LOGGER.error("Failed to initialize or refresh Waystones player buttons", error);
            }
        }
    }

    private static Screen onScreenOpening(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Screen current = minecraft.screen;
            if (isWaystoneSelectionScreen(current) && current != screen) {
                cleanupMethod().invoke(null, current);
                if (injectorUnavailableScreen == current) {
                    injectorUnavailableScreen = null;
                }
                if (failureLoggedScreen == current) {
                    failureLoggedScreen = null;
                }
            }
            if (isWaystoneSelectionScreen(screen)) {
                return screen;
            }
            if (minecraft.player != null) {
                WaystonesCompat.stopUsingWarpStone(minecraft.player);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            WaystonesPTPT.LOGGER.error("Failed to clean up Waystones screen state", error);
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
        Class<?> injector = Class.forName("com.palosj.waystonesptpt.client.WaystonePlayerScreenInjector");
        Method resolved = injector.getMethod(action.methodName, Screen.class);
        switch (action) {
            case INIT -> initMethod = resolved;
            case RENDER -> renderMethod = resolved;
            case TICK -> tickMethod = resolved;
        }
        return resolved;
    }

    private static Method cleanupMethod() throws ReflectiveOperationException {
        if (cleanupMethod != null) {
            return cleanupMethod;
        }
        Class<?> injector = Class.forName("com.palosj.waystonesptpt.client.WaystonePlayerScreenInjector");
        cleanupMethod = injector.getMethod("onScreenClosed", Screen.class);
        return cleanupMethod;
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
