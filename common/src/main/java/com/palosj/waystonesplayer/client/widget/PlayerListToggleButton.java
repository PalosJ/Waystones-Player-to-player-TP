package com.palosj.waystonesplayer.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PlayerListToggleButton extends Button {
    private static final int FACE_SIZE = 16;
    private static final int PADDING = 2;

    private ResourceLocation skinTexture;
    private boolean open;

    public PlayerListToggleButton(int x, int y, OnPress onPress) {
        super(x, y, 20, 20, actionMessage(false), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(getMessage()));

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && minecraft.player != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
            if (playerInfo != null) {
                try {
                    skinTexture = playerInfo.getSkin().texture();
                } catch (RuntimeException ignored) {
                    skinTexture = null;
                }
            }
        }
    }

    public void setOpen(boolean open) {
        this.open = open;
        setMessage(actionMessage(open));
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        if (open) {
            guiGraphics.renderOutline(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 0xFFFFFFFF);
        }
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        if (skinTexture != null) {
            try {
                PlayerFaceRenderer.draw(guiGraphics, skinTexture, getX() + PADDING, getY() + PADDING, FACE_SIZE);
                return;
            } catch (RuntimeException ignored) {
                skinTexture = null;
            }
        }

        guiGraphics.drawCenteredString(font, "P", getX() + width / 2, getY() + 6, color);
    }

    private static Component actionMessage(boolean open) {
        return Component.translatable(open
                ? "gui.waystonesplayer.hide_online_players"
                : "gui.waystonesplayer.show_online_players");
    }
}
