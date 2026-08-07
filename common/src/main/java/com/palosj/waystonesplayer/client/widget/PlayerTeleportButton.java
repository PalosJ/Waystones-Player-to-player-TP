package com.palosj.waystonesplayer.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

public class PlayerTeleportButton extends Button {
    private static final int FACE_SIZE = 16;
    private static final int FACE_PADDING = 2;

    private ResourceLocation skinTexture;

    public PlayerTeleportButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.visible = false;
        this.active = false;
    }

    public void bind(PlayerInfo playerInfo) {
        try {
            this.skinTexture = playerInfo.getSkin().texture();
        } catch (RuntimeException e) {
            this.skinTexture = null;
        }
        Component name = Component.literal(playerInfo.getProfile().getName());
        this.setMessage(name);
        this.setTooltip(Tooltip.create(name));
        this.visible = true;
        this.active = true;
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        boolean hasFace = false;
        if (skinTexture != null) {
            try {
                PlayerFaceRenderer.draw(guiGraphics, skinTexture, this.getX() + FACE_PADDING, this.getY() + FACE_PADDING, FACE_SIZE);
                hasFace = true;
            } catch (RuntimeException e) {
                skinTexture = null;
            }
        }

        int textX = hasFace
                ? this.getX() + FACE_PADDING + FACE_SIZE + 4
                : this.getX() + (this.width - font.width(this.getMessage())) / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        int maxWidth = Math.max(8, this.width - (textX - this.getX()) - 4);
        FormattedText name = font.substrByWidth(this.getMessage(), maxWidth);
        guiGraphics.drawString(font, name.getString(), textX, textY, color, false);
    }
}
