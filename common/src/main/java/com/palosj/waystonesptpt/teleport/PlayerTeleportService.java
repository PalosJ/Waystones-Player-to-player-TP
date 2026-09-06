package com.palosj.waystonesptpt.teleport;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.compat.WaystonesCompat;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerTeleportService {
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final RequestRateLimiter REQUEST_LIMITER = new RequestRateLimiter(REQUEST_COOLDOWN_TICKS);

    private static final Map<UUID, PendingRequest> PENDING = new HashMap<>();

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
            runtime.displayMessage(sender, "message.waystonesptpt.teleport_cooling_down");
            return;
        }
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_SILENT) {
            return;
        }

        PendingRequest previous = PENDING.get(senderId);
        if (previous != null) {
            if (previous.attempt().validatePreparation(runtime.currentTick(sender))
                    || previous.attempt().state() == TeleportAttempt.State.COMMITTING) {
                runtime.displayMessage(sender, "message.waystonesptpt.teleport_pending");
                return;
            }
            PENDING.remove(senderId, previous);
        }

        Optional<WaystonesCompat.WarpStoneUse> warpStoneUse = runtime.resolveWarpStoneUse(sender);
        if (warpStoneUse.isEmpty()) {
            WaystonesPTPT.LOGGER.debug(
                    "{} sent an invalid warp stone player teleport request.",
                    runtime.playerName(sender));
            runtime.displayMessage(sender, "message.waystonesptpt.invalid_context");
            return;
        }

        Optional<TeleportRuntimeBoundary.TargetPlayer> resolvedTarget =
                runtime.resolveOnlineTarget(sender, targetPlayerId);
        if (resolvedTarget.isEmpty()) {
            runtime.displayMessage(sender, "message.waystonesptpt.target_unavailable");
            return;
        }

        TeleportRuntimeBoundary.TargetPlayer target = resolvedTarget.orElseThrow();
        if (senderId.equals(target.id())) {
            runtime.displayMessage(sender, "message.waystonesptpt.target_self");
            return;
        }

        if (!runtime.allowsReceiving(target.player())) {
            runtime.displayMessage(sender, "message.waystonesptpt.target_receiving_disabled");
            return;
        }

        TeleportRuntimeBoundary.PlayerRotation originalRotation = runtime.captureRotation(sender);
        WaystonesCompat.WarpStoneUse boundUse = warpStoneUse.orElseThrow();
        TeleportAttempt attempt = new TeleportAttempt(runtime.currentTick(sender),
                runtime.captureRequestValidity(sender, target.id(), boundUse));
        PendingRequest pending = new PendingRequest(sender, runtime, attempt);
        PENDING.put(senderId, pending);
        try {
            runtime.tryTeleport(sender, target.player(), boundUse, experienceMode, attempt)
                    .whenComplete((result, error) -> runtime.executeOnServerThread(sender, () -> {
                        try {
                            boolean committed = attempt.state() == TeleportAttempt.State.COMMITTING;
                            if (!attempt.settle() || !runtime.isSameSession(sender)) {
                                return;
                            }
                            if (error != null) {
                                WaystonesPTPT.LOGGER.warn("Player teleport preparation failed.", error);
                                runtime.displayMessage(sender, "message.waystonesptpt.compatibility_unavailable");
                                return;
                            }
                            settleTeleport(sender, boundUse, originalRotation, result, committed,
                                    attempt.durabilityCost(), runtime);
                        } finally {
                            PENDING.remove(senderId, pending);
                        }
                    }));
        } catch (RuntimeException | LinkageError error) {
            attempt.cancel();
            PENDING.remove(senderId, pending);
            WaystonesPTPT.LOGGER.warn("Player teleport failed before completion registration.", error);
            runtime.displayMessage(sender, "message.waystonesptpt.compatibility_unavailable");
        }
    }

    private static void settleTeleport(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse successfulUse,
            TeleportRuntimeBoundary.PlayerRotation originalRotation,
            Optional<TeleportOutcome> result,
            boolean committed,
            int damage,
            TeleportRuntimeBoundary runtime) {
        if (result.isEmpty()) {
            runtime.displayMessage(sender, "message.waystonesptpt.compatibility_unavailable");
            return;
        }

        TeleportOutcome outcome = result.orElseThrow();
        if (outcome == TeleportOutcome.UNAFFORDABLE) {
            runtime.displayMessage(sender, "message.waystonesptpt.insufficient_experience");
            return;
        }
        if (outcome == TeleportOutcome.FAILED) {
            runtime.displayMessage(sender, "message.waystonesptpt.teleport_failed");
            return;
        }

        if (!committed) {
            runtime.displayMessage(sender, "message.waystonesptpt.teleport_failed");
            return;
        }
        runtime.restoreRotation(sender, originalRotation);
        runtime.resetFallDistance(sender);
        Optional<TeleportRuntimeBoundary.DurabilityTarget> durabilityTarget =
                runtime.resolveDurabilityTarget(sender, successfulUse);
        if (damage > 0 && durabilityTarget.isPresent()) {
            runtime.damageWarpStone(durabilityTarget.orElseThrow(), sender, successfulUse, damage);
        } else if (damage > 0) {
            WaystonesPTPT.LOGGER.error(
                    "The bound Warp Stone disappeared after a confirmed player teleport; no unrelated item was damaged.");
            runtime.displayMessage(sender, "message.waystonesptpt.post_teleport_item_changed");
        }
        if (outcome == TeleportOutcome.MOVED_INCOMPATIBLY) {
            runtime.displayMessage(sender, "message.waystonesptpt.post_teleport_destination_changed");
        }
        runtime.closeContainer(sender);
    }

    public static void tickPendingRequests() {
        for (var entry : List.copyOf(PENDING.entrySet())) {
            PendingRequest pending = entry.getValue();
            if (pending.attempt().state() == TeleportAttempt.State.PREPARING
                    && !pending.attempt().validatePreparation(pending.runtime().currentTick(pending.sender()))) {
                PENDING.remove(entry.getKey(), pending);
                if (pending.runtime().isSameSession(pending.sender())) {
                    pending.runtime().displayMessage(pending.sender(), "message.waystonesptpt.teleport_cancelled");
                }
            }
        }
    }

    private record PendingRequest(ServerPlayer sender, TeleportRuntimeBoundary runtime, TeleportAttempt attempt) { }

    public static void clearCooldown(UUID playerId) {
        REQUEST_LIMITER.clear(playerId);
        PendingRequest pending = PENDING.remove(playerId);
        if (pending != null) {
            pending.attempt().cancel();
        }
    }
}
