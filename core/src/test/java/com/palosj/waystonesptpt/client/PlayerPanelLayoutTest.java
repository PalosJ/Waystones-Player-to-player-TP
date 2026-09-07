package com.palosj.waystonesptpt.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerPanelLayoutTest {
    private static final int WAYSTONES_WIDTH = 270;

    @Test
    void leavesTheWaystonesScreenCenteredOnWideDisplays() {
        PlayerPanelLayout layout = resolveCentered(854);

        assertEquals(292, layout.waystonesX());
        assertEquals(118, layout.panelX());
        assertEquals(164, layout.panelWidth());
        assertEquals(PlayerPanelLayout.Mode.FULL, layout.mode());
        assertFalse(layout.avatarOnly());
    }

    @Test
    void movesWaystonesOnlyAsFarAsNeededAtAutoScaleWidth() {
        PlayerPanelLayout layout = resolveCentered(480);

        assertEquals(178, layout.waystonesX());
        assertEquals(4, layout.panelX());
        assertEquals(164, layout.panelWidth());
        assertEquals(PlayerPanelLayout.Mode.FULL, layout.mode());
    }

    @Test
    void continuouslyNarrowsTheNamedPanelToItsMinimum() {
        PlayerPanelLayout compact = resolveCentered(426);
        PlayerPanelLayout minimum = resolveCentered(416);

        assertEquals(138, compact.panelWidth());
        assertEquals(152, compact.waystonesX());
        assertEquals(PlayerPanelLayout.Mode.COMPACT, compact.mode());
        assertEquals(128, minimum.panelWidth());
        assertEquals(142, minimum.waystonesX());
        assertEquals(PlayerPanelLayout.Mode.COMPACT, minimum.mode());
    }

    @Test
    void respectsTheFullToCompactBoundaryAt164And163Pixels() {
        PlayerPanelLayout fullBoundary = resolveCentered(452);
        PlayerPanelLayout compactBoundary = resolveCentered(451);

        assertEquals(164, fullBoundary.panelWidth());
        assertEquals(PlayerPanelLayout.Mode.FULL, fullBoundary.mode());
        assertEquals(163, compactBoundary.panelWidth());
        assertEquals(PlayerPanelLayout.Mode.COMPACT, compactBoundary.mode());
    }

    @Test
    void switchesToTheAvatarRailBelowTheNamedMinimum() {
        PlayerPanelLayout layout = resolveCentered(415);

        assertEquals(36, layout.panelWidth());
        assertEquals(PlayerPanelLayout.Mode.AVATARS, layout.mode());
        assertTrue(layout.avatarOnly());
    }

    @Test
    void fitsTheAvatarRailAndWaystonesAtTheNormalMinimumWidth() {
        PlayerPanelLayout layout = resolveCentered(320);

        assertEquals(2, layout.panelX());
        assertEquals(48, layout.waystonesX());
        assertEquals(36, layout.panelWidth());
        assertEquals(2, layout.screenMargin());
    }

    @Test
    void repeatedResolutionDoesNotAccumulateAnOffset() {
        PlayerPanelLayout first = resolveCentered(480);
        PlayerPanelLayout second = resolveCentered(480);

        assertEquals(first, second);
    }

    @Test
    void panelHeightNeverBecomesNegativeOnAnAbnormallyShortScreen() {
        assertEquals(0, PlayerPanelLayout.resolvePanelHeight(269, 40, 60, 4));
        assertEquals(176, PlayerPanelLayout.resolvePanelHeight(269, 240, 60, 4));
    }

    @Test
    void scrolling26ReservesTheContainerAndItsSideButtonsBeyondTheInsetList() {
        // Upstream container: 270px; scrolling list starts 25px inside it.
        // Manage/filter buttons start 8px before the container, i.e. 33px before the list.
        for (int screenWidth : new int[] {320, 416, 426, 480, 854}) {
            int listX = (screenWidth - 270) / 2 + 25;
            PlayerPanelLayout layout = PlayerPanelLayout.resolve(screenWidth, listX, 245, 33);
            int nativeSideLeft = layout.waystonesX() - 33;
            int containerRight = layout.waystonesX() + 245;
            assertTrue(layout.panelX() >= layout.screenMargin());
            assertTrue(layout.panelX() + layout.panelWidth() + PlayerPanelLayout.PANEL_GAP <= nativeSideLeft);
            assertTrue(containerRight <= screenWidth - layout.screenMargin());
        }
        PlayerPanelLayout wide = PlayerPanelLayout.resolve(854, 317, 245, 33);
        assertEquals(118, wide.panelX());
        assertEquals(317, wide.waystonesX());
        assertEquals(PlayerPanelLayout.Mode.FULL, wide.mode());
    }

    @Test
    void measured26BoundsPreserveCompactAndAvatarBreakpoints() {
        assertEquals(PlayerPanelLayout.Mode.COMPACT, PlayerPanelLayout.resolve(426, 103, 245, 33).mode());
        assertEquals(128, PlayerPanelLayout.resolve(416, 98, 245, 33).panelWidth());
        assertEquals(PlayerPanelLayout.Mode.AVATARS, PlayerPanelLayout.resolve(415, 97, 245, 33).mode());
        assertEquals(2, PlayerPanelLayout.resolve(320, 50, 245, 33).panelX());
    }

    private static PlayerPanelLayout resolveCentered(int screenWidth) {
        return PlayerPanelLayout.resolve(screenWidth, (screenWidth - WAYSTONES_WIDTH) / 2, WAYSTONES_WIDTH);
    }
}
