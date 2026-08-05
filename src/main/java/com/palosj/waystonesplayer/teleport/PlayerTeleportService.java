package com.palosj.waystonesplayer.teleport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;

public final class PlayerTeleportService {
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Integer> LAST_REQUEST_TICKS = new HashMap<>();

    private PlayerTeleportService() {
    }

    public static void handleRequest(ServerPlayer sender, UUID targetPlayerId) {
        AbstractContainerMenu menu = sender.containerMenu;
        if (!WaystonesCompat.isWarpStoneMenu(menu)) {
            WaystonesPlayer.LOGGER.warn("{} sent an invalid warp stone player teleport request.", sender.getGameProfile().getName());
            sender.displayClientMessage(translatable("message.waystonesplayer.invalid_context"), false);
            return;
        }

        WaystonesCompat.stopUsingWarpStone(sender);
        sender.closeContainer();

        ServerPlayer target = sender.server.getPlayerList().getPlayer(targetPlayerId);
        if (target == null) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_offline"), false);
            return;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_self"), false);
            return;
        }

        int currentTick = sender.server.getTickCount();
        Integer lastRequestTick = LAST_REQUEST_TICKS.get(sender.getUUID());
        if (lastRequestTick != null && currentTick - lastRequestTick < REQUEST_COOLDOWN_TICKS) {
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_cooling_down"), false);
            return;
        }
        Vec3 targetPos = target.position();
        int cost = 0;
        boolean hungerCostEnabled = HungerCostService.isEnabled();

        if (hungerCostEnabled) {
            Vec3 senderPos = sender.position();
            cost = HungerCostService.calculateFoodCost(senderPos, targetPos);

            if (!HungerCostService.canAfford(sender, cost)) {
                sender.displayClientMessage(translatable("message.waystonesplayer.insufficient_hunger"), false);
                return;
            }
        }

        LAST_REQUEST_TICKS.put(sender.getUUID(), currentTick);

        ServerLevel targetLevel = target.serverLevel();
        try {
            sender.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, target.getYRot(), target.getXRot());
            sender.resetFallDistance();
        } catch (RuntimeException e) {
            WaystonesPlayer.LOGGER.error("Failed to teleport {} to {}.", sender.getGameProfile().getName(), target.getGameProfile().getName(), e);
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_failed"), false);
            return;
        }

        if (hungerCostEnabled && cost > 0) {
            HungerCostService.consume(sender, cost);
        }
    }

    public static void clearCooldown(UUID playerId) {
        LAST_REQUEST_TICKS.remove(playerId);
    }

    private static Component translatable(String key) {
        return Component.translatable(key).copy().withStyle(ChatFormatting.RED);
    }
}
