package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerToolbarLayoutTest {
    @Test
    void fullToolbarUsesNativeGapAndOverhangsTheButtonByTwoPixelsOnEachSide() {
        PlayerToolbarLayout toolbar = PlayerToolbarLayout.resolve(100, 132, false);
        assertEquals(98, toolbar.left());
        assertEquals(110, toolbar.searchWidth());
        assertEquals(210, toolbar.receivingX());
        assertEquals(230, toolbar.receivingX() + PlayerToolbarLayout.BUTTON_SIZE);
    }

    @Test
    void everyNamedPanelWidthKeepsTheRowAndToolbarCentersAligned() {
        for (int panelWidth = 128; panelWidth <= 164; panelWidth++) {
            int rowWidth = PlayerDestinationListLayout.rowWidth(panelWidth, false);
            int rowLeft = 200 - rowWidth / 2 + 2;
            PlayerToolbarLayout toolbar = PlayerToolbarLayout.resolve(rowLeft, rowWidth, false);
            int toolbarRight = toolbar.receivingX() + PlayerToolbarLayout.BUTTON_SIZE;
            assertEquals(rowLeft * 2 + rowWidth - 4, toolbar.left() + toolbarRight);
            assertEquals(2, toolbar.receivingX() - toolbar.left() - toolbar.searchWidth());
            assertEquals(rowWidth, toolbarRight - toolbar.left());
        }
    }

    @Test
    void avatarToolbarHasNoSearchAndMatchesTheTwentyPixelPlayerButton() {
        PlayerToolbarLayout toolbar = PlayerToolbarLayout.resolve(28, 24, true);
        assertEquals(0, toolbar.searchWidth());
        assertEquals(28, toolbar.receivingX());
        assertEquals(20, PlayerToolbarLayout.BUTTON_SIZE);
    }
}
