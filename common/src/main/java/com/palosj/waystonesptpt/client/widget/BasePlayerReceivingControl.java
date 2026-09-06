package com.palosj.waystonesptpt.client.widget;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.network.payload.ReceivingDirectoryPayload;
import com.palosj.waystonesptpt.network.payload.UpdateReceivingPayload;
import com.palosj.waystonesptpt.network.ModNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class BasePlayerReceivingControl extends Button {
    private Component description = Component.empty();
    private boolean displayedAllowed;
    private boolean displayedReady;
    private boolean initialized;

    protected BasePlayerReceivingControl(int x, int y) {
        super(x, y, PlayerToolbarLayout.BUTTON_SIZE, PlayerToolbarLayout.BUTTON_SIZE,
                Component.empty(), button -> submit(), DEFAULT_NARRATION);
        ReceivingClientState.begin();
        tick();
    }

    public void updateDirectory(List<UUID> playerIds, boolean first) {
        if (!ReceivingClientState.setDirectory(playerIds) && !first) { return; }
        int batchSize = ReceivingDirectoryPayload.MAX_ENTRIES;
        for (int from = 0; from < Math.max(1, playerIds.size()); from += batchSize) {
            ModNetworking.sendToServer(new ReceivingDirectoryPayload(ReceivingClientState.session(), from == 0,
                    playerIds.subList(from, Math.min(playerIds.size(), from + batchSize))));
        }
    }

    private static void submit() {
        if (!ReceivingClientState.ready()) { return; }
        boolean allowed = !ReceivingClientState.ownAllowed();
        long changeId = ReceivingClientState.beginChange();
        ModNetworking.sendToServer(new UpdateReceivingPayload(ReceivingClientState.session(), changeId, allowed));
    }

    public void tick() {
        boolean allowed = ReceivingClientState.ownAllowed();
        boolean ready = ReceivingClientState.ready();
        active = ready;
        if (initialized && displayedAllowed == allowed && displayedReady == ready) { return; }
        initialized = true;
        displayedAllowed = allowed;
        displayedReady = ready;
        description = Component.translatable(!ready ? "gui.waystonesptpt.receiving_pending"
                : allowed ? "gui.waystonesptpt.receiving_allowed" : "gui.waystonesptpt.receiving_disabled");
        setTooltip(Tooltip.create(description));
    }

    protected boolean receivingAllowed() {
        return displayedAllowed;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, description);
        if (active) { output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused")); }
    }
}
