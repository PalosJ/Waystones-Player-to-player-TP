package com.palosj.waystonesplayer.client.widget;

import java.util.UUID;

import com.palosj.waystonesplayer.client.PlayerProfileCompat;

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
    private final boolean avatarOnly;
    private UUID playerId;
    private PlayerSkin skin;
    private PlayerInfo skinSource;
    private int skinRetryTicks;

    public PlayerTeleportButton(int x, int y, int width, int height, boolean avatarOnly, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.avatarOnly = avatarOnly;
        visible = false;
        active = false;
    }

    public void bind(PlayerInfo playerInfo) {
        playerId = PlayerProfileCompat.id(playerInfo);
        skinSource = playerInfo;
        skinRetryTicks = 0;
        refreshSkin(playerInfo);
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
        refreshSkinFromConnection();
        boolean hasFace = false;
        if (skin != null) {
            try {
                PlayerFaceRenderer.draw(guiGraphics, skin,
                        getX() + FACE_PADDING, getY() + FACE_PADDING, FACE_SIZE);
                hasFace = true;
            } catch (RuntimeException error) {
                skin = null;
                skinRetryTicks = 20;
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

    private void refreshSkinFromConnection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (playerId == null || minecraft.getConnection() == null) {
            return;
        }
        PlayerInfo current = minecraft.getConnection().getPlayerInfo(playerId);
        if (current != null) {
            if (current != skinSource) {
                skinSource = current;
                skin = null;
                skinRetryTicks = 0;
            }
            if (skinRetryTicks > 0) {
                skinRetryTicks--;
                return;
            }
            refreshSkin(current);
        }
    }

    private void refreshSkin(PlayerInfo playerInfo) {
        try {
            skin = playerInfo.getSkin();
            skinRetryTicks = skin == null ? 20 : 0;
        } catch (RuntimeException error) {
            skin = null;
            skinRetryTicks = 20;
        }
    }
}
