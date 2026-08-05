package com.palosj.waystonesplayer.teleport;

import java.util.List;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

final class HungerWarpRequirement implements WarpRequirement {
    private final WarpRequirement delegate;
    private final WaystoneTeleportContext context;
    private final int hungerCost;

    HungerWarpRequirement(WarpRequirement delegate, WaystoneTeleportContext context, int hungerCost) {
        this.delegate = delegate;
        this.context = context;
        this.hungerCost = hungerCost;
    }

    @Override
    public boolean canAfford(Player player) {
        return delegate.canAfford(player) && HungerCostService.canAfford(player, hungerCost);
    }

    @Override
    public void consume(Player player) {
        delegate.consume(player);
        WaystoneTeleportHandler.markPendingHungerCost(context, player.getUUID(), hungerCost);
    }

    @Override
    public void rollback(Player player) {
        delegate.rollback(player);
        WaystoneTeleportHandler.clearPendingHungerCost(context);
    }

    @Override
    public void appendHoverText(Player player, List<Component> tooltip) {
        delegate.appendHoverText(player, tooltip);
    }
}
