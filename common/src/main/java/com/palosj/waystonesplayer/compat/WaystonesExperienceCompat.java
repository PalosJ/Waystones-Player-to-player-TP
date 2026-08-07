package com.palosj.waystonesplayer.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.teleport.TeleportCost;

import net.minecraft.server.level.ServerPlayer;

public final class WaystonesExperienceCompat {
    private static final String EVALUATOR_CLASS_NAME =
            "com.palosj.waystonesplayer.compat.WaystonesExperienceEvaluator";
    private static final AtomicBoolean COMPAT_FAILURE_LOGGED = new AtomicBoolean();

    private WaystonesExperienceCompat() {
    }

    public static Optional<TeleportCost> resolveExperienceCost(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode) {
        try {
            Object result = EvaluatorMethodHolder.RESOLVE_METHOD.invoke(null, sender, target, warpStoneUse, mode);
            if (result instanceof TeleportCost cost) {
                return Optional.of(cost);
            }
            return compatibilityFailure(new IllegalStateException("Waystones experience evaluator returned no cost."));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error && !(cause instanceof LinkageError)) {
                throw error;
            }
            return compatibilityFailure(cause);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return compatibilityFailure(e);
        }
    }

    private static Optional<TeleportCost> compatibilityFailure(Throwable error) {
        if (COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            WaystonesPlayer.LOGGER.warn(
                    "Waystones experience-cost compatibility is unavailable; player teleports that require experience will be rejected.",
                    error);
        }
        return Optional.empty();
    }

    private static Method findEvaluatorMethod() {
        try {
            Class<?> evaluatorClass = Class.forName(
                    EVALUATOR_CLASS_NAME,
                    true,
                    WaystonesExperienceCompat.class.getClassLoader());
            return evaluatorClass.getDeclaredMethod(
                    "resolveExperienceCost",
                    ServerPlayer.class,
                    ServerPlayer.class,
                    WaystonesCompat.WarpStoneUse.class,
                    PlayerTeleportExperienceMode.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not resolve the Waystones experience evaluator.", e);
        }
    }

    private static final class EvaluatorMethodHolder {
        private static final Method RESOLVE_METHOD = findEvaluatorMethod();
    }
}
