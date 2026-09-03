package com.palosj.waystonesptpt.client.widget;

import java.util.UUID;

import com.palosj.waystonesptpt.client.SkinRetryThrottle;

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
    private static final int SKIN_RETRY_TICKS = 20;

    private final boolean avatarOnly;
    private UUID playerId;
    private ResourceLocation skinTexture;
    private PlayerInfo skinSource;
    private final SkinRetryThrottle skinRetry = new SkinRetryThrottle(SKIN_RETRY_TICKS);

    public PlayerTeleportButton(int x, int y, int width, int height, boolean avatarOnly, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.avatarOnly = avatarOnly;
        this.visible = false;
        this.active = false;
    }

    public void bind(PlayerInfo playerInfo) {
        UUID newPlayerId = playerInfo.getProfile().getId();
        if (!newPlayerId.equals(playerId) || playerInfo != skinSource) {
            playerId = newPlayerId;
            skinSource = playerInfo;
            skinTexture = null;
            skinRetry.reset();
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
                skinRetry.delayAfterFailure();
            }
        }

        if (avatarOnly) {
            if (!hasFace) {
                guiGraphics.drawCenteredString(font,
                        PlayerButtonTextLayout.firstCharacter(getMessage().getString()),
                        getX() + width / 2,
                        getY() + (height - font.lineHeight) / 2 + 1,
                        color);
            }
            return;
        }

        int textX;
        int maxWidth;
        if (hasFace) {
            textX = this.getX() + FACE_PADDING + FACE_SIZE + 4;
            maxWidth = Math.max(8, this.width - (textX - this.getX()) - 4);
        } else {
            maxWidth = Math.max(8, this.width - 8);
            textX = 0;
        }

        FormattedText name = font.substrByWidth(this.getMessage(), maxWidth);
        if (!hasFace) {
            textX = PlayerButtonTextLayout.centeredTextX(this.getX(), this.width, font.width(name));
        }
        int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
        guiGraphics.drawString(font, name.getString(), textX, textY, color, false);
    }

    public void tickSkin() {
        if (playerId == null || skinSource == null || skinTexture != null) {
            return;
        }
        if (!skinRetry.advanceAndIsReady()) {
            return;
        }
        refreshSkin(skinSource);
    }

    private void refreshSkin(PlayerInfo playerInfo) {
        try {
            skinTexture = playerInfo.getSkin().texture();
            if (skinTexture == null) {
                skinRetry.delayAfterFailure();
            } else {
                skinRetry.reset();
            }
        } catch (RuntimeException error) {
            skinTexture = null;
            skinRetry.delayAfterFailure();
        }
    }
}
