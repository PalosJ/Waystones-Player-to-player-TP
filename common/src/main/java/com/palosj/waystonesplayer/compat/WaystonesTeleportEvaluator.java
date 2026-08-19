package com.palosj.waystonesplayer.compat;

import java.util.List;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.teleport.TeleportArrivalVerifier;
import com.palosj.waystonesplayer.teleport.TeleportOutcome;
import com.palosj.waystonesplayer.teleport.TeleportRuntime;

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
            PlayerTeleportExperienceMode mode) {
        BlockPos requestedTarget = target.blockPosition();
        WaystoneImpl targetWaystone = new WaystoneImpl(
                WaystoneTypes.WAYSTONE,
                target.getUUID(),
                target.serverLevel().dimension(),
                requestedTarget,
                WaystoneOrigin.PLAYER,
                target.getUUID(),
                target.getGameProfile().getName());
        targetWaystone.setName(Component.literal(target.getGameProfile().getName()));
        targetWaystone.setTransient(true);

        WaystoneTeleportContext baseContext = WaystonesAPI.createUnboundTeleportContext(sender, targetWaystone)
                .setWarpItem(warpStoneUse.stack())
                .setWarpHand(warpStoneUse.hand())
                .setPlaysSound(true)
                .setPlaysEffect(true)
                .setAppliesModifiers(false)
                .addFlag(WaystonesPlayer.id("player_destination"));

        WarpRequirement requirement = resolveExperienceRequirement(baseContext, mode);
        if (!sender.getAbilities().instabuild && !requirement.canAfford(sender)) {
            return TeleportOutcome.UNAFFORDABLE;
        }

        ExperienceSnapshot experienceSnapshot = ExperienceSnapshot.capture(sender);
        LockedWaystoneTeleportContext teleportContext = new LockedWaystoneTeleportContext(baseContext);
        WarpRequirement chargedRequirement = sender.getAbilities().instabuild
                ? NoRequirement.INSTANCE
                : requirement;
        teleportContext.lockRequirements(new GuardedRollbackRequirement(
                chargedRequirement,
                experienceSnapshot,
                sender,
                warpStoneUse,
                teleportContext));

        TeleportArrivalVerifier.Position before = positionOf(sender);
        boolean apiReportedSender;
        try {
            var result = tryTeleportSynchronously(teleportContext);
            apiReportedSender = result.left()
                    .map(entities -> entities.stream().anyMatch(entity -> entity == sender))
                    .orElse(false);
        } catch (TeleportRejectedException error) {
            experienceSnapshot.restore();
            WaystonesPlayer.LOGGER.debug("Rejected an unsafe player-destination teleport: {}", error.getMessage());
            return TeleportOutcome.FAILED;
        } catch (RuntimeException | LinkageError error) {
            if (TeleportArrivalVerifier.hasMoved(before, positionOf(sender))) {
                WaystonesPlayer.LOGGER.warn(
                        "Waystones reported an exception after the player moved; treating the confirmed movement as successful.",
                        error);
                return TeleportOutcome.SUCCESS;
            }
            restoreAfterFailure(experienceSnapshot, error);
            throw error;
        }

        boolean confirmedMovement = TeleportArrivalVerifier.succeeded(
                apiReportedSender,
                before,
                positionOf(sender));
        if (!confirmedMovement) {
            experienceSnapshot.restore();
            return TeleportOutcome.FAILED;
        }
        if (teleportContext.replacementAttempted()) {
            WaystonesPlayer.LOGGER.error(
                    "Waystones moved a player after an event attempted to replace locked requirements; "
                            + "preserving the confirmed movement and consumed experience.");
        }
        return TeleportOutcome.SUCCESS;
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
                player.serverLevel().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ());
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

    private record GuardedRollbackRequirement(
            WarpRequirement delegate,
            ExperienceSnapshot snapshot,
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            LockedWaystoneTeleportContext teleportContext) implements WarpRequirement {
        @Override
        public boolean canAfford(Player player) {
            validate(player);
            return delegate.canAfford(player);
        }

        @Override
        public void consume(Player player) {
            validate(player);
            try {
                delegate.consume(player);
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
        }

        @Override
        public void rollback(Player player) {
            snapshot.restore();
        }

        @Override
        public void appendHoverText(Player player, List<Component> tooltip) {
            delegate.appendHoverText(player, tooltip);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        private void validate(Player player) {
            if (player != sender) {
                throw new TeleportRejectedException("requirement evaluated for an unexpected player");
            }
            if (teleportContext.replacementAttempted()) {
                throw new TeleportRejectedException("a Waystones event attempted to replace the locked requirements");
            }
            if (!TeleportRuntime.isWarpStoneUseBound(sender, warpStoneUse)) {
                throw new TeleportRejectedException("the bound Warp Stone changed before teleport consumption");
            }
            if (!TeleportRuntime.hasAdjacentNonSuffocatingSpace(sender, teleportContext)) {
                throw new TeleportRejectedException("the destination has no adjacent non-suffocating space");
            }
        }
    }

    private static final class TeleportRejectedException extends RuntimeException {
        private TeleportRejectedException(String message) {
            super(message);
        }
    }
}
