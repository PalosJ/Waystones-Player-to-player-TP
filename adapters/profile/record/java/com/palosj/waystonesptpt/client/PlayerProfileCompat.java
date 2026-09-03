package com.palosj.waystonesptpt.client;

import java.util.UUID;

import net.minecraft.client.multiplayer.PlayerInfo;

public final class PlayerProfileCompat {
    private PlayerProfileCompat() {
    }

    public static UUID id(PlayerInfo playerInfo) {
        return playerInfo.getProfile().id();
    }

    public static String name(PlayerInfo playerInfo) {
        return playerInfo.getProfile().name();
    }
}
