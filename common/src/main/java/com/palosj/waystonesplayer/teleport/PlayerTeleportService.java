package com.palosj.waystonesplayer.teleport;

import java.util.Optional;
import java.util.UUID;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerTeleportService {
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final RequestRateLimiter REQUEST_LIMITER = new RequestRateLimiter(REQUEST_COOLDOWN_TICKS);

    private PlayerTeleportService() {
    }

    public static void handleRequest(
            ServerPlayer sender,
            UUID targetPlayerId,
            PlayerTeleportExperienceMode experienceMode) {
        handleRequest(sender, targetPlayerId, experienceMode, TeleportRuntime.live(), REQUEST_LIMITER);
    }

    static void handleRequest(
            ServerPlayer sender,
            UUID targetPlayerId,
            PlayerTeleportExperienceMode experienceMode,
            TeleportRuntimeBoundary runtime,
            RequestRateLimiter requestLimiter) {
        UUID senderId = runtime.playerId(sender);
        RequestRateLimiter.Result rateLimitResult = requestLimiter.acquire(senderId, runtime.currentTick(sender));
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_NOTIFY) {
            runtime.displayMessage(sender, "message.waystonesplayer.teleport_cooling_down");
            return;
        }
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_SILENT) {
            return;
        }

        Optional<WaystonesCompat.WarpStoneUse> warpStoneUse = runtime.resolveWarpStoneUse(sender);
        if (warpStoneUse.isEmpty()) {
            WaystonesPlayer.LOGGER.debug(
                    "{} sent an invalid warp stone player teleport request.",
                    runtime.playerName(sender));
            runtime.displayMessage(sender, "message.waystonesplayer.invalid_context");
            return;
        }

        Optional<TeleportRuntimeBoundary.TargetPlayer> resolvedTarget =
                runtime.resolveListedTarget(sender, targetPlayerId);
        if (resolvedTarget.isEmpty()) {
            runtime.displayMessage(sender, "message.waystonesplayer.target_unavailable");
            return;
        }

        TeleportRuntimeBoundary.TargetPlayer target = resolvedTarget.orElseThrow();
        if (senderId.equals(target.id())) {
            runtime.displayMessage(sender, "message.waystonesplayer.target_self");
            return;
        }

        Optional<TeleportOutcome> result = runtime.tryTeleport(
                sender,
                target.player(),
                warpStoneUse.orElseThrow(),
                experienceMode);
        if (result.isEmpty()) {
            runtime.displayMessage(sender, "message.waystonesplayer.compatibility_unavailable");
            return;
        }

        TeleportOutcome outcome = result.orElseThrow();
        if (outcome == TeleportOutcome.UNAFFORDABLE) {
            runtime.displayMessage(sender, "message.waystonesplayer.insufficient_experience");
            return;
        }
        if (outcome == TeleportOutcome.FAILED) {
            runtime.displayMessage(sender, "message.waystonesplayer.teleport_failed");
            return;
        }

        runtime.resetFallDistance(sender);
        WaystonesCompat.WarpStoneUse successfulUse = warpStoneUse.orElseThrow();
        Optional<TeleportRuntimeBoundary.DurabilityTarget> durabilityTarget =
                runtime.resolveDurabilityTarget(sender, successfulUse);
        if (durabilityTarget.isPresent()) {
            runtime.damageWarpStone(durabilityTarget.orElseThrow(), sender, successfulUse);
        } else {
            WaystonesPlayer.LOGGER.error(
                    "The bound Warp Stone disappeared after a confirmed player teleport; no unrelated item was damaged.");
            runtime.displayMessage(sender, "message.waystonesplayer.post_teleport_item_changed");
        }
        if (outcome == TeleportOutcome.MOVED_INCOMPATIBLY) {
            runtime.displayMessage(sender, "message.waystonesplayer.post_teleport_destination_changed");
        }
        runtime.closeContainer(sender);
    }

    public static void clearCooldown(UUID playerId) {
        REQUEST_LIMITER.clear(playerId);
    }
}
