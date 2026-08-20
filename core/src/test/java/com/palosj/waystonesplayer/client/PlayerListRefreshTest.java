package com.palosj.waystonesplayer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerListRefreshTest {
    private static final PlayerDirectoryEntry ALPHA = player(1, "Alpha");
    private static final PlayerDirectoryEntry BRAVO = player(2, "Bravo");
    private static final PlayerDirectoryEntry CHARLIE = player(3, "Charlie");

    @Test
    void identicalPlayersDoNotTriggerARebuild() {
        assertFalse(PlayerListRefresh.hasChanged(List.of(ALPHA, BRAVO), List.of(ALPHA, BRAVO)));
        assertTrue(PlayerListRefresh.hasChanged(List.of(ALPHA), List.of(ALPHA, BRAVO)));
        assertTrue(PlayerListRefresh.hasChanged(List.of(ALPHA), List.of(player(1, "Renamed"))));
    }

    @Test
    void stableDirectoryComparisonIgnoresConnectionOrderingButNotIdentityOrName() {
        assertTrue(PlayerListRefresh.hasSamePlayersIgnoringOrder(
                List.of(ALPHA, BRAVO),
                List.of(BRAVO, ALPHA)));
        assertFalse(PlayerListRefresh.hasSamePlayersIgnoringOrder(
                List.of(ALPHA, BRAVO),
                List.of(ALPHA, CHARLIE)));
        assertFalse(PlayerListRefresh.hasSamePlayersIgnoringOrder(
                List.of(player(1, "FB")),
                List.of(player(1, "Ea"))));
        assertFalse(PlayerListRefresh.hasSamePlayersIgnoringOrder(
                List.of(ALPHA),
                List.of(ALPHA, ALPHA)));
    }

    @Test
    void equalJavaNameHashesStillDetectAnExactRename() {
        assertEquals("FB".hashCode(), "Ea".hashCode());
        assertTrue(PlayerListRefresh.hasChanged(
                List.of(player(1, "FB")),
                List.of(player(1, "Ea"))));
    }

    @Test
    void joiningBeforeTheVisibleAnchorKeepsTheSamePlayerAtTheTop() {
        double restored = PlayerListRefresh.restoreScrollAmount(
                List.of(BRAVO, CHARLIE),
                List.of(ALPHA, BRAVO, CHARLIE),
                27,
                22);

        assertEquals(49, restored);
    }

    @Test
    void removingTheAnchorFallsBackToTheSameBoundedIndex() {
        double restored = PlayerListRefresh.restoreScrollAmount(
                List.of(ALPHA, BRAVO, CHARLIE),
                List.of(ALPHA, CHARLIE),
                27,
                22);

        assertEquals(27, restored);
    }

    @Test
    void focusSurvivesOnlyWhileThePlayerStillExists() {
        assertEquals(BRAVO.id(), PlayerListRefresh.restoreFocusedPlayer(BRAVO.id(), List.of(ALPHA, BRAVO)));
        assertNull(PlayerListRefresh.restoreFocusedPlayer(BRAVO.id(), List.of(ALPHA)));
    }

    @Test
    void removedFocusFallsBackToTheNearestSurvivingIndex() {
        assertEquals(CHARLIE.id(), PlayerListRefresh.restoreFocusedPlayer(
                BRAVO.id(),
                List.of(ALPHA, BRAVO, CHARLIE),
                List.of(ALPHA, CHARLIE)));
        assertEquals(ALPHA.id(), PlayerListRefresh.restoreFocusedPlayer(
                CHARLIE.id(),
                List.of(ALPHA, BRAVO, CHARLIE),
                List.of(ALPHA)));
        assertNull(PlayerListRefresh.restoreFocusedPlayer(
                ALPHA.id(),
                List.of(ALPHA),
                List.of()));
    }

    private static PlayerDirectoryEntry player(long suffix, String name) {
        return new PlayerDirectoryEntry(new UUID(0, suffix), name);
    }
}
