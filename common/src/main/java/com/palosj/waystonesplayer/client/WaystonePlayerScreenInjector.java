package com.palosj.waystonesplayer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.client.widget.PlayerDestinationList;
import com.palosj.waystonesplayer.client.widget.PlayerListToggleButton;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.network.payload.RequestPlayerTeleportPayload;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.client.gui.screens.BalmScreenUtils;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WaystonePlayerScreenInjector {
    private static final AtomicBoolean LAYOUT_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_GAP = 2;
    private static final int WAYSTONES_SIDE_BUTTON_LEFT_OFFSET = 8;
    private static final int HEADER_HEIGHT = 64;
    private static final int FOOTER_HEIGHT = 25;
    private static final int TITLE_Y = 20;
    private static final int EMPTY_STATE_Y_OFFSET = -20;
    private static final int SCREEN_MARGIN = 4;
    private static final int TOGGLE_SIZE = 20;
    private static final int TARGET_PANEL_HEIGHT = 269;

    private WaystonePlayerScreenInjector() {
    }

    public static void onScreenInit(Screen candidate) {
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
            int panelHeight = Math.min(Math.max(guiHeight, TARGET_PANEL_HEIGHT),
                    screen.height - guiTop - SCREEN_MARGIN);
            int panelX = guiLeft - PANEL_WIDTH - WAYSTONES_SIDE_BUTTON_LEFT_OFFSET - PANEL_GAP;

            if (panelX >= SCREEN_MARGIN) {
                new PlayerPanel(onlinePlayers, panelX, guiTop, PANEL_WIDTH, panelHeight).attach(screen);
            } else {
                new NarrowPlayerOverlay(screen, onlinePlayers, guiLeft, guiTop, guiWidth, panelHeight).attach();
            }
        } catch (Exception e) {
            WaystonesPlayer.LOGGER.error("WaystonesPlayer GUI injection failed", e);
        }
    }

    private static List<PlayerInfo> getOnlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) {
            return null;
        }

        List<PlayerInfo> onlinePlayers = new ArrayList<>(minecraft.getConnection().getListedOnlinePlayers());
        onlinePlayers.removeIf(info -> info.getProfile().getId().equals(minecraft.player.getUUID()));
        onlinePlayers.sort(Comparator.comparing(info -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        return onlinePlayers;
    }

    private static AbstractWaystoneList<?> findWaystoneList(WaystoneSelectionScreenBase screen) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWaystoneList<?> waystoneList) {
                return waystoneList;
            }
        }
        return null;
    }

    private static final class PlayerPanel {
        private final List<PlayerInfo> onlinePlayers;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private PlayerPanelLabels labels;
        private PlayerDestinationList playerList;

        private PlayerPanel(List<PlayerInfo> onlinePlayers, int x, int y, int width, int height) {
            this.onlinePlayers = onlinePlayers;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void attach(Screen screen) {
            labels = new PlayerPanelLabels(x, y, width, height, onlinePlayers.isEmpty());
            BalmScreenUtils.addRenderableWidget(screen, labels);

            int listHeight = Math.max(PlayerDestinationList.ENTRY_HEIGHT, height - HEADER_HEIGHT - FOOTER_HEIGHT);
            playerList = new PlayerDestinationList(x, y + HEADER_HEIGHT, width, listHeight, onlinePlayers, targetPlayerId -> {
                Balm.networking().sendToServer(new RequestPlayerTeleportPayload(targetPlayerId));
            });
            BalmScreenUtils.addRenderableWidget(screen, playerList);
        }

        private void setVisible(boolean visible) {
            labels.visible = visible;
            playerList.setPanelVisible(visible);
        }
    }

    private static final class PlayerPanelLabels extends AbstractWidget {
        private final boolean empty;
        private final Component title = Component.translatable("gui.waystonesplayer.online_players");
        private final Component emptyMessage = Component.translatable("gui.waystonesplayer.no_other_players")
                .copy()
                .withStyle(ChatFormatting.RED);

        private PlayerPanelLabels(int x, int y, int width, int height, boolean empty) {
            super(x, y, width, height, Component.empty());
            this.empty = empty;
            active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            guiGraphics.drawCenteredString(font, title, getX() + width / 2, getY() + TITLE_Y, 0xFFFFFFFF);
            if (empty) {
                guiGraphics.drawCenteredString(font, emptyMessage, getX() + width / 2,
                        getY() + height / 2 + EMPTY_STATE_Y_OFFSET, 0xFFFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private static final class NarrowPlayerOverlay {
        private final WaystoneSelectionScreenBase screen;
        private final List<PlayerInfo> onlinePlayers;
        private final int guiLeft;
        private final int guiTop;
        private final int guiWidth;
        private final int panelHeight;
        private final List<WidgetState> originalWidgetStates = new ArrayList<>();
        private GuiEventListener previousFocus;
        private OverlayBackdrop backdrop;
        private PlayerPanel panel;
        private PlayerListToggleButton toggleButton;
        private boolean open;

        private NarrowPlayerOverlay(WaystoneSelectionScreenBase screen, List<PlayerInfo> onlinePlayers,
                                    int guiLeft, int guiTop, int guiWidth, int panelHeight) {
            this.screen = screen;
            this.onlinePlayers = onlinePlayers;
            this.guiLeft = guiLeft;
            this.guiTop = guiTop;
            this.guiWidth = guiWidth;
            this.panelHeight = panelHeight;
        }

        private void attach() {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof AbstractWidget widget) {
                    originalWidgetStates.add(new WidgetState(widget, widget.visible, widget.active));
                }
            }

            backdrop = new OverlayBackdrop(guiLeft, guiTop, guiWidth, panelHeight);
            BalmScreenUtils.addRenderableWidget(screen, backdrop);

            int overlayWidth = PANEL_WIDTH;
            int panelX = guiLeft + (guiWidth - overlayWidth) / 2;
            panel = new PlayerPanel(onlinePlayers, panelX, guiTop, overlayWidth, panelHeight);
            panel.attach(screen);
            panel.setVisible(false);

            int toggleX = Math.min(guiLeft + guiWidth + 2, screen.width - TOGGLE_SIZE - 2);
            toggleButton = new PlayerListToggleButton(toggleX, guiTop, ignored -> toggle());
            BalmScreenUtils.addRenderableWidget(screen, toggleButton);
        }

        private void toggle() {
            open = !open;
            if (open) {
                previousFocus = screen.getFocused();
                for (WidgetState state : originalWidgetStates) {
                    state.widget().visible = false;
                    state.widget().active = false;
                }
                backdrop.visible = true;
                panel.setVisible(true);
                screen.setFocused(toggleButton);
            } else {
                for (WidgetState state : originalWidgetStates) {
                    state.widget().visible = state.visible();
                    state.widget().active = state.active();
                }
                backdrop.visible = false;
                panel.setVisible(false);
                screen.setFocused(previousFocus);
            }
            toggleButton.setOpen(open);
        }
    }

    private static final class OverlayBackdrop extends AbstractWidget {
        private OverlayBackdrop(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            visible = false;
            active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getRight(), getBottom(), 0xE0181818);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private record WidgetState(AbstractWidget widget, boolean visible, boolean active) {
    }
}
