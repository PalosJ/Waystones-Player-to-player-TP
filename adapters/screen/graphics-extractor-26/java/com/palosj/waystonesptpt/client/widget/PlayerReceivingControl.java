package com.palosj.waystonesptpt.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PlayerReceivingControl extends BasePlayerReceivingControl {
    public PlayerReceivingControl(int x, int y) {
        super(x, y);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        int color = !active ? 0xFFA0A0A0 : receivingAllowed() ? 0xFF55FF55 : 0xFFFF5555;
        int x = getX() + 5;
        int y = getY() + 5;
        if (receivingAllowed()) {
            for (int step = 0; step < 4; step++) {
                graphics.fill(x + step, y + 4 + step, x + step + 2, y + 6 + step, color);
            }
            for (int step = 0; step < 6; step++) {
                graphics.fill(x + 3 + step, y + 7 - step, x + 5 + step, y + 9 - step, color);
            }
        } else {
            for (int step = 0; step < 8; step++) {
                graphics.fill(x + step, y + step, x + step + 2, y + step + 2, color);
                graphics.fill(x + 7 - step, y + step, x + 9 - step, y + step + 2, color);
            }
        }
    }
}
