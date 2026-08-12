package com.palosj.waystonesplayer.compat;

import java.util.List;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.teleport.TeleportArrivalVerifier;
import com.palosj.waystonesplayer.teleport.TeleportOutcome;

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
import net.blay09.mods.waystones.requirement.RequirementModifierParser;
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
                target.level().dimension(),
                requestedTarget,
                WaystoneOrigin.PLAYER,
                target.getUUID());
        targetWaystone.setName(Component.literal(target.getScoreboardName()));
        targetWaystone.setTransient(true);

        WaystoneTeleportContext teleportContext = WaystonesAPI.createUnboundTeleportContext(sender, targetWaystone)
                .setWarpItem(warpStoneUse.stack())
                .setWarpHand(warpStoneUse.hand())
                .setPlaysSound(true)
                .setPlaysEffect(true)
                .setAppliesModifiers(false)
                .addFlag(WaystonesPlayer.id("player_destination"));

        WarpRequirement requirement = resolveExperienceRequirement(teleportContext, mode);
        if (!sender.getAbilities().instabuild && !requirement.canAfford(sender)) {
            return TeleportOutcome.UNAFFORDABLE;
        }

        ExperienceSnapshot snapshot = ExperienceSnapshot.capture(sender);
        teleportContext.setRequirements(sender.getAbilities().instabuild
                ? NoRequirement.INSTANCE
                : new ExactRollbackRequirement(teleportContext, requirement, snapshot));

        TeleportArrivalVerifier.Position before = positionOf(sender);
        TeleportArrivalVerifier.BlockTarget targetBlock = new TeleportArrivalVerifier.BlockTarget(
                target.level().dimension().identifier().toString(),
                requestedTarget.getX(), requestedTarget.getY(), requestedTarget.getZ());
        boolean apiReportedSender;
        try {
            var result = WaystonesAPI.tryTeleport(teleportContext);
            apiReportedSender = result.left()
                    .map(entities -> entities.stream().anyMatch(entity -> entity == sender))
                    .orElse(false);
        } catch (RuntimeException | LinkageError error) {
            if (TeleportArrivalVerifier.hasMoved(before, positionOf(sender))) {
                WaystonesPlayer.LOGGER.warn(
                        "Waystones reported an exception after the player moved; treating movement as successful.",
                        error);
                return TeleportOutcome.SUCCESS;
            }
            restoreAfterFailure(snapshot, error);
            throw error;
        }

        if (!TeleportArrivalVerifier.succeeded(apiReportedSender, before, positionOf(sender), targetBlock)) {
            snapshot.restore();
            return TeleportOutcome.FAILED;
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
        WarpRequirementsContextImpl context = new WarpRequirementsContextImpl(teleportContext);
        for (String rule : teleportsConfig.warpRequirements) {
            if (rule.isBlank()) {
                continue;
            }
            for (ConfiguredRequirementModifier<?, ?> modifier : RequirementModifierParser.parse(rule)) {
                RequirementFunction<?, ?> function = modifier.requirement().modifier();
                if (ExperienceRequirementRules.shouldApply(
                        function.getRequirementType().toString(),
                        function.getId().toString(),
                        function.isEnabled(),
                        mode == PlayerTeleportExperienceMode.ALWAYS)) {
                    apply(context, modifier);
                }
            }
        }
        return context.resolve();
    }

    private static <T extends WarpRequirement, P> void apply(
            WarpRequirementsContextImpl context,
            ConfiguredRequirementModifier<T, P> modifier) {
        context.apply(modifier);
    }

    private static TeleportArrivalVerifier.Position positionOf(ServerPlayer player) {
        return new TeleportArrivalVerifier.Position(
                player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ());
    }

    private static void restoreAfterFailure(ExperienceSnapshot snapshot, Throwable error) {
        try {
            snapshot.restore();
        } catch (RuntimeException restoreError) {
            error.addSuppressed(restoreError);
        }
    }

    private record ExperienceSnapshot(ServerPlayer player, float progress, int level, int total) {
        private static ExperienceSnapshot capture(ServerPlayer player) {
            return new ExperienceSnapshot(
                    player, player.experienceProgress, player.experienceLevel, player.totalExperience);
        }

        private void restore() {
            player.experienceProgress = progress;
            player.experienceLevel = level;
            player.totalExperience = total;
            player.connection.send(new ClientboundSetExperiencePacket(progress, total, level));
        }
    }

    private record ExactRollbackRequirement(
            WaystoneTeleportContext context,
            WarpRequirement delegate,
            ExperienceSnapshot snapshot) implements WarpRequirement {
        @Override
        public boolean canAfford(Player player) {
            return delegate.canAfford(player);
        }

        @Override
        public void consume(WaystoneTeleportContext ignored, Player player) {
            try {
                delegate.consume(context, player);
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
        }

        @Override
        public void rollback(WaystoneTeleportContext ignored, Player player) {
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
    }
}
