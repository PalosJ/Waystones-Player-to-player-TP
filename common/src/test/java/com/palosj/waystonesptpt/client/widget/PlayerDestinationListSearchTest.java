package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.authlib.GameProfile;
import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlayerDestinationListSearchTest {
    private final PlayerInfo alice = player(1, "Alice");
    private final PlayerInfo bob = player(2, "Bob");
    private final PlayerInfo bot = player(3, "Carpet_Alice_Bot");

    @AfterEach
    void cleanup() {
        ReceivingClientState.clear();
    }

    @Test
    void clearingSearchRestoresTheSameRowsAndResetsScroll() throws ReflectiveOperationException {
        PlayerDestinationList list = directory();
        var original = List.copyOf(list.children());
        list.setScrollAmount(22);
        assertTrue(WidgetTestCompat.scrollAmount(list) > 0);
        list.setSearchQuery("ALICE");
        assertEquals(2, list.visiblePlayerCount());
        assertEquals(0, WidgetTestCompat.scrollAmount(list));
        assertSame(original.getFirst(), list.children().getFirst());
        list.setSearchQuery("no match");
        assertEquals(0, list.visiblePlayerCount());
        list.setSearchQuery("");
        assertEquals(original, list.children());
        for (int i = 0; i < original.size(); i++) {
            assertSame(original.get(i), list.children().get(i));
        }
    }

    @Test
    void anOfflineHiddenRowIsRemovedAndARejoiningPlayerGetsANewRow() {
        PlayerDestinationList list = directory();
        var oldBob = list.children().get(1);
        list.setSearchQuery("alice");
        list.updatePlayers(List.of(alice, bot));
        assertEquals(2, list.visiblePlayerCount());
        list.setSearchQuery("");
        assertEquals(2, list.visiblePlayerCount());
        list.updatePlayers(List.of(alice, player(2, "Bob"), bot));
        assertNotSame(oldBob, list.children().get(1));
    }

    @Test
    void filteringDoesNotDropReceivingStatesAndDisabledPlayersStillMatch() {
        UUID session = ReceivingClientState.begin();
        UUID bobId = new UUID(0, 2);
        ReceivingClientState.setDirectory(List.of(new UUID(0, 1), bobId, new UUID(0, 3)));
        ReceivingClientState.accept(new ReceivingStatePayload(session, 0, true,
                List.of(new ReceivingStatePayload.Entry(bobId, false))));
        PlayerDestinationList list = directory();
        list.setSearchQuery("alice");
        assertFalse(ReceivingClientState.allows(bobId));
        list.setSearchQuery("bob");
        assertEquals(1, list.visiblePlayerCount());
        PlayerTeleportButton button = (PlayerTeleportButton) list.children().getFirst().children().getFirst();
        assertFalse(button.active);
        assertEquals(session, ReceivingClientState.session());
    }

    private PlayerDestinationList directory() {
        return new PlayerDestinationList(0, 64, 164, 44, List.of(alice, bob, bot), false,
                ignored -> fail("Filtering must not request a teleport"));
    }

    private static PlayerInfo player(int id, String name) {
        return new PlayerInfo(new GameProfile(new UUID(0, id), name), false);
    }
}
