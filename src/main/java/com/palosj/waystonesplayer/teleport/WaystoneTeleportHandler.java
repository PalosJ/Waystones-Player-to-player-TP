package com.palosj.waystonesplayer.teleport;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WaystoneTeleportHandler {

    private static final Map<WaystoneTeleportContext, PendingHungerCost> PENDING_HUNGER_COSTS = new IdentityHashMap<>();

    private WaystoneTeleportHandler() {
    }

    @SubscribeEvent
    public static void onWaystoneTeleportPre(WaystoneTeleportEvent.Pre event) {
        if (!(event.getContext().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack warpItem = event.getContext().getWarpItem();
        if (!WaystonesCompat.isWarpStone(warpItem)) {
            return;
        }

        WaystonesCompat.stopUsingWarpStone(player);

        if (!HungerCostService.isEnabled()) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        Waystone targetWaystone = event.getContext().getTargetWaystone();
        if (targetWaystone == null) {
            return;
        }

        Vec3 targetPos = Vec3.atCenterOf(targetWaystone.getPos());
        int cost = HungerCostService.calculateFoodCost(player.position(), targetPos);
        if (!HungerCostService.canAfford(player, cost)) {
            player.displayClientMessage(translatable("message.waystonesplayer.insufficient_hunger"), false);
            event.setCanceled(true);
            return;
        }

        event.setRequirements(new HungerWarpRequirement(event.getRequirements(), event.getContext(), cost));
    }

    @SubscribeEvent
    public static void onWaystoneTeleportComplete(WaystoneTeleportEvent.Complete event) {
        PendingHungerCost pending = PENDING_HUNGER_COSTS.remove(event.getContext());
        if (pending == null) {
            return;
        }

        if (!(event.getContext().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!pending.playerId().equals(player.getUUID())) {
            return;
        }

        boolean primaryTeleportSucceeded = event.getPrimaryResult()
                .filter(result -> result.isSuccessful() && result.entity().getUUID().equals(player.getUUID()))
                .isPresent();
        if (!primaryTeleportSucceeded) {
            return;
        }

        HungerCostService.consume(player, pending.hungerCost());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            PENDING_HUNGER_COSTS.entrySet().removeIf(entry -> entry.getValue().playerId().equals(playerId));
        }
    }

    static void markPendingHungerCost(WaystoneTeleportContext context, UUID playerId, int hungerCost) {
        PENDING_HUNGER_COSTS.put(context, new PendingHungerCost(playerId, hungerCost));
    }

    static void clearPendingHungerCost(WaystoneTeleportContext context) {
        PENDING_HUNGER_COSTS.remove(context);
    }

    private static Component translatable(String key) {
        return Component.translatable(key).copy().withStyle(ChatFormatting.RED);
    }

    private record PendingHungerCost(UUID playerId, int hungerCost) {
    }
}
