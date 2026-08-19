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
import com.palosj.waystonesplayer.client.PlayerDirectoryRefreshPolicy;
import com.palosj.waystonesplayer.client.PlayerPanelLifecycle;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WaystonePlayerScreenInjector {
    private static final AtomicBoolean LAYOUT_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean LAYOUT_SYNC_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean DIRECTORY_REFRESH_FAILURE_LOGGED = new AtomicBoolean();
    private static final PlayerPanelLifecycle<Screen, PlayerPanel> PANELS = new PlayerPanelLifecycle<>();
    private static final Map<WaystoneSelectionScreenBase, WaystonesLayoutState> ACTIVE_LAYOUTS = new WeakHashMap<>();
    private static Object cachedConnection;
    private static UUID cachedSelf;
    private static List<PlayerInfo> cachedOnlinePlayers = List.of();
    private static boolean directoryCacheInitialized;
    private static long clientTick;
    private static long lastDirectoryRefreshTick;
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
        PANELS.detach(candidate);
        if (candidate instanceof WaystoneSelectionScreenBase prior) {
            ACTIVE_LAYOUTS.remove(prior);
        }
        try {
            if (!(candidate instanceof WaystoneSelectionScreenBase screen)) {
                return;
            }
            AbstractContainerMenu menu = screen.getMenu();
            if (!WaystonesCompat.isWarpStoneMenu(menu)) {
                return;
            }
            List<PlayerInfo> players = getOnlinePlayers();
            if (players == null) {
                return;
            }
            lastDirectoryRefreshTick = clientTick;
            EditBox searchBox = findSearchBox(screen);
            if (searchBox == null) {
                logLayoutFailure("the search box layout anchor was unavailable");
                return;
            }

            int center = screen.width / 2;
            List<AbstractWidget> upstreamWidgets = findWaystonesWidgets(screen, searchBox, center);
            Bounds bounds = boundsOf(upstreamWidgets);
            int guiTop = searchBox.getY() - (HEADER_HEIGHT - 24);
            int panelHeight = PlayerPanelLayout.resolvePanelHeight(
                    Math.max(TARGET_PANEL_HEIGHT, bounds.bottom() - guiTop),
                    screen.height,
                    guiTop,
                    SCREEN_MARGIN);
            if (panelHeight < HEADER_HEIGHT + FOOTER_HEIGHT + PlayerDestinationList.ENTRY_HEIGHT) {
                logLayoutFailure("the screen was too short for an interactive row");
                return;
            }

            PlayerPanelLayout layout = PlayerPanelLayout.resolve(
                    screen.width,
                    bounds.left(),
                    bounds.right() - bounds.left());
            int shiftX = layout.waystonesX() - bounds.left();
            if (shiftX != 0) {
                WaystonesLayoutState state = new WaystonesLayoutState(screen, shiftX, upstreamWidgets);
                state.synchronize(screen);
                ACTIVE_LAYOUTS.put(screen, state);
            }

            PlayerPanel panel = new PlayerPanel(
                    players, layout.panelX(), guiTop, layout.panelWidth(), panelHeight, layout.avatarOnly());
            panel.attach(screen);
            PANELS.attach(screen, panel);
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
            clientTick++;
            Object currentConnection = Minecraft.getInstance().getConnection();
            boolean connectionChanged = currentConnection != cachedConnection;
            if (PlayerDirectoryRefreshPolicy.shouldRefresh(
                    directoryCacheInitialized,
                    connectionChanged,
                    clientTick,
                    lastDirectoryRefreshTick)) {
                List<PlayerInfo> players = getOnlinePlayers();
                lastDirectoryRefreshTick = clientTick;
                if (players != null) {
                    panel.refresh(players);
                }
            }
            panel.tick();
        } catch (RuntimeException error) {
            if (DIRECTORY_REFRESH_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.warn(
                        "Waystones player list could not be refreshed; the last valid list will remain visible.",
                        error);
            }
        }
    }

    public static void onScreenClosed(Screen candidate) {
        PANELS.detach(candidate);
        if (candidate instanceof WaystoneSelectionScreenBase screen) {
            ACTIVE_LAYOUTS.remove(screen);
        }
        if (PANELS.isEmpty()) {
            resetDirectoryCache();
        }
    }

    private static void synchronizeLayout(Screen candidate) {
        if (!(candidate instanceof WaystoneSelectionScreenBase screen)) {
            return;
        }
        WaystonesLayoutState state = ACTIVE_LAYOUTS.get(screen);
        if (state == null) {
            return;
        }
        try {
            state.synchronize(screen);
        } catch (RuntimeException error) {
            ACTIVE_LAYOUTS.remove(screen);
            if (LAYOUT_SYNC_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPlayer.LOGGER.error("WaystonesPlayer could not keep recreated controls aligned", error);
            }
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
        List<PlayerInfo> players = new ArrayList<>(connection.getListedOnlinePlayers());
        players.removeIf(info -> PlayerProfileCompat.id(info).equals(selfId));
        players.sort(Comparator
                .comparing(PlayerProfileCompat::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PlayerProfileCompat::name)
                .thenComparing(PlayerProfileCompat::id));
        cachedConnection = connection;
        cachedSelf = selfId;
        cachedOnlinePlayers = List.copyOf(players);
        directoryCacheInitialized = true;
        return cachedOnlinePlayers;
    }

    private static void resetDirectoryCache() {
        cachedConnection = null;
        cachedSelf = null;
        cachedOnlinePlayers = List.of();
        directoryCacheInitialized = false;
        lastDirectoryRefreshTick = clientTick;
    }

    private static EditBox findSearchBox(WaystoneSelectionScreenBase screen) {
        EditBox closest = null;
        int distance = Integer.MAX_VALUE;
        int expectedX = screen.width / 2 - SEARCH_BOX_WIDTH / 2;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof EditBox editBox
                    && editBox.getWidth() == SEARCH_BOX_WIDTH
                    && editBox.getHeight() == SEARCH_BOX_HEIGHT
                    && editBox.getX() == expectedX) {
                int candidate = Math.abs(editBox.getX() + editBox.getWidth() / 2 - screen.width / 2);
                if (candidate < distance) {
                    closest = editBox;
                    distance = candidate;
                }
            }
        }
        return closest;
    }

    private static List<AbstractWidget> findWaystonesWidgets(
            WaystoneSelectionScreenBase screen,
            EditBox searchBox,
            int center) {
        List<AbstractWidget> result = new ArrayList<>();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget
                    && (widget == searchBox || isPageButton(widget, center) || isWaystonesWidget(widget))) {
                result.add(widget);
            }
        }
        return result;
    }

    private static Bounds boundsOf(List<AbstractWidget> widgets) {
        if (widgets.isEmpty()) {
            throw new IllegalStateException("Waystones controls were unavailable");
        }
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

    private static boolean isPageButton(AbstractWidget widget, int center) {
        if (!(widget instanceof Button) || widget.getWidth() != PAGE_BUTTON_WIDTH) {
            return false;
        }
        return widget.getX() == center + PREVIOUS_PAGE_X_OFFSET
                || widget.getX() == center + NEXT_PAGE_X_OFFSET;
    }

    private static void logLayoutFailure(String reason) {
        if (LAYOUT_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            WaystonesPlayer.LOGGER.warn("Waystones player panel was not added because {}.", reason);
        }
    }

    private record Bounds(int left, int top, int right, int bottom) {
    }

    private static final class WaystonesLayoutState {
        private final int baseImageWidth;
        private final int shiftX;
        private final Map<AbstractWidget, Integer> baseX = new WeakHashMap<>();

        private WaystonesLayoutState(
                WaystoneSelectionScreenBase screen,
                int shiftX,
                List<AbstractWidget> initialWidgets) {
            baseImageWidth = ((AbstractContainerScreenAccessor) screen).waystonesplayer$getImageWidth();
            this.shiftX = shiftX;
            for (AbstractWidget widget : initialWidgets) {
                baseX.put(widget, widget.getX());
            }
        }

        private void synchronize(WaystoneSelectionScreenBase screen) {
            ((AbstractContainerScreenAccessor) screen).waystonesplayer$setImageWidth(baseImageWidth + shiftX * 2);
            Set<AbstractWidget> current = Collections.newSetFromMap(new IdentityHashMap<>());
            for (GuiEventListener listener : screen.children()) {
                if (!(listener instanceof AbstractWidget widget)
                        || (!baseX.containsKey(widget) && !isWaystonesWidget(widget))) {
                    continue;
                }
                int originalX = baseX.computeIfAbsent(widget, ignored -> widget.getX());
                widget.setX(originalX + shiftX);
                current.add(widget);
            }
            baseX.keySet().retainAll(current);
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
        private PlayerDestinationList list;

        private PlayerPanel(List<PlayerInfo> players, int x, int y, int width, int height, boolean avatarOnly) {
            initialPlayers = players;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.avatarOnly = avatarOnly;
        }

        private void attach(Screen screen) {
            labels = new PlayerPanelLabels(x, y, width, height, initialPlayers.size(), avatarOnly);
            ClientBalmCompat.addRenderableWidget(screen, labels);
            list = new PlayerDestinationList(
                    x,
                    y + HEADER_HEIGHT,
                    width,
                    height - HEADER_HEIGHT - FOOTER_HEIGHT,
                    initialPlayers,
                    avatarOnly,
                    id -> ClientBalmCompat.sendToServer(new RequestPlayerTeleportPayload(id)));
            ClientBalmCompat.addRenderableWidget(screen, list);
        }

        private void refresh(List<PlayerInfo> players) {
            list.updatePlayers(players);
            labels.setPlayerCount(players.size());
        }

        private void tick() {
            list.tickVisibleEntries();
        }
    }

    private static final class PlayerPanelLabels extends AbstractWidget {
        private boolean empty;
        private final boolean avatarOnly;
        private int count;
        private final int panelHeight;
        private final Component title = Component.translatable("gui.waystonesplayer.online_players");
        private final Component emptyMessage = Component.translatable("gui.waystonesplayer.no_other_players")
                .copy()
                .withStyle(ChatFormatting.RED);

        private PlayerPanelLabels(int x, int y, int width, int height, int count, boolean avatarOnly) {
            super(x, y, width, HEADER_HEIGHT, narrationMessage(count));
            this.avatarOnly = avatarOnly;
            this.panelHeight = height;
            updateState(count);
        }

        private void setPlayerCount(int count) {
            if (this.count != count) {
                updateState(count);
            }
        }

        private void updateState(int count) {
            this.count = count;
            empty = count == 0;
            setMessage(narrationMessage(count));
            setTooltip(avatarOnly ? Tooltip.create(getMessage()) : null);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            Component heading = avatarOnly ? Component.literal(Integer.toString(count)) : title;
            graphics.drawCenteredString(font, heading, getX() + width / 2, getY() + TITLE_Y, 0xFFFFFFFF);
            if (empty && !avatarOnly) {
                graphics.drawCenteredString(font, emptyMessage, getX() + width / 2,
                        getY() + panelHeight / 2 + EMPTY_STATE_Y_OFFSET, 0xFFFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return false;
        }

        @Override
        public NarratableEntry.NarrationPriority narrationPriority() {
            return isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : NarratableEntry.NarrationPriority.NONE;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }

        private static Component narrationMessage(int count) {
            if (count == 0) {
                return Component.translatable("narration.waystonesplayer.online_players.empty");
            }
            return Component.translatable("gui.waystonesplayer.online_players")
                    .append(": ")
                    .append(Integer.toString(count));
        }
    }
}
