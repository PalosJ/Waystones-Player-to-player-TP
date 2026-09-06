package com.palosj.waystonesptpt.client.widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.palosj.waystonesptpt.client.PlayerDirectoryEntry;
import com.palosj.waystonesptpt.client.PlayerDirectorySearch;
import com.palosj.waystonesptpt.client.PlayerListRefresh;
import com.palosj.waystonesptpt.client.PlayerProfileCompat;

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
    private List<PlayerDirectoryEntry> visiblePlayers = List.of();
    private String searchQuery = "";
    private List<PlayerInfo> sourcePlayers = List.of();
    private Map<UUID, PlayerEntry> entriesById = Map.of();

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
        if (onlinePlayers == sourcePlayers) {
            return;
        }
        sourcePlayers = onlinePlayers;
        List<PlayerDirectoryEntry> current = onlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(PlayerProfileCompat.id(info), PlayerProfileCompat.name(info)))
                .toList();
        if (!PlayerListRefresh.hasChanged(players, current)) {
            for (PlayerInfo playerInfo : onlinePlayers) {
                PlayerEntry entry = entriesById.get(PlayerProfileCompat.id(playerInfo));
                if (entry != null) {
                    entry.bind(playerInfo);
                }
            }
            return;
        }

        players = List.copyOf(current);
        Map<UUID, PlayerEntry> previousEntries = new HashMap<>(entriesById);
        Map<UUID, PlayerEntry> currentEntries = new HashMap<>();
        for (PlayerInfo playerInfo : onlinePlayers) {
            UUID playerId = PlayerProfileCompat.id(playerInfo);
            PlayerEntry entry = previousEntries.remove(playerId);
            if (entry == null) {
                entry = new PlayerEntry(playerInfo, onPlayerSelected);
            } else {
                entry.bind(playerInfo);
            }
            currentEntries.put(playerId, entry);
        }
        entriesById = Map.copyOf(currentEntries);
        refreshVisiblePlayers(false);
    }

    public String searchQuery() {
        return searchQuery;
    }

    public int visiblePlayerCount() {
        return visiblePlayers.size();
    }

    public void setSearchQuery(String query) {
        if (!searchQuery.equals(query)) {
            searchQuery = query;
            refreshVisiblePlayers(true);
        }
    }

    private void refreshVisiblePlayers(boolean queryChanged) {
        List<PlayerDirectoryEntry> filtered = PlayerDirectorySearch.filter(players, searchQuery);
        if (!queryChanged && filtered.equals(visiblePlayers)) {
            return;
        }
        double previousScrollAmount = scrollAmount();
        UUID previousFocus = focusedPlayerId();
        List<PlayerDirectoryEntry> previous = visiblePlayers;
        visiblePlayers = filtered;
        PlayerEntry focusedEntry = getFocused();
        if (focusedEntry != null) {
            focusedEntry.clearButtonFocus();
        }
        setFocused(null);
        clearEntries();
        for (PlayerDirectoryEntry player : visiblePlayers) {
            addEntry(entriesById.get(player.id()));
        }

        setScrollAmount(queryChanged ? 0 : PlayerListRefresh.restoreScrollAmount(
                previous,
                visiblePlayers,
                previousScrollAmount,
                ENTRY_HEIGHT));
        UUID restoredFocus = queryChanged ? null
                : PlayerListRefresh.restoreFocusedPlayer(previousFocus, previous, visiblePlayers);
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

    public void tickVisibleEntries() {
        int first = Math.max(0, (int) Math.floor(scrollAmount() / ENTRY_HEIGHT));
        int visibleCount = Math.max(1, (getHeight() + ENTRY_HEIGHT - 1) / ENTRY_HEIGHT + 1);
        int last = Math.min(children().size(), first + visibleCount);
        for (int index = first; index < last; index++) {
            children().get(index).tick();
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
            playerId = PlayerProfileCompat.id(playerInfo);
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

        private void bind(PlayerInfo playerInfo) {
            playerButton.bind(playerInfo);
        }

        private void tick() {
            playerButton.tickSkin();
        }

        private UUID playerId() {
            return playerId;
        }

        private void focusButton() {
            setFocused(playerButton);
            playerButton.setFocused(true);
        }

        private void clearButtonFocus() {
            playerButton.setFocused(false);
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
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  boolean hovered, float partialTick) {
            playerButton.setPosition(getContentX(), getContentY());
            playerButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
