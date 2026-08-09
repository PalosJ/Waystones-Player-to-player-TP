package com.palosj.waystonesplayer.client.widget;

final class PlayerButtonTextLayout {
    private PlayerButtonTextLayout() {
    }

    static int centeredTextX(int buttonX, int buttonWidth, int renderedTextWidth) {
        return buttonX + Math.max(0, (buttonWidth - renderedTextWidth) / 2);
    }
}
