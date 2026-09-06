package com.palosj.waystonesptpt.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;

/** Client-thread state without physical client class references, safe for packet registration on a server. */
public final class ReceivingClientState {
    private static UUID session;
    private static Set<UUID> directory = Set.of();
    private static final Map<UUID, Boolean> STATES = new HashMap<>();
    private static boolean ownAllowed = true;
    private static boolean confirmed;
    private static long nextChangeId;
    private static long pendingChange;
    private ReceivingClientState() { }

    public static UUID begin() {
        clear();
        session = UUID.randomUUID();
        return session;
    }
    public static void clear() {
        session = null;
        directory = Set.of();
        STATES.clear();
        ownAllowed = true;
        confirmed = false;
        pendingChange = 0;
    }
    public static UUID session() { return session; }
    public static boolean setDirectory(List<UUID> players) {
        Set<UUID> latest = Set.copyOf(players);
        boolean changed = !latest.equals(directory);
        directory = latest;
        STATES.keySet().retainAll(directory);
        return changed;
    }
    public static void accept(ReceivingStatePayload payload) {
        if (!payload.session().equals(session)) { return; }
        confirmed = true;
        ownAllowed = payload.ownAllowed();
        if (payload.acknowledgedChange() == pendingChange) { pendingChange = 0; }
        for (ReceivingStatePayload.Entry entry : payload.entries()) {
            if (directory.contains(entry.playerId())) { STATES.put(entry.playerId(), entry.allowed()); }
        }
    }
    public static boolean allows(UUID id) { return STATES.getOrDefault(id, true); }
    public static boolean ownAllowed() { return ownAllowed; }
    public static boolean ready() { return confirmed && pendingChange == 0; }
    public static long beginChange() {
        if (!ready()) { throw new IllegalStateException("Receiving preference is awaiting server confirmation"); }
        pendingChange = ++nextChangeId;
        return pendingChange;
    }
}
