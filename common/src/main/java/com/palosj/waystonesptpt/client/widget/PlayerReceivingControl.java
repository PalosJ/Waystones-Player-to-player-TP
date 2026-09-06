package com.palosj.waystonesptpt.client.widget;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.network.payload.ReceivingDirectoryPayload;
import com.palosj.waystonesptpt.network.payload.UpdateReceivingPayload;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class PlayerReceivingControl extends Button {
    private final boolean avatarOnly;
    private Component description = Component.empty();
    private boolean displayedAllowed;
    private boolean displayedReady;
    private boolean initialized;

    public PlayerReceivingControl(int x, int y, int width, boolean avatarOnly) {
        super(x, y, width, 20, Component.empty(), button -> submit(), DEFAULT_NARRATION);
        this.avatarOnly = avatarOnly;
        ReceivingClientState.begin();
        tick();
    }

    public void updateDirectory(List<UUID> playerIds, boolean first) {
        if (!ReceivingClientState.setDirectory(playerIds) && !first) { return; }
        int batchSize = ReceivingDirectoryPayload.MAX_ENTRIES;
        for (int from = 0; from < Math.max(1, playerIds.size()); from += batchSize) {
            Balm.getNetworking().sendToServer(new ReceivingDirectoryPayload(ReceivingClientState.session(), from == 0,
                    playerIds.subList(from, Math.min(playerIds.size(), from + batchSize))));
        }
    }

    private static void submit() {
        if (!ReceivingClientState.ready()) { return; }
        boolean allowed = !ReceivingClientState.ownAllowed();
        long changeId = ReceivingClientState.beginChange();
        Balm.getNetworking().sendToServer(new UpdateReceivingPayload(ReceivingClientState.session(), changeId, allowed));
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
        setMessage(avatarOnly ? Component.literal(!ready ? "…" : allowed ? "+" : "−")
                : Component.translatable(!ready ? "gui.waystonesptpt.receiving_wait"
                        : allowed ? "gui.waystonesptpt.receiving_on" : "gui.waystonesptpt.receiving_off"));
        setTooltip(Tooltip.create(description));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, description);
        if (active) { output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused")); }
    }
}
