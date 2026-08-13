package com.palosj.waystonesplayer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.widget.PlayerDestinationList;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.mixin.client.AbstractContainerScreenAccessor;
import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;

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
    private static final Map<Screen, PlayerPanel> PANELS = new WeakHashMap<>();
    private static Object cachedConnection;
    private static UUID cachedSelf;
    private static long cachedDirectoryFingerprint;
    private static List<PlayerInfo> cachedOnlinePlayers = List.of();
    private static boolean directoryCacheInitialized;
    private static final int HEADER_HEIGHT = 64;
    private static final int FOOTER_HEIGHT = 25;
    private static final int TITLE_Y = 20;
    private static final int EMPTY_STATE_Y_OFFSET = -20;
    private static final int SCREEN_MARGIN = 4;
    private static final int TARGET_PANEL_HEIGHT = 269;

    private WaystonePlayerScreenInjector() {
    }

    public static void onScreenInit(Screen candidate) {
        PANELS.remove(candidate);
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

            AbstractWaystoneList<?> waystoneList = findWaystoneList(screen);
            if (waystoneList == null) {
                if (LAYOUT_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
                    WaystonesPlayer.LOGGER.warn(
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
                    WaystonesPlayer.LOGGER.warn(
                            "Waystones player panel was not added because the screen is too short for an interactive row.");
                }
                return;
            }
            PlayerPanelLayout layout = PlayerPanelLayout.resolve(screen.width, guiLeft, guiWidth);
            moveWaystonesLayout(screen, guiLeft, guiTop, guiWidth, guiHeight,
                    layout.waystonesX() - guiLeft);

            PlayerPanel panel = new PlayerPanel(onlinePlayers, layout.panelX(), guiTop, layout.panelWidth(), panelHeight,
                    layout.avatarOnly());
            panel.attach(screen);
            PANELS.put(screen, panel);
        } catch (Exception e) {
            WaystonesPlayer.LOGGER.error("WaystonesPlayer GUI injection failed", e);
        }
    }

    public static void onClientTick(Screen candidate) {
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
            var profile = info.getProfile();
            fingerprint = mixFingerprint(fingerprint, profile.getId().getMostSignificantBits());
            fingerprint = mixFingerprint(fingerprint, profile.getId().getLeastSignificantBits());
            fingerprint = mixFingerprint(fingerprint, profile.getName().hashCode());
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
        onlinePlayers.removeIf(info -> info.getProfile().getId().equals(selfId));
        onlinePlayers.sort(Comparator
                .comparing((PlayerInfo info) -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(info -> info.getProfile().getName())
                .thenComparing(info -> info.getProfile().getId()));
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
        layout.waystonesplayer$setLeftPos(layout.waystonesplayer$getLeftPos() + deltaX);

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

        return widget instanceof EditBox
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
        private PlayerPanelLabels labels;
        private PlayerDestinationList playerList;

        private PlayerPanel(List<PlayerInfo> onlinePlayers, int x, int y, int width, int height,
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
            BalmScreenUtils.addRenderableWidget(screen, labels);

            int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
            playerList = new PlayerDestinationList(
                    x, y + HEADER_HEIGHT, width, listHeight, initialPlayers, avatarOnly, targetPlayerId -> {
                Balm.networking().sendToServer(new RequestPlayerTeleportPayload(targetPlayerId));
            });
            BalmScreenUtils.addRenderableWidget(screen, playerList);
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
                return Component.translatable("narration.waystonesplayer.online_players.empty");
            }

            return Component.translatable("gui.waystonesplayer.online_players")
                    .append(": ")
                    .append(Integer.toString(playerCount));
        }
    }
}
