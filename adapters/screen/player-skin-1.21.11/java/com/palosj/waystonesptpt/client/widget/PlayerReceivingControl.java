package com.palosj.waystonesptpt.client.widget;

public final class PlayerReceivingControl extends BasePlayerReceivingControl {
    public PlayerReceivingControl(int x, int y, int width, boolean avatarOnly) {
        super(x, y, width, avatarOnly);
    }

    @Override
    protected void renderContents(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(graphics);
        renderDefaultLabel(graphics.textRenderer());
    }
}
