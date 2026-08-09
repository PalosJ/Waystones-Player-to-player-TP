package com.palosj.waystonesplayer.client;

record PlayerPanelLayout(
        int waystonesX,
        int panelX,
        int panelWidth,
        int screenMargin,
        Mode mode) {
    static final int FULL_PANEL_WIDTH = 164;
    static final int MIN_NAMED_PANEL_WIDTH = 128;
    static final int AVATAR_PANEL_WIDTH = 36;
    static final int NAMED_SCREEN_MARGIN = 4;
    static final int AVATAR_SCREEN_MARGIN = 2;
    static final int WAYSTONES_SIDE_BUTTON_LEFT_OFFSET = 8;
    static final int PANEL_GAP = 2;

    static PlayerPanelLayout resolve(int screenWidth, int preferredWaystonesX, int waystonesWidth) {
        int availableNamedWidth = screenWidth
                - NAMED_SCREEN_MARGIN * 2
                - waystonesWidth
                - WAYSTONES_SIDE_BUTTON_LEFT_OFFSET
                - PANEL_GAP;

        Mode mode;
        int panelWidth;
        int screenMargin;
        if (availableNamedWidth >= FULL_PANEL_WIDTH) {
            mode = Mode.FULL;
            panelWidth = FULL_PANEL_WIDTH;
            screenMargin = NAMED_SCREEN_MARGIN;
        } else if (availableNamedWidth >= MIN_NAMED_PANEL_WIDTH) {
            mode = Mode.COMPACT;
            panelWidth = availableNamedWidth;
            screenMargin = NAMED_SCREEN_MARGIN;
        } else {
            mode = Mode.AVATARS;
            panelWidth = AVATAR_PANEL_WIDTH;
            screenMargin = AVATAR_SCREEN_MARGIN;
        }

        int minimumWaystonesX = screenMargin
                + panelWidth
                + WAYSTONES_SIDE_BUTTON_LEFT_OFFSET
                + PANEL_GAP;
        int maximumWaystonesX = Math.max(0, screenWidth - screenMargin - waystonesWidth);
        int waystonesX = minimumWaystonesX <= maximumWaystonesX
                ? clamp(preferredWaystonesX, minimumWaystonesX, maximumWaystonesX)
                : maximumWaystonesX;
        int panelX = Math.max(0, waystonesX
                - WAYSTONES_SIDE_BUTTON_LEFT_OFFSET
                - PANEL_GAP
                - panelWidth);

        return new PlayerPanelLayout(waystonesX, panelX, panelWidth, screenMargin, mode);
    }

    boolean avatarOnly() {
        return mode == Mode.AVATARS;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    enum Mode {
        FULL,
        COMPACT,
        AVATARS
    }
}
