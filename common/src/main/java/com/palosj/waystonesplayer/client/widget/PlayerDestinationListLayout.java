package com.palosj.waystonesplayer.client.widget;

final class PlayerDestinationListLayout {
    static final int AVATAR_ROW_LEFT_OFFSET = 8;
    private static final int MAX_NAMED_ROW_WIDTH = 132;
    private static final int NAMED_ROW_HORIZONTAL_INSET = 24;
    private static final int AVATAR_ROW_WIDTH = 24;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int NAMED_SCROLLBAR_BUTTON_GAP = 6;
    private static final int AVATAR_SCROLLBAR_BUTTON_GAP = 2;

    private PlayerDestinationListLayout() {
    }

    static int rowWidth(int panelWidth, boolean avatarOnly) {
        return avatarOnly
                ? AVATAR_ROW_WIDTH
                : Math.min(MAX_NAMED_ROW_WIDTH,
                        Math.max(AVATAR_ROW_WIDTH, panelWidth - NAMED_ROW_HORIZONTAL_INSET));
    }

    static int scrollbarPosition(int rowLeft, boolean avatarOnly) {
        int gap = avatarOnly ? AVATAR_SCROLLBAR_BUTTON_GAP : NAMED_SCROLLBAR_BUTTON_GAP;
        return rowLeft - SCROLLBAR_WIDTH - gap;
    }
}
