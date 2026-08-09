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
    private final boolean avatarOnly;
    private final int rowWidth;

    public PlayerDestinationList(int x, int y, int width, int height, List<PlayerInfo> onlinePlayers,
                                 boolean avatarOnly, Consumer<UUID> onPlayerSelected) {
        super(Minecraft.getInstance(), width, height, y, ENTRY_HEIGHT);
        this.avatarOnly = avatarOnly;
        rowWidth = PlayerDestinationListLayout.rowWidth(width, avatarOnly);
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
    public int getRowLeft() {
        return avatarOnly ? getX() + PlayerDestinationListLayout.AVATAR_ROW_LEFT_OFFSET : super.getRowLeft();
    }

    @Override
    protected int getScrollbarPosition() {
        return PlayerDestinationListLayout.scrollbarPosition(getRowLeft(), avatarOnly);
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
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
                    avatarOnly,
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
