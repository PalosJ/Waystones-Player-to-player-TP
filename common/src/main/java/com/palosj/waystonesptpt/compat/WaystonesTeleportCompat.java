package com.palosj.waystonesptpt.compat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.teleport.TeleportOutcome;
import com.palosj.waystonesptpt.teleport.TeleportAttempt;

import net.minecraft.server.level.ServerPlayer;

public final class WaystonesTeleportCompat {
    private static final AtomicBoolean COMPAT_FAILURE_LOGGED = new AtomicBoolean();

    private WaystonesTeleportCompat() {
    }

    public static Optional<TeleportOutcome> tryTeleport(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode,
            TeleportAttempt attempt) {
        try {
            return Optional.of(WaystonesTeleportEvaluator.tryTeleport(sender, target, warpStoneUse, mode, attempt));
        } catch (LinkageError | RuntimeException error) {
            if (COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
                WaystonesPTPT.LOGGER.warn(
                        "Waystones player-destination compatibility is unavailable; the teleport was rejected.",
                        error);
            }
            return Optional.empty();
        }
    }
}
