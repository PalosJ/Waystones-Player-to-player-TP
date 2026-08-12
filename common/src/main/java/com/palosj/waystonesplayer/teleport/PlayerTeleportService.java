package com.palosj.waystonesplayer.teleport;

import java.util.Optional;
import java.util.UUID;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.compat.WaystonesTeleportCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class PlayerTeleportService {
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final RequestRateLimiter REQUEST_LIMITER = new RequestRateLimiter(REQUEST_COOLDOWN_TICKS);

    private PlayerTeleportService() {
    }

    public static void handleRequest(
            ServerPlayer sender,
            UUID targetPlayerId,
            PlayerTeleportExperienceMode experienceMode) {
        MinecraftServer server = sender.level().getServer();
        int currentTick = server.getTickCount();
        RequestRateLimiter.Result rateLimitResult = REQUEST_LIMITER.acquire(sender.getUUID(), currentTick);
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_NOTIFY) {
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_cooling_down"), false);
            return;
        }
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_SILENT) {
            return;
        }

        AbstractContainerMenu menu = sender.containerMenu;
        Optional<WaystonesCompat.WarpStoneUse> warpStoneUse = WaystonesCompat.resolveWarpStoneUse(sender, menu);
        if (warpStoneUse.isEmpty()) {
            WaystonesPlayer.LOGGER.debug("{} sent an invalid warp stone player teleport request.", sender.getScoreboardName());
            sender.displayClientMessage(translatable("message.waystonesplayer.invalid_context"), false);
            return;
        }

        ServerPlayer target = server.getPlayerList().getPlayer(targetPlayerId);
        if (target == null || !target.allowsListing()) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_unavailable"), false);
            return;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_self"), false);
            return;
        }

        Optional<TeleportOutcome> result = WaystonesTeleportCompat.tryTeleport(
                sender,
                target,
                warpStoneUse.orElseThrow(),
                experienceMode);
        if (result.isEmpty()) {
            sender.displayClientMessage(translatable("message.waystonesplayer.compatibility_unavailable"), false);
            return;
        }

        if (result.orElseThrow() == TeleportOutcome.UNAFFORDABLE) {
            sender.displayClientMessage(translatable("message.waystonesplayer.insufficient_experience"), false);
            return;
        }
        if (result.orElseThrow() == TeleportOutcome.FAILED) {
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_failed"), false);
            return;
        }

        sender.resetFallDistance();
        WaystonesCompat.WarpStoneUse successfulUse = warpStoneUse.orElseThrow();
        DurabilityCompat.hurtAndBreak(successfulUse.stack(), sender, successfulUse.hand());
        sender.closeContainer();
    }

    public static void clearCooldown(UUID playerId) {
        REQUEST_LIMITER.clear(playerId);
    }

    private static Component translatable(String key) {
        return Component.translatable(key).copy().withStyle(ChatFormatting.RED);
    }
}
