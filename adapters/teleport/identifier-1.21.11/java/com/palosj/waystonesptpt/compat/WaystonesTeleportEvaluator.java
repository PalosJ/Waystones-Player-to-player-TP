package com.palosj.waystonesptpt.compat;

import java.util.List;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.WaystonesPTPT;
import com.palosj.waystonesptpt.teleport.TeleportArrivalVerifier;
import com.palosj.waystonesptpt.teleport.TeleportOutcome;
import com.palosj.waystonesptpt.teleport.TeleportAttempt;
import com.palosj.waystonesptpt.teleport.TeleportRuntime;

import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.requirement.RequirementFunction;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.blay09.mods.waystones.requirement.ConfiguredRequirementModifier;
import net.blay09.mods.waystones.requirement.NoRequirement;
import net.blay09.mods.waystones.requirement.WarpRequirementsContextImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

final class WaystonesTeleportEvaluator {
    private WaystonesTeleportEvaluator() {
    }

    static TeleportOutcome tryTeleport(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode,
            TeleportAttempt attempt) {
        BlockPos requestedTarget = target.blockPosition();
        WaystoneImpl targetWaystone = new WaystoneImpl(
                WaystoneTypes.WAYSTONE,
                target.getUUID(),
                target.level().dimension(),
                requestedTarget,
                WaystoneOrigin.PLAYER,
                target.getUUID());
        targetWaystone.setName(Component.literal(target.getScoreboardName()));
        targetWaystone.setTransient(true);

        WaystoneTeleportContext baseContext = WaystonesAPI.createUnboundTeleportContext(sender, targetWaystone)
                .setWarpItem(warpStoneUse.stack())
                .setWarpHand(warpStoneUse.hand())
                .setPlaysSound(true)
                .setPlaysEffect(true)
                .setAppliesModifiers(false)
                .addFlag(WaystonesPTPT.id("player_destination"));

        WarpRequirement requirement = resolveExperienceRequirement(baseContext, mode);
        if (!sender.getAbilities().instabuild && !requirement.canAfford(sender)) {
            return TeleportOutcome.UNAFFORDABLE;
        }

        LockedWaystoneTeleportContext teleportContext = new LockedWaystoneTeleportContext(baseContext);
        GuardedRollbackRequirement guardedRequirement = new GuardedRollbackRequirement(
                sender, target, targetWaystone, mode, warpStoneUse, teleportContext, attempt);
        teleportContext.lockRequirements(guardedRequirement);

        boolean apiReportedSender;
        try {
            var result = tryTeleportSynchronously(teleportContext);
            apiReportedSender = result.left()
                    .map(entities -> entities.stream().anyMatch(entity -> entity == sender))
                    .orElse(false);
        } catch (TeleportRejectedException error) {
            guardedRequirement.restore();
            WaystonesPTPT.LOGGER.debug("Rejected a guarded player-destination teleport: {}", error.getMessage());
            return TeleportOutcome.FAILED;
        } catch (RuntimeException | LinkageError error) {
            if (guardedRequirement.hasMovedSinceCommit()) {
                WaystonesPTPT.LOGGER.warn(
                        "Waystones reported an exception after the player moved; treating the confirmed movement as successful.",
                        error);
                return finalMovementOutcome(sender, guardedRequirement, teleportContext);
            }
            guardedRequirement.restore();
            throw error;
        }

        boolean confirmedMovement = guardedRequirement.beforeExecution() != null
                && TeleportArrivalVerifier.succeeded(
                apiReportedSender,
                guardedRequirement.beforeExecution(),
                positionOf(sender));
        if (!confirmedMovement) {
            guardedRequirement.restore();
            return TeleportOutcome.FAILED;
        }
        return finalMovementOutcome(sender, guardedRequirement, teleportContext);
    }

    private static WarpRequirement resolveExperienceRequirement(
            WaystoneTeleportContext teleportContext,
            PlayerTeleportExperienceMode mode) {
        var teleportsConfig = WaystonesConfig.getActive().teleports;
        if (!mode.shouldEvaluateWaystonesExperience(teleportsConfig.enableCosts)) {
            return NoRequirement.INSTANCE;
        }

        WarpRequirementsContextImpl requirementsContext = new WarpRequirementsContextImpl(teleportContext);
        for (String configuredRule : teleportsConfig.warpRequirements) {
            if (configuredRule.isBlank()) {
                continue;
            }
            List<? extends ConfiguredRequirementModifier<?, ?>> modifiers =
                    ExperienceRequirementSafety.parseRequiredRule(configuredRule);
            for (ConfiguredRequirementModifier<?, ?> modifier : modifiers) {
                RequirementFunction<?, ?> function = modifier.requirement().modifier();
                String requirementType = function.getRequirementType().toString();
                String functionId = function.getId().toString();
                ExperienceRequirementSafety.validateModifierIdentity(requirementType, functionId);
                if (ExperienceRequirementRules.shouldApply(
                        requirementType,
                        functionId,
                        function.isEnabled(),
                        mode == PlayerTeleportExperienceMode.ALWAYS)) {
                    int expected = ExperienceRequirementSafety.validateBeforeApply(requirementsContext, modifier);
                    apply(requirementsContext, modifier);
                    ExperienceRequirementSafety.validateAfterApply(requirementsContext, requirementType, expected);
                }
            }
        }
        WarpRequirement result = requirementsContext.resolve();
        ExperienceRequirementSafety.validateRequirementTree(result);
        return result;
    }

    @SuppressWarnings("deprecation")
    private static com.mojang.datafixers.util.Either<List<net.minecraft.world.entity.Entity>,
            net.blay09.mods.waystones.api.error.WaystoneTeleportError> tryTeleportSynchronously(
                    WaystoneTeleportContext teleportContext) {
        // Player targets are online, so their destination chunk is already loaded. The asynchronous
        // API additionally validates a real waystone block and therefore rejects this transient target.
        return WaystonesAPI.tryTeleport(teleportContext);
    }

    private static <T extends WarpRequirement, P> void apply(
            WarpRequirementsContextImpl context,
            ConfiguredRequirementModifier<T, P> modifier) {
        context.apply(modifier);
    }

    private static TeleportArrivalVerifier.Position positionOf(ServerPlayer player) {
        return new TeleportArrivalVerifier.Position(
                player.level().dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ());
    }

    private static TeleportArrivalVerifier.Target targetOf(WaystoneTeleportContext context) {
        var target = context.getTargetWaystone();
        BlockPos position = target.getPos();
        return new TeleportArrivalVerifier.Target(
                target.getDimension().identifier().toString(),
                position.getX(),
                position.getY(),
                position.getZ());
    }

    private static TeleportOutcome finalMovementOutcome(
            ServerPlayer sender,
            GuardedRollbackRequirement guardedRequirement,
            LockedWaystoneTeleportContext teleportContext) {
        boolean targetMatches = false;
        try {
            TeleportArrivalVerifier.Target validatedTarget = guardedRequirement.validatedTarget();
            targetMatches = validatedTarget != null
                    && TeleportArrivalVerifier.matchesTargetOrHorizontalAdjacent(
                            positionOf(sender),
                            validatedTarget);
        } catch (RuntimeException | LinkageError error) {
            WaystonesPTPT.LOGGER.error(
                    "Failed to verify the destination after a confirmed player movement; "
                            + "preserving consumed experience and reporting a compatibility warning.",
                    error);
        }
        if (targetMatches && !teleportContext.replacementAttempted()) {
            return TeleportOutcome.SUCCESS;
        }

        WaystonesPTPT.LOGGER.error(
                "Waystones moved a player without a matching validated destination "
                        + "(targetMatches={}, replacementAttempted={}); "
                        + "preserving consumed experience and reporting a compatibility warning.",
                targetMatches,
                teleportContext.replacementAttempted());
        return TeleportOutcome.MOVED_INCOMPATIBLY;
    }

    private static void restoreAfterFailure(ExperienceSnapshot snapshot, Throwable originalError) {
        try {
            snapshot.restore();
        } catch (RuntimeException restoreError) {
            originalError.addSuppressed(restoreError);
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

    private static final class GuardedRollbackRequirement implements WarpRequirement {
        private final ServerPlayer sender;
        private final ServerPlayer selectedPlayer;
        private final WaystoneImpl playerDestination;
        private final PlayerTeleportExperienceMode mode;
        private final WaystonesCompat.WarpStoneUse warpStoneUse;
        private final LockedWaystoneTeleportContext teleportContext;
        private final TeleportAttempt attempt;
        private WarpRequirement charge = NoRequirement.INSTANCE;
        private ExperienceSnapshot snapshot;
        private TeleportArrivalVerifier.Position beforeExecution;
        private TeleportArrivalVerifier.Target validatedTarget;

        private GuardedRollbackRequirement(
                ServerPlayer sender, ServerPlayer selectedPlayer, WaystoneImpl playerDestination,
                PlayerTeleportExperienceMode mode, WaystonesCompat.WarpStoneUse warpStoneUse,
                LockedWaystoneTeleportContext teleportContext, TeleportAttempt attempt) {
            this.sender = sender;
            this.selectedPlayer = selectedPlayer;
            this.playerDestination = playerDestination;
            this.mode = mode;
            this.warpStoneUse = warpStoneUse;
            this.teleportContext = teleportContext;
            this.attempt = attempt;
        }

        @Override
        public boolean canAfford(Player player) {
            prepareCharge(player);
            return charge.canAfford(player);
        }

        @Override
        public void consume(Player player) {
            consumeCharge(player, () -> charge.consume(player));
        }

        public void consume(WaystoneTeleportContext context, Player player) {
            consumeCharge(player, () -> charge.consume(context, player));
        }

        private void consumeCharge(Player player, Runnable consumer) {
            prepareCharge(player);
            if (!charge.canAfford(player)) {
                throw new TeleportRejectedException("experience changed before consumption");
            }
            validatedTarget = targetOf(teleportContext);
            attempt.beginCommit(sender.level().getServer().getTickCount());
            snapshot = ExperienceSnapshot.capture(sender);
            beforeExecution = positionOf(sender);
            try {
                consumer.run();
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
        }

        public void rollback(WaystoneTeleportContext context, Player player) {
            restore();
        }

        @Override
        public void rollback(Player player) {
            restore();
        }

        private void restore() {
            if (snapshot != null && !hasMovedSinceCommit()) {
                snapshot.restore();
                snapshot = null;
            }
        }

        private boolean hasMovedSinceCommit() {
            return beforeExecution != null && TeleportArrivalVerifier.hasMoved(beforeExecution, positionOf(sender));
        }

        private TeleportArrivalVerifier.Position beforeExecution() {
            return beforeExecution;
        }

        @Override
        public void appendHoverText(Player player, List<Component> tooltip) {
            charge.appendHoverText(player, tooltip);
        }

        @Override
        public boolean isEmpty() {
            // The transaction guard must execute even for free and creative teleports.
            return false;
        }

        private TeleportArrivalVerifier.Target validatedTarget() {
            return validatedTarget;
        }

        private void prepareCharge(Player player) {
            if (player != sender || !attempt.validatePreparation(sender.level().getServer().getTickCount())) {
                throw new TeleportRejectedException("the request expired or its session/menu/target changed");
            }
            if (teleportContext.replacementAttempted()) {
                throw new TeleportRejectedException("a Waystones event changed locked cost inputs");
            }
            if (!TeleportRuntime.isWarpStoneUseBound(sender, warpStoneUse)) {
                throw new TeleportRejectedException("the bound Warp Stone changed before consumption");
            }
            ServerPlayer currentTarget = sender.level().getServer().getPlayerList().getPlayer(selectedPlayer.getUUID());
            if (currentTarget == null || currentTarget.isRemoved()) {
                throw new TeleportRejectedException("the selected player disconnected");
            }
            // Preserve explicit event redirection, otherwise follow the selected player at execution time.
            if (teleportContext.getTargetWaystone() == playerDestination) {
                playerDestination.setDimension(currentTarget.level().dimension());
                playerDestination.setPos(currentTarget.blockPosition().immutable());
            }
            charge = sender.getAbilities().instabuild ? NoRequirement.INSTANCE
                    : resolveExperienceRequirement(teleportContext, mode);
        }
    }

    private static final class TeleportRejectedException extends RuntimeException {
        private TeleportRejectedException(String message) {
            super(message);
        }
    }
}
