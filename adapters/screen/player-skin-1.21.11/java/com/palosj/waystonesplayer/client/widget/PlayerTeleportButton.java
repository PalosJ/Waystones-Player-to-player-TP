package com.palosj.waystonesplayer.client.widget;

import java.util.UUID;

import com.palosj.waystonesplayer.client.PlayerProfileCompat;
import com.palosj.waystonesplayer.client.SkinRetryThrottle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.PlayerSkin;

public class PlayerTeleportButton extends Button {
    private static final int FACE_SIZE = 16;
    private static final int FACE_PADDING = 2;
    private static final int SKIN_RETRY_TICKS = 20;
    private final boolean avatarOnly;
    private UUID playerId;
    private PlayerSkin skin;
    private PlayerInfo skinSource;
    private final SkinRetryThrottle skinRetry = new SkinRetryThrottle(SKIN_RETRY_TICKS);

    public PlayerTeleportButton(int x, int y, int width, int height, boolean avatarOnly, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.avatarOnly = avatarOnly;
        visible = false;
        active = false;
    }

    public void bind(PlayerInfo playerInfo) {
        UUID newPlayerId = PlayerProfileCompat.id(playerInfo);
        if (!newPlayerId.equals(playerId) || playerInfo != skinSource) {
            playerId = newPlayerId;
            skinSource = playerInfo;
            skin = null;
            skinRetry.reset();
        }
        Component name = Component.literal(PlayerProfileCompat.name(playerInfo));
        setMessage(name);
        setTooltip(Tooltip.create(name));
        visible = true;
        active = true;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(guiGraphics);
        Font font = Minecraft.getInstance().font;
        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        boolean hasFace = false;
        if (skin != null) {
            try {
                PlayerFaceRenderer.draw(guiGraphics, skin,
                        getX() + FACE_PADDING, getY() + FACE_PADDING, FACE_SIZE);
                hasFace = true;
            } catch (RuntimeException error) {
                skin = null;
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
            textX = getX() + FACE_PADDING + FACE_SIZE + 4;
            maxWidth = Math.max(8, width - (textX - getX()) - 4);
        } else {
            maxWidth = Math.max(8, width - 8);
            textX = 0;
        }
        FormattedText name = font.substrByWidth(getMessage(), maxWidth);
        if (!hasFace) {
            textX = PlayerButtonTextLayout.centeredTextX(getX(), width, font.width(name));
        }
        int textY = getY() + (height - font.lineHeight) / 2 + 1;
        guiGraphics.drawString(font, name.getString(), textX, textY, color, false);
    }

    public void tickSkin() {
        if (playerId == null || skinSource == null || skin != null) {
            return;
        }
        if (!skinRetry.advanceAndIsReady()) {
            return;
        }
        refreshSkin(skinSource);
    }

    private void refreshSkin(PlayerInfo playerInfo) {
        try {
            skin = playerInfo.getSkin();
            if (skin == null) {
                skinRetry.delayAfterFailure();
            } else {
                skinRetry.reset();
            }
        } catch (RuntimeException error) {
            skin = null;
            skinRetry.delayAfterFailure();
        }
    }
}
