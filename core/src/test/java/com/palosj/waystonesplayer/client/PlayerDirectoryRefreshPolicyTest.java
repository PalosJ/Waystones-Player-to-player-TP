package com.palosj.waystonesplayer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerDirectoryRefreshPolicyTest {
    @Test
    void refreshesImmediatelyForInitializationAndConnectionChanges() {
        assertTrue(PlayerDirectoryRefreshPolicy.shouldRefresh(false, false, 1, 0));
        assertTrue(PlayerDirectoryRefreshPolicy.shouldRefresh(true, true, 1, 0));
    }

    @Test
    void refreshesAtMostOnceEveryFiveTicks() {
        assertFalse(PlayerDirectoryRefreshPolicy.shouldRefresh(true, false, 4, 0));
        assertTrue(PlayerDirectoryRefreshPolicy.shouldRefresh(true, false, 5, 0));
    }

    @Test
    void reducesFullScansByAtLeastEightyPercent() {
        for (int playerCount : new int[] { 50, 200, 1000 }) {
            long lastRefresh = 0;
            int scans = 0;
            for (long tick = 1; tick <= 200; tick++) {
                if (PlayerDirectoryRefreshPolicy.shouldRefresh(true, false, tick, lastRefresh)) {
                    scans++;
                    lastRefresh = tick;
                }
            }
            assertEquals(40, scans);
            assertEquals(playerCount * 40, playerCount * scans);
            assertTrue((long) playerCount * scans <= (long) playerCount * 200 / 5);
        }
    }

    @Test
    void refreshesIfTheTickCounterMovesBackwards() {
        assertTrue(PlayerDirectoryRefreshPolicy.shouldRefresh(true, false, 2, 10));
    }
}
