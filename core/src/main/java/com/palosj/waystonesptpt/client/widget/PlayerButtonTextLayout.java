package com.palosj.waystonesptpt.client.widget;

public final class PlayerButtonTextLayout {
    private PlayerButtonTextLayout() {
    }

    public static int centeredTextX(int buttonX, int buttonWidth, int renderedTextWidth) {
        return buttonX + Math.max(0, (buttonWidth - renderedTextWidth) / 2);
    }

    public static String firstCharacter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }

        return value.substring(0, value.offsetByCodePoints(0, 1));
    }
}
