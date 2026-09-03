package com.palosj.waystonesptpt.compat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.teleport.TeleportOutcome;

import net.minecraft.server.level.ServerPlayer;

public final class WaystonesTeleportCompat {
    private static final AtomicBoolean COMPAT_FAILURE_LOGGED = new AtomicBoolean();

    private WaystonesTeleportCompat() {
    }

    public static CompletionStage<Optional<TeleportOutcome>> tryTeleport(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode) {
        try {
            return WaystonesTeleportEvaluator.tryTeleport(sender, target, warpStoneUse, mode)
                    .thenApply(Optional::of)
                    .exceptionally(error -> {
                        logCompatibilityFailure(error);
                        return Optional.empty();
                    });
        } catch (LinkageError | RuntimeException error) {
            logCompatibilityFailure(error);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private static void logCompatibilityFailure(Throwable error) {
        if (COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            WaystonesPTPT.LOGGER.warn(
                    "Waystones player-destination compatibility is unavailable; the teleport was rejected.",
                    error);
        }
    }
}
