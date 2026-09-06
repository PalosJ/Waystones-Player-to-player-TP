package com.palosj.waystonesptpt.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.palosj.waystonesptpt.compat.WaystonesCompat;
import com.palosj.waystonesptpt.network.payload.ReceivingDirectoryPayload;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;
import com.palosj.waystonesptpt.network.payload.UpdateReceivingPayload;
import com.palosj.waystonesptpt.network.ModNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** All methods run on the server thread. Subscriptions never supply new directory UUIDs. */
public final class PlayerReceivingService {
    private static final Map<UUID, Subscription> SUBSCRIPTIONS = new HashMap<>();
    private PlayerReceivingService() { }

    public static boolean allows(MinecraftServer server, UUID targetId) {
        return PlayerReceivingData.get(server).allows(targetId);
    }

    public static boolean hasSession(ServerPlayer sender) {
        Subscription subscription = SUBSCRIPTIONS.get(sender.getUUID());
        return subscription != null && subscription.sender == sender && subscription.valid();
    }

    public static void requestDirectory(ServerPlayer sender, ReceivingDirectoryPayload payload) {
        if (!validMenu(sender)) { return; }
        Subscription subscription = SUBSCRIPTIONS.get(sender.getUUID());
        if (payload.replace()) {
            subscription = new Subscription(sender, sender.containerMenu, payload.session());
            SUBSCRIPTIONS.put(sender.getUUID(), subscription);
        }
        if (subscription == null || !subscription.matches(sender, payload.session())) { return; }
        MinecraftServer server = sender.level().getServer();
        List<ReceivingStatePayload.Entry> entries = new ArrayList<>();
        for (UUID id : payload.playerIds()) {
            if (server.getPlayerList().getPlayer(id) != null && !id.equals(sender.getUUID())) {
                subscription.players.add(id);
                entries.add(new ReceivingStatePayload.Entry(id, allows(server, id)));
            }
        }
        send(subscription, 0, entries);
    }

    public static void updateOwnPreference(ServerPlayer sender, UpdateReceivingPayload payload) {
        Subscription subscription = SUBSCRIPTIONS.get(sender.getUUID());
        if (!validMenu(sender) || subscription == null || !subscription.matches(sender, payload.session())) { return; }
        MinecraftServer server = sender.level().getServer();
        int tick = server.getTickCount();
        // A rejected rapid toggle is still acknowledged with the authoritative value.
        if (!subscription.hasChanged || tick - subscription.lastChangeTick >= 10) {
            subscription.hasChanged = true;
            subscription.lastChangeTick = tick;
            boolean changed = PlayerReceivingData.get(server).setAllowed(sender.getUUID(), payload.allowed());
            if (changed) {
                for (Subscription observer : List.copyOf(SUBSCRIPTIONS.values())) {
                    if (observer.sender != sender && observer.players.contains(sender.getUUID())
                            && observer.valid() && observer.sender.level().getServer() == server) {
                        send(observer, 0, List.of(new ReceivingStatePayload.Entry(sender.getUUID(), payload.allowed())));
                    }
                }
            }
        }
        send(subscription, payload.changeId(), List.of());
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) { return; }
        SUBSCRIPTIONS.values().removeIf(subscription -> !subscription.valid());
        for (Subscription subscription : SUBSCRIPTIONS.values()) {
            subscription.players.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }

    public static void logout(UUID playerId) {
        SUBSCRIPTIONS.remove(playerId);
    }

    private static void send(Subscription subscription, long acknowledgedChange,
            List<ReceivingStatePayload.Entry> entries) {
        ModNetworking.sendToClient(subscription.sender, new ReceivingStatePayload(subscription.session,
                acknowledgedChange, allows(subscription.sender.level().getServer(), subscription.sender.getUUID()), entries));
    }

    private static boolean validMenu(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved()
                && WaystonesCompat.resolveWarpStoneUse(player, player.containerMenu).isPresent();
    }

    private static final class Subscription {
        private final ServerPlayer sender;
        private final AbstractContainerMenu menu;
        private final UUID session;
        private final Set<UUID> players = new HashSet<>();
        private int lastChangeTick;
        private boolean hasChanged;
        private Subscription(ServerPlayer sender, AbstractContainerMenu menu, UUID session) {
            this.sender = sender;
            this.menu = menu;
            this.session = session;
        }
        private boolean matches(ServerPlayer player, UUID expectedSession) {
            return sender == player && player.containerMenu == menu && session.equals(expectedSession);
        }
        private boolean valid() {
            return sender.containerMenu == menu && validMenu(sender)
                    && sender.level().getServer().getPlayerList().getPlayer(sender.getUUID()) == sender;
        }
    }
}
