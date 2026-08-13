package com.palosj.waystonesplayer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.widget.PlayerDestinationList;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.mixin.client.AbstractContainerScreenAccessor;
import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WaystonePlayerScreenInjector {
    private static final AtomicBoolean LAYOUT_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean LAYOUT_SYNC_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean DIRECTORY_REFRESH_FAILURE_LOGGED = new AtomicBoolean();
    private static final Map<Screen, PlayerPanel> PANELS = new WeakHashMap<>();
    private static final Map<WaystoneSelectionScreenBase, WaystonesLayoutState> ACTIVE_LAYOUTS = new WeakHashMap<>();
    private static Object cachedConnection;
    private static UUID cachedSelf;
    private static long cachedDirectoryFingerprint;
    private static List<PlayerInfo> cachedOnlinePlayers = List.of();
    private static boolean directoryCacheInitialized;
    private static final int HEADER_HEIGHT = 64;
    private static final int FOOTER_HEIGHT = 25;
    private static final int TITLE_Y = 20;
    private static final int EMPTY_STATE_Y_OFFSET = -20;
    private static final int TARGET_PANEL_HEIGHT = 269;
    private static final int SCREEN_MARGIN = 4;
    private static final int PAGE_BUTTON_WIDTH = 95;
    private static final int SEARCH_BOX_WIDTH = 198;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int PREVIOUS_PAGE_X_OFFSET = -100;
    private static final int NEXT_PAGE_X_OFFSET = 5;
    private static final String WAYSTONES_WIDGET_PACKAGE = "net.blay09.mods.waystones.client.gui.widget.";

    private WaystonePlayerScreenInjector() {
    }

    public static void onScreenInit(Screen candidate) {
        PANELS.remove(candidate);
        if (candidate instanceof WaystoneSelectionScreenBase previous) {
            ACTIVE_LAYOUTS.remove(previous);
        }

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

            EditBox searchBox = findSearchBox(screen);
            if (searchBox == null) {
                logLayoutFailure("the search box layout anchor was unavailable");
                return;
            }

            int contentCenter = screen.width / 2;
            List<AbstractWidget> initialWidgets = findWaystonesWidgets(screen, searchBox, contentCenter);
            if (initialWidgets.isEmpty()) {
                logLayoutFailure("the upstream controls were unavailable");
                return;
            }

            Bounds bounds = boundsOf(initialWidgets);
            int guiTop = searchBox.getY() - (HEADER_HEIGHT - 24);
            int desiredPanelHeight = Math.max(TARGET_PANEL_HEIGHT, bounds.bottom() - guiTop);
            PlayerPanelLayout layout = PlayerPanelLayout.resolve(
                    screen.width,
                    bounds.left(),
                    bounds.right() - bounds.left());
            int panelHeight = PlayerPanelLayout.resolvePanelHeight(
                    desiredPanelHeight,
                    screen.height,
                    guiTop,
                    SCREEN_MARGIN);
            if (panelHeight < HEADER_HEIGHT + FOOTER_HEIGHT + PlayerDestinationList.ENTRY_HEIGHT) {
                logLayoutFailure("the screen was too short for an interactive row");
                return;
            }

            int shiftX = layout.waystonesX() - bounds.left();
            if (shiftX != 0) {
                WaystonesLayoutState layoutState = new WaystonesLayoutState(screen, shiftX, initialWidgets);
                layoutState.synchronize(screen);
                ACTIVE_LAYOUTS.put(screen, layoutState);
            }

            PlayerPanel panel = new PlayerPanel(
                    onlinePlayers,
                    layout.panelX(),
                    guiTop,
                    layout.panelWidth(),
                    panelHeight,
                    layout.avatarOnly());
            panel.attach(screen);
            PANELS.put(screen, panel);
        } catch (Exception error) {
            WaystonesPlayer.LOGGER.error("WaystonesPlayer GUI injection failed", error);
        }
    }

    public static void onScreenRender(Screen candidate) {
        synchronizeLayout(candidate);
    }

    public static void onClientTick(Screen candidate) {
        synchronizeLayout(candidate);
        PlayerPanel panel = PANELS.get(candidate);
        if (panel == null) {
            return;
        }

        try {
            List<PlayerInfo> onlinePlayers = getOnlinePlayers();
            if (onlinePlayers != null) {
                panel.refresh(onlinePlayers);
            }
        } catch (RuntimeException error) {
            if (DIRECTORY_REFRESH_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.warn(
                        "Waystones player list could not be refreshed; the last valid list will remain visible.",
                        error);
            }
        }
    }

    private static void synchronizeLayout(Screen candidate) {
        if (!(candidate instanceof WaystoneSelectionScreenBase screen)) {
            return;
        }
        WaystonesLayoutState layoutState = ACTIVE_LAYOUTS.get(screen);
        if (layoutState == null) {
            return;
        }

        try {
            layoutState.synchronize(screen);
        } catch (RuntimeException error) {
            ACTIVE_LAYOUTS.remove(screen);
            if (LAYOUT_SYNC_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error(
                        "WaystonesPlayer could not keep recreated Waystones controls aligned",
                        error);
            }
        }
    }

    private static List<PlayerInfo> getOnlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            cachedConnection = null;
            cachedSelf = null;
            cachedOnlinePlayers = List.of();
            directoryCacheInitialized = false;
            return null;
        }

        UUID selfId = minecraft.player.getUUID();
        long fingerprint = 0xcbf29ce484222325L;
        int count = 0;
        for (PlayerInfo info : connection.getListedOnlinePlayers()) {
            fingerprint = mixFingerprint(fingerprint, PlayerProfileCompat.id(info).getMostSignificantBits());
            fingerprint = mixFingerprint(fingerprint, PlayerProfileCompat.id(info).getLeastSignificantBits());
            fingerprint = mixFingerprint(fingerprint, PlayerProfileCompat.name(info).hashCode());
            count++;
        }
        fingerprint = mixFingerprint(fingerprint, count);
        if (directoryCacheInitialized
                && connection == cachedConnection
                && selfId.equals(cachedSelf)
                && fingerprint == cachedDirectoryFingerprint) {
            return cachedOnlinePlayers;
        }

        List<PlayerInfo> onlinePlayers = new ArrayList<>(connection.getListedOnlinePlayers());
        onlinePlayers.removeIf(info -> PlayerProfileCompat.id(info).equals(selfId));
        onlinePlayers.sort(Comparator
                .comparing(PlayerProfileCompat::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PlayerProfileCompat::name)
                .thenComparing(PlayerProfileCompat::id));
        cachedConnection = connection;
        cachedSelf = selfId;
        cachedDirectoryFingerprint = fingerprint;
        cachedOnlinePlayers = List.copyOf(onlinePlayers);
        directoryCacheInitialized = true;
        return cachedOnlinePlayers;
    }

    private static long mixFingerprint(long hash, long value) {
        return (hash ^ value) * 0x100000001b3L;
    }

    private static EditBox findSearchBox(WaystoneSelectionScreenBase screen) {
        EditBox closestToCenter = null;
        int closestDistance = Integer.MAX_VALUE;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof EditBox editBox
                    && editBox.getWidth() == SEARCH_BOX_WIDTH
                    && editBox.getHeight() == SEARCH_BOX_HEIGHT) {
                int distance = Math.abs(editBox.getX() + editBox.getWidth() / 2 - screen.width / 2);
                if (distance < closestDistance) {
                    closestToCenter = editBox;
                    closestDistance = distance;
                }
            }
        }
        return closestToCenter;
    }

    private static List<AbstractWidget> findWaystonesWidgets(
            WaystoneSelectionScreenBase screen,
            EditBox searchBox,
            int contentCenter) {
        List<AbstractWidget> result = new ArrayList<>();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget
                    && (widget == searchBox || isPageButton(widget, contentCenter) || isWaystonesWidget(widget))) {
                result.add(widget);
            }
        }
        return result;
    }

    private static Bounds boundsOf(List<AbstractWidget> widgets) {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (AbstractWidget widget : widgets) {
            left = Math.min(left, widget.getX());
            top = Math.min(top, widget.getY());
            right = Math.max(right, widget.getRight());
            bottom = Math.max(bottom, widget.getBottom());
        }
        return new Bounds(left, top, right, bottom);
    }

    private static boolean isWaystonesWidget(AbstractWidget widget) {
        return widget.getClass().getName().startsWith(WAYSTONES_WIDGET_PACKAGE);
    }

    private static boolean isPageButton(AbstractWidget widget, int contentCenter) {
        if (!(widget instanceof Button) || widget.getWidth() != PAGE_BUTTON_WIDTH) {
            return false;
        }
        int x = widget.getX();
        return x == contentCenter + PREVIOUS_PAGE_X_OFFSET
                || x == contentCenter + NEXT_PAGE_X_OFFSET;
    }

    private static void logLayoutFailure(String reason) {
        if (LAYOUT_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            WaystonesPlayer.LOGGER.warn("Waystones player panel was not added because {}.", reason);
        }
    }

    private record Bounds(int left, int top, int right, int bottom) {
    }

    private static final class WaystonesLayoutState {
        private final int baseLeftPos;
        private final int shiftX;
        private final Map<AbstractWidget, Integer> baseX = new WeakHashMap<>();

        private WaystonesLayoutState(
                WaystoneSelectionScreenBase screen,
                int shiftX,
                List<AbstractWidget> initialWidgets) {
            this.baseLeftPos = ((AbstractContainerScreenAccessor) screen).waystonesplayer$getLeftPos();
            this.shiftX = shiftX;
            for (AbstractWidget widget : initialWidgets) {
                baseX.put(widget, widget.getX());
            }
        }

        private void synchronize(WaystoneSelectionScreenBase screen) {
            ((AbstractContainerScreenAccessor) screen).waystonesplayer$setLeftPos(baseLeftPos + shiftX);

            Set<AbstractWidget> currentWidgets = Collections.newSetFromMap(new IdentityHashMap<>());
            for (GuiEventListener listener : screen.children()) {
                if (!(listener instanceof AbstractWidget widget)
                        || (!baseX.containsKey(widget) && !isWaystonesWidget(widget))) {
                    continue;
                }
                int originalX = baseX.computeIfAbsent(widget, ignored -> widget.getX());
                widget.setX(originalX + shiftX);
                currentWidgets.add(widget);
            }
            baseX.keySet().retainAll(currentWidgets);
        }
    }

    private static final class PlayerPanel {
        private final List<PlayerInfo> initialPlayers;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean avatarOnly;
        private PlayerPanelLabels labels;
        private PlayerDestinationList playerList;

        private PlayerPanel(
                List<PlayerInfo> onlinePlayers,
                int x,
                int y,
                int width,
                int height,
                boolean avatarOnly) {
            this.initialPlayers = onlinePlayers;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.avatarOnly = avatarOnly;
        }

        private void attach(Screen screen) {
            labels = new PlayerPanelLabels(x, y, width, height, initialPlayers.size(), avatarOnly);
            ClientBalmCompat.addRenderableWidget(screen, labels);

            int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
            playerList = new PlayerDestinationList(
                    x,
                    y + HEADER_HEIGHT,
                    width,
                    listHeight,
                    initialPlayers,
                    avatarOnly,
                    targetPlayerId -> ClientBalmCompat.sendToServer(
                            new RequestPlayerTeleportPayload(targetPlayerId)));
            ClientBalmCompat.addRenderableWidget(screen, playerList);
        }

        private void refresh(List<PlayerInfo> currentPlayers) {
            playerList.updatePlayers(currentPlayers);
            labels.setPlayerCount(currentPlayers.size());
        }
    }

    private static final class PlayerPanelLabels extends AbstractWidget {
        private boolean empty;
        private final boolean avatarOnly;
        private int playerCount;
        private final int panelHeight;
        private final Component title = Component.translatable("gui.waystonesplayer.online_players");
        private final Component emptyMessage = Component.translatable("gui.waystonesplayer.no_other_players")
                .copy()
                .withStyle(ChatFormatting.RED);

        private PlayerPanelLabels(int x, int y, int width, int height, int playerCount, boolean avatarOnly) {
            super(x, y, width, HEADER_HEIGHT, narrationMessage(playerCount));
            this.empty = playerCount == 0;
            this.avatarOnly = avatarOnly;
            this.playerCount = playerCount;
            this.panelHeight = height;
            updateState(playerCount);
        }

        private void setPlayerCount(int playerCount) {
            if (this.playerCount != playerCount) {
                updateState(playerCount);
            }
        }

        private void updateState(int playerCount) {
            this.playerCount = playerCount;
            this.empty = playerCount == 0;
            setMessage(narrationMessage(playerCount));
            setTooltip(avatarOnly ? Tooltip.create(getMessage()) : null);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            Component heading = avatarOnly ? Component.literal(Integer.toString(playerCount)) : title;
            guiGraphics.drawCenteredString(font, heading, getX() + width / 2, getY() + TITLE_Y, 0xFFFFFFFF);
            if (empty && !avatarOnly) {
                guiGraphics.drawCenteredString(font, emptyMessage, getX() + width / 2,
                        getY() + panelHeight / 2 + EMPTY_STATE_Y_OFFSET, 0xFFFFFFFF);
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
                return Component.translatable("narration.waystonesplayer.online_players.empty");
            }
            return Component.translatable("gui.waystonesplayer.online_players")
                    .append(": ")
                    .append(Integer.toString(playerCount));
        }
    }
}
