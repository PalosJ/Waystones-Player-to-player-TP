package com.palosj.waystonesplayer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerPanelLifecycleTest {
    @Test
    void oneHundredOpenCloseCyclesLeaveNoRegisteredPanel() {
        PlayerPanelLifecycle<Object, Object> lifecycle = new PlayerPanelLifecycle<>();
        for (int cycle = 0; cycle < 100; cycle++) {
            Object screen = new Object();
            Object panel = new Object();
            lifecycle.attach(screen, panel);
            assertSame(panel, lifecycle.get(screen));
            lifecycle.detach(screen);
        }

        assertTrue(lifecycle.isEmpty());
        assertEquals(0, lifecycle.size());
    }
}
