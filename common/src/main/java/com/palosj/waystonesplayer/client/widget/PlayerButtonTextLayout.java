package com.palosj.waystonesplayer.client.widget;

final class PlayerButtonTextLayout {
    private PlayerButtonTextLayout() {
    }

    static int centeredTextX(int buttonX, int buttonWidth, int renderedTextWidth) {
        return buttonX + Math.max(0, (buttonWidth - renderedTextWidth) / 2);
    }

    static String firstCharacter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }

        return value.substring(0, value.offsetByCodePoints(0, 1));
    }
}
