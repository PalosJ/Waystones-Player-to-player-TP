package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerDestinationListLayoutTest {
    @Test
    void namedRowsNarrowWithThePanel() {
        assertEquals(132, PlayerDestinationListLayout.rowWidth(164, false));
        assertEquals(104, PlayerDestinationListLayout.rowWidth(128, false));
    }

    @Test
    void avatarRowsKeepAClickableTwentyPixelButton() {
        int rowWidth = PlayerDestinationListLayout.rowWidth(36, true);

        assertEquals(24, rowWidth);
        assertEquals(20, rowWidth - 4);
    }

    @Test
    void scrollbarGapsStayVisibleInBothModes() {
        int rowLeft = 100;
        int scrollbarWidth = 6;

        int namedPosition = PlayerDestinationListLayout.scrollbarPosition(rowLeft, false);
        int avatarPosition = PlayerDestinationListLayout.scrollbarPosition(rowLeft, true);

        assertEquals(6, rowLeft - (namedPosition + scrollbarWidth));
        assertEquals(2, rowLeft - (avatarPosition + scrollbarWidth));
    }
}
