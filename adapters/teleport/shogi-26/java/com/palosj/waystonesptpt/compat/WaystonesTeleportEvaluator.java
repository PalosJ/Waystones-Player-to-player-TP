package com.palosj.waystonesptpt.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Either;
import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.teleport.TeleportArrivalVerifier;
import com.palosj.waystonesptpt.teleport.TeleportOutcome;
import com.palosj.waystonesptpt.teleport.TeleportAttempt;
import com.palosj.waystonesptpt.teleport.TeleportRuntime;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class WaystonesTeleportEvaluator {
    private static final Method ASYNC_TELEPORT = findAsyncTeleport();

    private WaystonesTeleportEvaluator() {
    }

    static CompletionStage<TeleportOutcome> tryTeleport(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode, TeleportAttempt attempt) {
        return tryTeleport(sender, target, warpStoneUse, mode, attempt, 0);
    }

    private static CompletionStage<TeleportOutcome> tryTeleport(
            ServerPlayer sender, ServerPlayer target, WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode, TeleportAttempt attempt, int retries) {
        PlayerDestinationWaystone targetWaystone = new PlayerDestinationWaystone(target);
        TransactionState transaction = new TransactionState(sender);
        LockedWaystoneTeleportContext[] holder = new LockedWaystoneTeleportContext[1];

        try {
            WaystoneTeleportContext baseContext = WaystonesAPI
                    .createUnboundTeleportContext(sender, targetWaystone)
                    .setWarpItem(warpStoneUse.stack())
                    .setWarpHand(warpStoneUse.hand())
                    .setPlaysSound(true)
                    .setPlaysEffect(true)
                    .addFlag(WaystonesPTPT.id("player_destination"));
            LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(
                    baseContext,
                    () -> beforeExecute(sender, warpStoneUse, targetWaystone, holder[0], transaction, attempt));
            holder[0] = context;

            ShogiExperienceRuleSafety.RuleCache rules = new ShogiExperienceRuleSafety.RuleCache(
                    net.minecraft.resources.RegistryOps.create(
                            com.mojang.serialization.JsonOps.INSTANCE, sender.registryAccess()));
            context.lock(locked -> evaluateRequirements(
                    sender,
                    warpStoneUse,
                    targetWaystone,
                    locked,
                    rules.get(WaystonesConfig.getActive().rules, mode), attempt));

            Either<List<Object>, List<Object>> initialRequirements = context.getRequirements();
            if (initialRequirements.right().isPresent()) {
                return CompletableFuture.completedFuture(TeleportOutcome.UNAFFORDABLE);
            }

            return invokeTeleport(context)
                    .handle((result, error) -> new ApiCompletion(result, unwrap(error)))
                    .thenCompose(completion -> onServerThread(
                            sender,
                            () -> finishTeleport(
                                    sender,
                                    context,
                                    transaction,
                                    completion)))
                    .thenCompose(outcome -> {
                        if (outcome == TeleportOutcome.FAILED && retries < 200
                                && attempt.validatePreparation(sender.level().getServer().getTickCount())
                                && targetWaystone.hasMoved()) {
                            ServerPlayer currentTarget = targetWaystone.onlineTarget().orElse(null);
                            if (currentTarget != null) {
                                return tryTeleport(sender, currentTarget, warpStoneUse, mode, attempt, retries + 1);
                            }
                        }
                        return CompletableFuture.completedFuture(outcome);
                    });
        } catch (TeleportRejectedException | IllegalArgumentException error) {
            WaystonesPTPT.LOGGER.debug("Rejected player-destination teleport: {}", error.getMessage());
            transaction.restore();
            return CompletableFuture.completedFuture(TeleportOutcome.FAILED);
        } catch (RuntimeException | LinkageError error) {
            transaction.restore();
            return CompletableFuture.failedFuture(error);
        }
    }

    private static Either<List<Object>, List<Object>> evaluateRequirements(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerDestinationWaystone targetWaystone,
            LockedWaystoneTeleportContext context,
            ShogiExperienceRuleSafety.CompiledRules compiledRules, TeleportAttempt attempt) {
        if (!attempt.validatePreparation(sender.level().getServer().getTickCount())) {
            return Either.right(List.of("Player teleport request is no longer valid"));
        }
        validateBeforeConsumption(sender, warpStoneUse, targetWaystone, context);
        boolean creative = sender.getAbilities().instabuild;
        Either<List<Object>, List<Object>> result = compiledRules.evaluate(context, creative);
        attempt.setDurabilityCost(creative ? 0 : compiledRules.damage().value());
        return result;
    }

    private static void beforeExecute(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerDestinationWaystone targetWaystone,
            LockedWaystoneTeleportContext context,
            TransactionState transaction, TeleportAttempt attempt) {
        validateBeforeConsumption(sender, warpStoneUse, targetWaystone, context);
        attempt.beginCommit(sender.level().getServer().getTickCount());
        transaction.capture(targetOf(targetWaystone));
    }

    private static void validateBeforeConsumption(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerDestinationWaystone targetWaystone,
            LockedWaystoneTeleportContext context) {
        context.requireUnmodified();
        if (!TeleportRuntime.isWarpStoneUseBound(sender, warpStoneUse)) {
            throw new TeleportRejectedException("the bound Warp Stone changed before XP consumption");
        }
        if (targetWaystone.liveTarget().isEmpty()) {
            throw new TeleportRejectedException("the selected player moved, disconnected, or changed dimension");
        }
    }

    private static CompletionStage<Either<?, ?>> invokeTeleport(WaystoneTeleportContext context) {
        if (ASYNC_TELEPORT == null) {
            @SuppressWarnings("deprecation")
            Either<?, ?> result = WaystonesAPI.tryTeleport(context);
            return CompletableFuture.completedFuture(result);
        }
        try {
            Object result = ASYNC_TELEPORT.invoke(null, context);
            if (!(result instanceof CompletionStage<?> stage)) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Waystones tryTeleportAsync returned an unknown result shape"));
            }
            return stage.thenApply(value -> {
                if (!(value instanceof Either<?, ?> either)) {
                    throw new IllegalStateException("Waystones tryTeleportAsync completed with an unknown result shape");
                }
                return either;
            });
        } catch (IllegalAccessException error) {
            return CompletableFuture.failedFuture(error);
        } catch (InvocationTargetException error) {
            return CompletableFuture.failedFuture(error.getCause());
        }
    }

    private static TeleportOutcome finishTeleport(
            ServerPlayer sender,
            LockedWaystoneTeleportContext context,
            TransactionState transaction,
            ApiCompletion completion) {
        TeleportArrivalVerifier.Position before = transaction.beforeExecution;
        boolean moved = before != null && TeleportArrivalVerifier.hasMoved(before, positionOf(sender));
        if (completion.error() != null) {
            if (moved) {
                WaystonesPTPT.LOGGER.warn(
                        "Waystones failed after moving the player; preserving XP and reporting an incompatible movement.",
                        completion.error());
                return finalMovementOutcome(sender, context, transaction);
            }
            transaction.restore();
            if (completion.error() instanceof TeleportRejectedException) {
                return TeleportOutcome.FAILED;
            }
            throw new CompletionException(completion.error());
        }

        Either<?, ?> apiResult = completion.result();
        boolean apiReportedSender = apiResult != null
                && apiResult.left().map(value -> value instanceof List<?> entities
                        && entities.stream().anyMatch(entity -> entity == sender)).orElse(false);
        if (before == null || !TeleportArrivalVerifier.succeeded(apiReportedSender, before, positionOf(sender))) {
            transaction.restore();
            return TeleportOutcome.FAILED;
        }
        return finalMovementOutcome(sender, context, transaction);
    }

    private static TeleportOutcome finalMovementOutcome(
            ServerPlayer sender,
            LockedWaystoneTeleportContext context,
            TransactionState transaction) {
        boolean targetMatches = false;
        try {
            TeleportArrivalVerifier.Target validatedTarget = transaction.validatedTarget();
            targetMatches = validatedTarget != null
                    && TeleportArrivalVerifier.matchesTargetOrHorizontalAdjacent(
                            positionOf(sender),
                            validatedTarget);
        } catch (RuntimeException | LinkageError error) {
            WaystonesPTPT.LOGGER.error(
                    "Could not validate a confirmed player movement; consumed XP will be preserved.",
                    error);
        }
        if (targetMatches && !context.wasModified()) {
            return TeleportOutcome.SUCCESS;
        }
        WaystonesPTPT.LOGGER.error(
                "Waystones moved a player outside the locked destination contract "
                        + "(targetMatches={}, contextModified={}).",
                targetMatches,
                context.wasModified());
        return TeleportOutcome.MOVED_INCOMPATIBLY;
    }

    private static CompletableFuture<TeleportOutcome> onServerThread(
            ServerPlayer sender,
            Supplier<TeleportOutcome> action) {
        MinecraftServer server = sender.level().getServer();
        if (server == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Player teleport completed without a server"));
        }
        if (server.isSameThread()) {
            try {
                return CompletableFuture.completedFuture(action.get());
            } catch (Throwable error) {
                return CompletableFuture.failedFuture(error);
            }
        }
        CompletableFuture<TeleportOutcome> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(action.get());
            } catch (Throwable error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    private static TeleportArrivalVerifier.Position positionOf(ServerPlayer player) {
        return new TeleportArrivalVerifier.Position(
                player.level().dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ());
    }

    private static TeleportArrivalVerifier.Target targetOf(PlayerDestinationWaystone target) {
        BlockPos position = target.getPos();
        return new TeleportArrivalVerifier.Target(
                target.getDimension().identifier().toString(),
                position.getX(),
                position.getY(),
                position.getZ());
    }

    private static Method findAsyncTeleport() {
        try {
            return WaystonesAPI.class.getMethod("tryTeleportAsync", WaystoneTeleportContext.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException || current instanceof InvocationTargetException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private record ApiCompletion(Either<?, ?> result, Throwable error) {
    }

    private static final class TransactionState {
        private final ServerPlayer player;
        private ExperienceSnapshot experienceSnapshot;
        private TeleportArrivalVerifier.Position beforeExecution;
        private TeleportArrivalVerifier.Target validatedTarget;

        private TransactionState(ServerPlayer player) {
            this.player = player;
        }

        private void capture(TeleportArrivalVerifier.Target target) {
            if (experienceSnapshot != null) {
                throw new TeleportRejectedException("Waystones attempted to consume requirements more than once");
            }
            beforeExecution = positionOf(player);
            experienceSnapshot = ExperienceSnapshot.capture(player);
            validatedTarget = target;
        }

        private TeleportArrivalVerifier.Target validatedTarget() {
            return validatedTarget;
        }

        private void restore() {
            if (experienceSnapshot != null && !TeleportArrivalVerifier.hasMoved(beforeExecution, positionOf(player))) {
                experienceSnapshot.restore();
                experienceSnapshot = null;
            }
        }
    }

    private record ExperienceSnapshot(
            ServerPlayer player,
            float progress,
            int level,
            int total) {
        private static ExperienceSnapshot capture(ServerPlayer player) {
            return new ExperienceSnapshot(
                    player,
                    player.experienceProgress,
                    player.experienceLevel,
                    player.totalExperience);
        }

        private void restore() {
            player.experienceProgress = progress;
            player.experienceLevel = level;
            player.totalExperience = total;
            player.connection.send(new ClientboundSetExperiencePacket(progress, total, level));
        }
    }
}
