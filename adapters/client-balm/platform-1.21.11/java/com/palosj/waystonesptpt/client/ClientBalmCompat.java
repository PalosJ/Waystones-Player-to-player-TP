package com.palosj.waystonesptpt.client;

import com.palosj.waystonesptpt.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.client.gui.screens.BalmScreenUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public final class ClientBalmCompat {
    private ClientBalmCompat() {
    }

    public static void addRenderableWidget(Screen screen, AbstractWidget widget) {
        BalmScreenUtils.addRenderableWidget(screen, widget);
    }

    public static void sendToServer(RequestPlayerTeleportPayload payload) {
        Balm.networking().sendToServer(payload);
    }
}
