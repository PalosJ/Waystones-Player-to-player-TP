package com.palosj.waystonesptpt.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.widget.PlayerDestinationList;
import com.palosj.waystonesptpt.client.widget.PlayerReceivingControl;
import com.palosj.waystonesptpt.client.widget.PlayerSearchBox;
import com.palosj.waystonesptpt.client.widget.PlayerToolbarLayout;
import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.compat.WaystonesCompat;
import com.palosj.waystonesptpt.compat.WaystoneScreenControls;
import com.palosj.waystonesptpt.mixin.client.AbstractContainerScreenAccessor;
import com.palosj.waystonesptpt.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.client.gui.screens.BalmScreenUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class WaystonePlayerScreenInjector {
    private static final PlayerPanelLifecycle<Screen, PlayerPanel> PANELS = new PlayerPanelLifecycle<>();
    private static final Set<Screen> FAILED_SCREENS = Collections.newSetFromMap(new WeakHashMap<>());
    private static Object cachedConnection;
    private static UUID cachedSelf;
    private static List<PlayerDirectoryEntry> cachedDirectoryEntries = List.of();
    private static List<PlayerInfo> cachedOnlinePlayers = List.of();
    private static Map<UUID, PlayerInfo> cachedPlayerInfoById = Map.of();
    private static boolean directoryCacheInitialized;
    private static long clientTick;
    private static long lastDirectoryRefreshTick;

    private static final int HEADER_HEIGHT = 64;
    private static final int FOOTER_HEIGHT = 25;
    private static final int TITLE_Y = 20;
    private static final int EMPTY_STATE_Y_OFFSET = -20;
    private static final int SCREEN_MARGIN = 4;
    private static final int TARGET_PANEL_HEIGHT = 269;
    private static final int EARLY_LAYOUT_HEIGHT = 200;
    private static final int SEARCH_BOX_HEADER_OFFSET = HEADER_HEIGHT - 24;

    private WaystonePlayerScreenInjector() {
    }

    public static void onScreenInit(Screen candidate) {
        PlayerPanel previousPanel = PANELS.get(candidate);
        String previousQuery = previousPanel == null ? "" : previousPanel.playerList.searchQuery();
        PANELS.detach(candidate);
        FAILED_SCREENS.remove(candidate);
        try {
            if (!(candidate instanceof AbstractContainerScreen<?> screen)
                    || !WaystonesCompat.isWarpStoneMenu(screen.getMenu())) {
                return;
            }

            List<PlayerInfo> onlinePlayers = getOnlinePlayers();
            if (onlinePlayers == null) {
                return;
            }
            lastDirectoryRefreshTick = clientTick;

            LayoutAnchor anchor = findLayoutAnchor(screen);
            if (anchor == null) {
                failScreen(candidate, "Waystones player panel was not added because no compatible 26.x layout anchor was found.", null);
                return;
            }

            int panelHeight = PlayerPanelLayout.resolvePanelHeight(
                    Math.max(anchor.height(), TARGET_PANEL_HEIGHT),
                    screen.height,
                    anchor.y(),
                    SCREEN_MARGIN);
            if (panelHeight < HEADER_HEIGHT + FOOTER_HEIGHT + PlayerDestinationList.ENTRY_HEIGHT) {
                failScreen(candidate, "Waystones player panel was not added because the screen is too short for an interactive row.", null);
                return;
            }

            PlayerPanelLayout layout = PlayerPanelLayout.resolve(screen.width, anchor.x(), anchor.width());
            int deltaX = layout.waystonesX() - anchor.x();
            moveWaystonesLayout(screen, anchor, deltaX);

            PlayerPanel panel = new PlayerPanel(
                    onlinePlayers,
                    layout.panelX(),
                    anchor.y(),
                    layout.panelWidth(),
                    panelHeight,
                    layout.avatarOnly(), previousQuery);
            panel.paginationDeltaX = WaystoneScreenControls.hasPagination(screen) ? deltaX : 0;
            panel.attach(screen);
            PANELS.attach(screen, panel);
        } catch (RuntimeException error) {
            PANELS.detach(candidate);
            failScreen(candidate, "WaystonesPTPT GUI injection failed for the current screen.", error);
        }
    }

    public static void onClientTick(Screen candidate) {
        PlayerPanel panel = PANELS.get(candidate);
        if (panel == null) {
            return;
        }

        try {
            clientTick++;
            Object currentConnection = Minecraft.getInstance().getConnection();
            boolean connectionChanged = currentConnection != cachedConnection;
            if (PlayerDirectoryRefreshPolicy.shouldRefresh(
                    directoryCacheInitialized,
                    connectionChanged,
                    clientTick,
                    lastDirectoryRefreshTick)) {
                List<PlayerInfo> onlinePlayers = getOnlinePlayers();
                lastDirectoryRefreshTick = clientTick;
                if (onlinePlayers != null) {
                    panel.refresh(onlinePlayers);
                }
            }
            panel.tick();
        } catch (RuntimeException error) {
            failScreen(candidate,
                    "Waystones player list could not be refreshed; the last valid list will remain visible.",
                    error);
        }
    }

    public static void onScreenClosed(Screen candidate) {
        ReceivingClientState.clear();
        PANELS.detach(candidate);
        FAILED_SCREENS.remove(candidate);
        if (PANELS.isEmpty()) {
            resetDirectoryCache();
        }
    }

    private static List<PlayerInfo> getOnlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            resetDirectoryCache();
            return null;
        }

        UUID selfId = minecraft.player.getUUID();
        List<PlayerInfo> onlinePlayers = new ArrayList<>(connection.getListedOnlinePlayers());
        onlinePlayers.removeIf(info -> info.getProfile().id().equals(selfId));
        List<PlayerDirectoryEntry> directoryEntries = onlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(info.getProfile().id(), info.getProfile().name()))
                .toList();
        if (connection == cachedConnection
                && selfId.equals(cachedSelf)
                && PlayerListRefresh.hasSamePlayersIgnoringOrder(cachedDirectoryEntries, directoryEntries)
                && onlinePlayers.stream().allMatch(info -> cachedPlayerInfoById.get(info.getProfile().id()) == info)) {
            return cachedOnlinePlayers;
        }

        onlinePlayers.sort(Comparator
                .comparing((PlayerInfo info) -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(info -> info.getProfile().name())
                .thenComparing(info -> info.getProfile().id()));
        cachedConnection = connection;
        cachedSelf = selfId;
        cachedOnlinePlayers = List.copyOf(onlinePlayers);
        cachedDirectoryEntries = cachedOnlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(info.getProfile().id(), info.getProfile().name()))
                .toList();
        Map<UUID, PlayerInfo> byId = new HashMap<>();
        cachedOnlinePlayers.forEach(info -> byId.put(info.getProfile().id(), info));
        cachedPlayerInfoById = Map.copyOf(byId);
        directoryCacheInitialized = true;
        return cachedOnlinePlayers;
    }

    private static void resetDirectoryCache() {
        cachedConnection = null;
        cachedSelf = null;
        cachedDirectoryEntries = List.of();
        cachedOnlinePlayers = List.of();
        cachedPlayerInfoById = Map.of();
        directoryCacheInitialized = false;
        lastDirectoryRefreshTick = clientTick;
    }

    private static LayoutAnchor findLayoutAnchor(AbstractContainerScreen<?> screen) {
        AbstractWidget waystoneList = null;
        EditBox searchBox = WaystoneScreenControls.searchBox(screen);
        int maximumBottom = 0;
        List<AbstractWidget> ownedControls = WaystoneScreenControls.ownedControls(screen);
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget) {
                if (isWaystonesWidget(widget) || ownedControls.contains(widget)) {
                    maximumBottom = Math.max(maximumBottom, widget.getBottom());
                }
                if (isClassOrSuperclassNamed(widget, "net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneList")) {
                    waystoneList = widget;
                }
            }
        }

        if (waystoneList != null) {
            return new LayoutAnchor(
                    waystoneList.getX(),
                    waystoneList.getY() - HEADER_HEIGHT,
                    waystoneList.getWidth(),
                    waystoneList.getHeight() + HEADER_HEIGHT + FOOTER_HEIGHT);
        }
        if (searchBox == null) {
            return null;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int guiTop = searchBox.getY() - SEARCH_BOX_HEADER_OFFSET;
        int guiHeight = Math.max(EARLY_LAYOUT_HEIGHT, maximumBottom - guiTop + SCREEN_MARGIN);
        return new LayoutAnchor(
                accessor.waystonesptpt$getLeftPos(),
                guiTop,
                accessor.waystonesptpt$getImageWidth(),
                guiHeight);
    }

    private static boolean isClassOrSuperclassNamed(Object value, String className) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static void moveWaystonesLayout(AbstractContainerScreen<?> screen, LayoutAnchor anchor, int deltaX) {
        if (deltaX == 0) {
            return;
        }

        AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
        layout.waystonesptpt$setLeftPos(layout.waystonesptpt$getLeftPos() + deltaX);

        List<AbstractWidget> ownedControls = WaystoneScreenControls.ownedControls(screen);
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget
                    && (isWaystonesWidget(widget) || ownedControls.contains(widget))) {
                widget.setX(widget.getX() + deltaX);
            }
        }
    }

    private static boolean isWaystonesWidget(AbstractWidget widget) {
        return widget.getClass().getName().startsWith("net.blay09.mods.waystones.client.gui.widget.");
    }

    /** Called immediately after upstream replaces pagination rows, before the next render/input event. */
    public static void onWaystonesListUpdated(Screen screen) {
        PlayerPanel panel = PANELS.get(screen);
        if (panel == null || panel.paginationDeltaX == 0) {
            return;
        }
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget) {
                String name = widget.getClass().getName();
                int offset;
                switch (name) {
                    case "net.blay09.mods.waystones.client.gui.widget.WaystoneButton" -> offset = -100;
                    case "net.blay09.mods.waystones.client.gui.widget.SortWaystoneButton" -> offset = 108;
                    case "net.blay09.mods.waystones.client.gui.widget.RemoveWaystoneButton" -> offset = 122;
                    default -> { continue; }
                }
                widget.setX(screen.width / 2 + offset + panel.paginationDeltaX);
            }
        }
    }

    private static void failScreen(Screen screen, String message, RuntimeException error) {
        if (!FAILED_SCREENS.add(screen)) {
            return;
        }
        if (error == null) {
            WaystonesPTPT.LOGGER.warn(message);
        } else {
            WaystonesPTPT.LOGGER.warn(message, error);
        }
    }

    private record LayoutAnchor(int x, int y, int width, int height) {
    }

    private static final class PlayerPanel {
        private final List<PlayerInfo> initialPlayers;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean avatarOnly;
        private final String initialQuery;
        private int paginationDeltaX;
        private PlayerReceivingControl receivingControl;
        private PlayerPanelLabels labels;
        private PlayerDestinationList playerList;

        private PlayerPanel(List<PlayerInfo> onlinePlayers, int x, int y, int width, int height,
                            boolean avatarOnly, String initialQuery) {
            this.initialPlayers = onlinePlayers;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.avatarOnly = avatarOnly;
            this.initialQuery = avatarOnly ? "" : initialQuery;
        }

        private void attach(Screen screen) {
            int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
            playerList = new PlayerDestinationList(
                    x, y + HEADER_HEIGHT, width, listHeight, initialPlayers, avatarOnly, targetPlayerId -> {
                Balm.networking().sendToServer(new RequestPlayerTeleportPayload(targetPlayerId));
            });
            PlayerToolbarLayout toolbar = PlayerToolbarLayout.resolve(playerList.getRowLeft(),
                    playerList.getRowWidth(), avatarOnly);
            int toolbarY = y + HEADER_HEIGHT - 24;
            if (!avatarOnly) {
                PlayerSearchBox searchBox = new PlayerSearchBox(Minecraft.getInstance().font,
                        toolbar.left(), toolbarY, toolbar.searchWidth());
                searchBox.setValue(initialQuery);
                searchBox.setResponder(query -> {
                    playerList.setSearchQuery(query);
                    updateLabels();
                });
                BalmScreenUtils.addRenderableWidget(screen, searchBox);
            }
            receivingControl = new PlayerReceivingControl(toolbar.receivingX(), toolbarY);
            BalmScreenUtils.addRenderableWidget(screen, receivingControl);
            receivingControl.updateDirectory(initialPlayers.stream().map(info -> info.getProfile().id()).toList(), true);
            labels = new PlayerPanelLabels(x, y, width, height, initialPlayers.size(), avatarOnly);
            BalmScreenUtils.addRenderableWidget(screen, labels);

            playerList.setSearchQuery(initialQuery);
            updateLabels();
            BalmScreenUtils.addRenderableWidget(screen, playerList);
        }

        private void refresh(List<PlayerInfo> currentPlayers) {
            receivingControl.updateDirectory(currentPlayers.stream().map(info -> info.getProfile().id()).toList(), false);
            playerList.updatePlayers(currentPlayers);
            updateLabels();
        }

        private void updateLabels() {
            labels.setPlayerCount(playerList.visiblePlayerCount(), !playerList.searchQuery().isEmpty());
        }

        private void tick() {
            receivingControl.tick();
            playerList.tickVisibleEntries();
        }
    }

    private static final class PlayerPanelLabels extends AbstractWidget {
        private boolean empty;
        private boolean searching;
        private final boolean avatarOnly;
        private int playerCount;
        private final int panelHeight;
        private final int panelY;
        private final Component title = Component.translatable("gui.waystonesptpt.online_players");
        private Component heading;
        private Component emptyMessage;

        private PlayerPanelLabels(int x, int y, int width, int height, int playerCount, boolean avatarOnly) {
            super(x, y + TITLE_Y, width, Minecraft.getInstance().font.lineHeight, narrationMessage(playerCount));
            this.empty = playerCount == 0;
            this.avatarOnly = avatarOnly;
            this.playerCount = playerCount;
            this.panelHeight = height;
            this.panelY = y;
            active = false;
            updateState(playerCount, false);
        }

        private void setPlayerCount(int playerCount, boolean searching) {
            if (this.playerCount != playerCount || this.searching != searching) {
                updateState(playerCount, searching);
            }
        }

        private void updateState(int playerCount, boolean searching) {
            this.playerCount = playerCount;
            this.empty = playerCount == 0;
            this.searching = searching;
            emptyMessage = Component.translatable(searching ? "gui.waystonesptpt.no_matching_players"
                    : "gui.waystonesptpt.no_other_players").withStyle(ChatFormatting.RED);
            setMessage(searching ? Component.translatable("gui.waystonesptpt.matching_players", playerCount)
                    : narrationMessage(playerCount));
            heading = avatarOnly ? Component.literal(Integer.toString(playerCount))
                    : searching ? getMessage() : title;
            setTooltip(avatarOnly ? Tooltip.create(getMessage()) : null);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            guiGraphics.centeredText(font, heading, getX() + width / 2, getY(), 0xFFFFFFFF);
            if (empty && !avatarOnly) {
                guiGraphics.centeredText(font, emptyMessage, getX() + width / 2,
                        panelY + panelHeight / 2 + EMPTY_STATE_Y_OFFSET, 0xFFFFFFFF);
            }
        }

        @Override
        public NarratableEntry.NarrationPriority narrationPriority() {
            return isFocused()
                    ? NarratableEntry.NarrationPriority.FOCUSED
                    : NarratableEntry.NarrationPriority.NONE;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, getMessage());
        }

        private static Component narrationMessage(int playerCount) {
            if (playerCount == 0) {
                return Component.translatable("narration.waystonesptpt.online_players.empty");
            }

            return Component.translatable("gui.waystonesptpt.online_players")
                    .append(": ")
                    .append(Integer.toString(playerCount));
        }
    }
}
