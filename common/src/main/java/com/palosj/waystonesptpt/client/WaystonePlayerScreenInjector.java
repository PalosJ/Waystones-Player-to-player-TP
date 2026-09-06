package com.palosj.waystonesptpt.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.client.PlayerDirectoryRefreshPolicy;
import com.palosj.waystonesptpt.client.PlayerPanelLifecycle;
import com.palosj.waystonesptpt.client.widget.PlayerDestinationList;
import com.palosj.waystonesptpt.client.widget.PlayerReceivingControl;
import com.palosj.waystonesptpt.client.widget.PlayerSearchBox;
import com.palosj.waystonesptpt.client.widget.PlayerToolbarLayout;
import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.compat.WaystonesCompat;
import com.palosj.waystonesptpt.mixin.client.AbstractContainerScreenAccessor;
import com.palosj.waystonesptpt.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.client.gui.screens.BalmScreenUtils;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WaystonePlayerScreenInjector {
    private static final AtomicBoolean LAYOUT_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean DIRECTORY_REFRESH_FAILURE_LOGGED = new AtomicBoolean();
    private static final PlayerPanelLifecycle<Screen, PlayerPanel> PANELS = new PlayerPanelLifecycle<>();
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

    private WaystonePlayerScreenInjector() {
    }

    public static void onScreenInit(Screen candidate) {
        PlayerPanel previousPanel = PANELS.get(candidate);
        String previousQuery = previousPanel == null ? "" : previousPanel.playerList.searchQuery();
        PANELS.detach(candidate);
        try {
            if (!(candidate instanceof WaystoneSelectionScreenBase screen)) {
                return;
            }

            AbstractContainerMenu menu = screen.getMenu();
            if (!WaystonesCompat.isWarpStoneMenu(menu)) {
                return;
            }

            List<PlayerInfo> onlinePlayers = getOnlinePlayers();
            if (onlinePlayers == null) {
                return;
            }
            lastDirectoryRefreshTick = clientTick;

            AbstractWaystoneList<?> waystoneList = findWaystoneList(screen);
            if (waystoneList == null) {
                if (LAYOUT_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
                    WaystonesPTPT.LOGGER.warn(
                            "Waystones player panel was not added because the destination list was unavailable.");
                }
                return;
            }

            int guiLeft = waystoneList.getX();
            int guiTop = waystoneList.getY() - HEADER_HEIGHT;
            int guiWidth = waystoneList.getWidth();
            int guiHeight = waystoneList.getHeight() + HEADER_HEIGHT + FOOTER_HEIGHT;
            int panelHeight = PlayerPanelLayout.resolvePanelHeight(
                    Math.max(guiHeight, TARGET_PANEL_HEIGHT),
                    screen.height,
                    guiTop,
                    SCREEN_MARGIN);
            if (panelHeight < HEADER_HEIGHT + FOOTER_HEIGHT + PlayerDestinationList.ENTRY_HEIGHT) {
                if (LAYOUT_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
                    WaystonesPTPT.LOGGER.warn(
                            "Waystones player panel was not added because the screen is too short for an interactive row.");
                }
                return;
            }
            PlayerPanelLayout layout = PlayerPanelLayout.resolve(screen.width, guiLeft, guiWidth);
            moveWaystonesLayout(screen, guiLeft, guiTop, guiWidth, guiHeight,
                    layout.waystonesX() - guiLeft);

            PlayerPanel panel = new PlayerPanel(onlinePlayers, layout.panelX(), guiTop, layout.panelWidth(), panelHeight,
                    layout.avatarOnly(), previousQuery);
            panel.attach(screen);
            PANELS.attach(screen, panel);
        } catch (Exception e) {
            WaystonesPTPT.LOGGER.error("WaystonesPTPT GUI injection failed", e);
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
            if (DIRECTORY_REFRESH_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPTPT.LOGGER.warn(
                        "Waystones player list could not be refreshed; the last valid list will remain visible.",
                        error);
            }
        }
    }

    public static void onScreenClosed(Screen candidate) {
        ReceivingClientState.clear();
        PANELS.detach(candidate);
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
        onlinePlayers.removeIf(info -> info.getProfile().getId().equals(selfId));
        List<PlayerDirectoryEntry> directoryEntries = onlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(info.getProfile().getId(), info.getProfile().getName()))
                .toList();
        if (connection == cachedConnection
                && selfId.equals(cachedSelf)
                && PlayerListRefresh.hasSamePlayersIgnoringOrder(cachedDirectoryEntries, directoryEntries)
                && onlinePlayers.stream().allMatch(info ->
                        cachedPlayerInfoById.get(info.getProfile().getId()) == info)) {
            return cachedOnlinePlayers;
        }
        onlinePlayers.sort(Comparator
                .comparing((PlayerInfo info) -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(info -> info.getProfile().getName())
                .thenComparing(info -> info.getProfile().getId()));
        cachedConnection = connection;
        cachedSelf = selfId;
        cachedOnlinePlayers = List.copyOf(onlinePlayers);
        cachedDirectoryEntries = cachedOnlinePlayers.stream()
                .map(info -> new PlayerDirectoryEntry(info.getProfile().getId(), info.getProfile().getName()))
                .toList();
        Map<UUID, PlayerInfo> playerInfoById = new HashMap<>();
        cachedOnlinePlayers.forEach(info -> playerInfoById.put(info.getProfile().getId(), info));
        cachedPlayerInfoById = Map.copyOf(playerInfoById);
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

    private static AbstractWaystoneList<?> findWaystoneList(WaystoneSelectionScreenBase screen) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWaystoneList<?> waystoneList) {
                return waystoneList;
            }
        }
        return null;
    }

    private static void moveWaystonesLayout(WaystoneSelectionScreenBase screen, int guiLeft, int guiTop,
                                            int guiWidth, int guiHeight, int deltaX) {
        if (deltaX == 0) {
            return;
        }

        AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
        layout.waystonesptpt$setLeftPos(layout.waystonesptpt$getLeftPos() + deltaX);

        int minimumX = guiLeft - PlayerPanelLayout.WAYSTONES_SIDE_BUTTON_LEFT_OFFSET;
        int maximumX = guiLeft + guiWidth;
        int maximumY = guiTop + guiHeight;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget
                    && isWaystonesLayoutWidget(widget, minimumX, maximumX, guiTop, maximumY)) {
                widget.setX(widget.getX() + deltaX);
            }
        }
    }

    private static boolean isWaystonesLayoutWidget(AbstractWidget widget, int minimumX, int maximumX,
                                                    int minimumY, int maximumY) {
        if (widget.getClass().getName().startsWith("net.blay09.mods.waystones.")) {
            return true;
        }

        return widget instanceof EditBox && !(widget instanceof PlayerSearchBox)
                && widget.getRight() > minimumX
                && widget.getX() < maximumX
                && widget.getBottom() > minimumY
                && widget.getY() < maximumY;
    }

    private static final class PlayerPanel {
        private final List<PlayerInfo> initialPlayers;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean avatarOnly;
        private final String initialQuery;
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
            receivingControl.updateDirectory(initialPlayers.stream().map(info -> info.getProfile().getId()).toList(), true);
            labels = new PlayerPanelLabels(x, y, width, height, initialPlayers.size(), avatarOnly);
            BalmScreenUtils.addRenderableWidget(screen, labels);

            playerList.setSearchQuery(initialQuery);
            updateLabels();
            BalmScreenUtils.addRenderableWidget(screen, playerList);
        }

        private void refresh(List<PlayerInfo> currentPlayers) {
            receivingControl.updateDirectory(currentPlayers.stream().map(info -> info.getProfile().getId()).toList(), false);
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
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            guiGraphics.drawCenteredString(font, heading, getX() + width / 2, getY(), 0xFFFFFFFF);
            if (empty && !avatarOnly) {
                guiGraphics.drawCenteredString(font, emptyMessage, getX() + width / 2,
                        panelY + panelHeight / 2 + EMPTY_STATE_Y_OFFSET, 0xFFFFFFFF);
            }
            if (isFocused()) {
                guiGraphics.renderOutline(getX() + 1, getY() + 1, width - 2, height - 2, 0xFFFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
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
