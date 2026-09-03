package com.palosj.waystonesptpt.client;

public record PlayerPanelLayout(
        int waystonesX,
        int panelX,
        int panelWidth,
        int screenMargin,
        Mode mode) {
    public static final int FULL_PANEL_WIDTH = 164;
    public static final int MIN_NAMED_PANEL_WIDTH = 128;
    public static final int AVATAR_PANEL_WIDTH = 36;
    public static final int NAMED_SCREEN_MARGIN = 4;
    public static final int AVATAR_SCREEN_MARGIN = 2;
    public static final int WAYSTONES_SIDE_BUTTON_LEFT_OFFSET = 8;
    public static final int PANEL_GAP = 2;

    public static PlayerPanelLayout resolve(int screenWidth, int preferredWaystonesX, int waystonesWidth) {
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

    public static int resolvePanelHeight(int desiredHeight, int screenHeight, int panelY, int screenMargin) {
        int availableHeight = Math.max(0, screenHeight - panelY - Math.max(0, screenMargin));
        return Math.min(Math.max(0, desiredHeight), availableHeight);
    }

    public boolean avatarOnly() {
        return mode == Mode.AVATARS;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    public enum Mode {
        FULL,
        COMPACT,
        AVATARS
    }
}
