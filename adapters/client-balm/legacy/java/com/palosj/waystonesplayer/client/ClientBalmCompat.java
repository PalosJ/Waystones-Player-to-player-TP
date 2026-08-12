package com.palosj.waystonesplayer.client;

import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public final class ClientBalmCompat {
    private ClientBalmCompat() {
    }

    public static void addRenderableWidget(Screen screen, AbstractWidget widget) {
        BalmClient.getScreens().addRenderableWidget(screen, widget);
    }

    public static void sendToServer(RequestPlayerTeleportPayload payload) {
        Balm.getNetworking().sendToServer(payload);
    }
}
