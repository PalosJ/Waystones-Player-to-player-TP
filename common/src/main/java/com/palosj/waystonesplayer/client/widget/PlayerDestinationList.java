package com.palosj.waystonesplayer.client.widget;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PlayerDestinationList extends ContainerObjectSelectionList<PlayerDestinationList.PlayerEntry> {
    public static final int ENTRY_HEIGHT = 22;
    private static final int ROW_HORIZONTAL_INSET = 32;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_BUTTON_GAP = 12;
    private final int rowWidth;

    public PlayerDestinationList(int x, int y, int width, int height, List<PlayerInfo> onlinePlayers,
                                 Consumer<UUID> onPlayerSelected) {
        super(Minecraft.getInstance(), width, height, y, ENTRY_HEIGHT);
        rowWidth = Math.max(24, width - ROW_HORIZONTAL_INSET);
        setX(x);

        for (PlayerInfo playerInfo : onlinePlayers) {
            addEntry(new PlayerEntry(playerInfo, onPlayerSelected));
        }
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return getRowLeft() - SCROLLBAR_WIDTH - SCROLLBAR_BUTTON_GAP;
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    public void setPanelVisible(boolean visible) {
        this.visible = visible;
        this.active = visible;
    }

    public final class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
        private final PlayerTeleportButton playerButton;
        private final List<AbstractWidget> widgets;

        private PlayerEntry(PlayerInfo playerInfo, Consumer<UUID> onPlayerSelected) {
            playerButton = new PlayerTeleportButton(
                    0,
                    0,
                    Math.max(20, getRowWidth() - 4),
                    20,
                    ignored -> onPlayerSelected.accept(playerInfo.getProfile().getId()));
            playerButton.bind(playerInfo);
            widgets = List.of(playerButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            playerButton.setPosition(left, top + 1);
            playerButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
