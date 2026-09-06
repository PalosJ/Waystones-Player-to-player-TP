package com.palosj.waystonesptpt.client.widget;

public record PlayerToolbarLayout(int left, int searchWidth, int receivingX) {
    public static final int BUTTON_SIZE = 20;
    public static final int GAP = 2;

    public static PlayerToolbarLayout resolve(int rowLeft, int rowWidth, boolean avatarOnly) {
        if (avatarOnly) {
            return new PlayerToolbarLayout(rowLeft, 0, rowLeft);
        }
        // The native list button is four pixels narrower than its row.
        int left = rowLeft - 2;
        int searchWidth = rowWidth - GAP - BUTTON_SIZE;
        return new PlayerToolbarLayout(left, searchWidth, left + searchWidth + GAP);
    }
}
