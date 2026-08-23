package com.palosj.waystonesplayer.client.widget;

import java.util.UUID;

import com.palosj.waystonesplayer.client.SkinRetryThrottle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PlayerTeleportButton extends Button {
    private static final int FACE_SIZE = 16;
    private static final int FACE_PADDING = 2;
    private static final int SKIN_RETRY_TICKS = 20;

    private final boolean avatarOnly;
    private final SkinRetryThrottle skinRetry = new SkinRetryThrottle(SKIN_RETRY_TICKS);
    private UUID playerId;
    private PlayerSkin skin;
    private PlayerInfo skinSource;

    public PlayerTeleportButton(int x, int y, int width, int height, boolean avatarOnly, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.avatarOnly = avatarOnly;
        visible = false;
        active = false;
    }

    public void bind(PlayerInfo playerInfo) {
        UUID newPlayerId = playerInfo.getProfile().id();
        if (!newPlayerId.equals(playerId) || playerInfo != skinSource) {
            playerId = newPlayerId;
            skinSource = playerInfo;
            skin = null;
            skinRetry.reset();
        }
        Component name = Component.literal(playerInfo.getProfile().name());
        setMessage(name);
        setTooltip(Tooltip.create(name));
        visible = true;
        active = true;
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hasFace = false;
        if (skin != null) {
            try {
                PlayerFaceExtractor.extractRenderState(
                        graphics,
                        skin,
                        getX() + FACE_PADDING,
                        getY() + FACE_PADDING,
                        FACE_SIZE);
                hasFace = true;
            } catch (RuntimeException error) {
                skin = null;
                skinRetry.delayAfterFailure();
            }
        }

        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        if (avatarOnly) {
            if (!hasFace) {
                graphics.centeredText(
                        font,
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
        String name = font.plainSubstrByWidth(getMessage().getString(), maxWidth);
        if (!hasFace) {
            textX = PlayerButtonTextLayout.centeredTextX(getX(), width, font.width(name));
        }
        int textY = getY() + (height - font.lineHeight) / 2 + 1;
        graphics.text(font, name, textX, textY, color, false);
    }

    public void tickSkin() {
        if (playerId == null || skinSource == null || skin != null) {
            return;
        }
        if (!skinRetry.advanceAndIsReady()) {
            return;
        }
        try {
            skin = skinSource.getSkin();
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
