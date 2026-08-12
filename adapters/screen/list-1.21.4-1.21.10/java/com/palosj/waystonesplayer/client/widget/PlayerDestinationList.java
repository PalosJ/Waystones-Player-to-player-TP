package com.palosj.waystonesplayer.client.widget;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.palosj.waystonesplayer.client.PlayerDirectoryEntry;
import com.palosj.waystonesplayer.client.PlayerListRefresh;

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
    private final Consumer<UUID> onPlayerSelected;
    private List<PlayerDirectoryEntry> players = List.of();

    public PlayerDestinationList(int x, int y, int width, int height, List<PlayerInfo> onlinePlayers,
                                 boolean avatarOnly, Consumer<UUID> onPlayerSelected) {
        super(Minecraft.getInstance(), width, height, y, ENTRY_HEIGHT);
        this.avatarOnly = avatarOnly;
        this.onPlayerSelected = onPlayerSelected;
        rowWidth = PlayerDestinationListLayout.rowWidth(width, avatarOnly);
        setX(x);
        updatePlayers(onlinePlayers);
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
    protected int scrollBarX() {
        return PlayerDestinationListLayout.scrollbarPosition(getRowLeft(), avatarOnly);
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    public void updatePlayers(List<PlayerInfo> onlinePlayers) {
        List<PlayerDirectoryEntry> current = onlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(info.getProfile().getId(), info.getProfile().getName()))
                .toList();
        if (!PlayerListRefresh.hasChanged(players, current)) {
            return;
        }

        double previousScrollAmount = scrollAmount();
        UUID previousFocus = focusedPlayerId();
        List<PlayerDirectoryEntry> previous = players;
        players = List.copyOf(current);

        clearEntries();
        for (PlayerInfo playerInfo : onlinePlayers) {
            addEntry(new PlayerEntry(playerInfo, onPlayerSelected));
        }

        setScrollAmount(PlayerListRefresh.restoreScrollAmount(
                previous,
                players,
                previousScrollAmount,
                ENTRY_HEIGHT));
        UUID restoredFocus = PlayerListRefresh.restoreFocusedPlayer(previousFocus, players);
        if (restoredFocus != null) {
            for (PlayerEntry entry : children()) {
                if (entry.playerId().equals(restoredFocus)) {
                    setFocused(entry);
                    entry.focusButton();
                    break;
                }
            }
        }
    }

    private UUID focusedPlayerId() {
        PlayerEntry focusedEntry = getFocused();
        if (focusedEntry != null) {
            return focusedEntry.playerId();
        }
        for (PlayerEntry entry : children()) {
            if (entry.playerButton.isFocused()) {
                return entry.playerId();
            }
        }
        return null;
    }

    public final class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
        private final PlayerTeleportButton playerButton;
        private final List<AbstractWidget> widgets;
        private final UUID playerId;

        private PlayerEntry(PlayerInfo playerInfo, Consumer<UUID> onPlayerSelected) {
            playerId = playerInfo.getProfile().getId();
            playerButton = new PlayerTeleportButton(
                    0,
                    0,
                    Math.max(20, getRowWidth() - 4),
                    20,
                    avatarOnly,
                    ignored -> onPlayerSelected.accept(playerId));
            playerButton.bind(playerInfo);
            widgets = List.of(playerButton);
        }

        private UUID playerId() {
            return playerId;
        }

        private void focusButton() {
            setFocused(playerButton);
            playerButton.setFocused(true);
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
