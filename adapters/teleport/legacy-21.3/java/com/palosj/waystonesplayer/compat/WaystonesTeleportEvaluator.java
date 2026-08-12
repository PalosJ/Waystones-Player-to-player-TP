package com.palosj.waystonesplayer.compat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

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
    private static final Method SET_WARP_HAND = findMethod(
            WaystoneTeleportContext.class,
            "setWarpHand",
            net.minecraft.world.InteractionHand.class);
    private static final Method SET_APPLIES_MODIFIERS = findMethod(
            WaystoneTeleportContext.class,
            "setAppliesModifiers",
            boolean.class);
    private static final Method CONSUME_WITH_CONTEXT = findMethod(
            WarpRequirement.class,
            "consume",
            WaystoneTeleportContext.class,
            Player.class);
    private static final Method ROLLBACK_WITH_CONTEXT = findMethod(
            WarpRequirement.class,
            "rollback",
            WaystoneTeleportContext.class,
            Player.class);

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
        markTransientWhenSupported(targetWaystone);

        WaystoneTeleportContext teleportContext = WaystonesAPI.createUnboundTeleportContext(sender, targetWaystone)
                .setWarpItem(warpStoneUse.stack())
                .setPlaysSound(true)
                .setPlaysEffect(true)
                .addFlag(WaystonesPlayer.id("player_destination"));
        configureOptionalContextFeatures(teleportContext, warpStoneUse);

        WarpRequirement requirement = resolveExperienceRequirement(teleportContext, mode);
        if (!sender.getAbilities().instabuild && !requirement.canAfford(sender)) {
            return TeleportOutcome.UNAFFORDABLE;
        }

        ExperienceSnapshot experienceSnapshot = ExperienceSnapshot.capture(sender);
        teleportContext.setRequirements(sender.getAbilities().instabuild
                ? NoRequirement.INSTANCE
                : new ExactRollbackRequirement(requirement, experienceSnapshot));

        TeleportArrivalVerifier.Position before = positionOf(sender);
        TeleportArrivalVerifier.BlockTarget targetBlock = new TeleportArrivalVerifier.BlockTarget(
                target.level().dimension().location().toString(),
                requestedTarget.getX(),
                requestedTarget.getY(),
                requestedTarget.getZ());
        boolean apiReportedSender;
        try {
            var result = tryTeleportSynchronously(teleportContext);
            apiReportedSender = result.left()
                    .map(entities -> entities.stream().anyMatch(entity -> entity == sender))
                    .orElse(false);
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

        if (!TeleportArrivalVerifier.succeeded(apiReportedSender, before, positionOf(sender), targetBlock)) {
            experienceSnapshot.restore();
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

        WarpRequirementsContextImpl requirementsContext = new WarpRequirementsContextImpl(teleportContext);
        for (String configuredRule : teleportsConfig.warpRequirements) {
            if (configuredRule.isBlank()) {
                continue;
            }
            applyParsedModifiers(
                    requirementsContext,
                    RequirementModifierParser.parse(configuredRule),
                    mode);
        }
        return requirementsContext.resolve();
    }

    @SuppressWarnings("deprecation")
    private static com.mojang.datafixers.util.Either<List<net.minecraft.world.entity.Entity>,
            net.blay09.mods.waystones.api.error.WaystoneTeleportError> tryTeleportSynchronously(
                    WaystoneTeleportContext teleportContext) {
        return WaystonesAPI.tryTeleport(teleportContext);
    }

    private static <T extends WarpRequirement, P> void apply(
            WarpRequirementsContextImpl context,
            ConfiguredRequirementModifier<T, P> modifier) {
        context.apply(modifier);
    }

    private static void applyParsedModifiers(
            WarpRequirementsContextImpl context,
            Object parsed,
            PlayerTeleportExperienceMode mode) {
        if (parsed instanceof Optional<?> optional) {
            optional.ifPresent(candidate -> applyIfExperienceModifier(context, candidate, mode));
            return;
        }
        if (parsed instanceof Iterable<?> candidates) {
            for (Object candidate : candidates) {
                applyIfExperienceModifier(context, candidate, mode);
            }
        }
    }

    private static void applyIfExperienceModifier(
            WarpRequirementsContextImpl context,
            Object candidate,
            PlayerTeleportExperienceMode mode) {
        if (!(candidate instanceof ConfiguredRequirementModifier<?, ?> modifier)) {
            return;
        }
        RequirementFunction<?, ?> function = modifier.requirement().modifier();
        if (ExperienceRequirementRules.shouldApply(
                function.getRequirementType().toString(),
                function.getId().toString(),
                function.isEnabled(),
                mode == PlayerTeleportExperienceMode.ALWAYS)) {
            apply(context, modifier);
        }
    }

    private static void markTransientWhenSupported(WaystoneImpl targetWaystone) {
        try {
            WaystoneImpl.class.getMethod("setTransient", boolean.class).invoke(targetWaystone, true);
        } catch (NoSuchMethodException ignored) {
            // Waystones 21.3 predates transient waystones; synchronous teleport accepts this unbound target.
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not mark the player destination as transient", error);
        }
    }

    private static void configureOptionalContextFeatures(
            WaystoneTeleportContext context,
            WaystonesCompat.WarpStoneUse warpStoneUse) {
        invokeOptional(SET_WARP_HAND, context, warpStoneUse.hand());
        invokeOptional(SET_APPLIES_MODIFIERS, context, false);
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static void invokeOptional(Method method, Object receiver, Object... arguments) {
        if (method == null) {
            return;
        }
        try {
            method.invoke(receiver, arguments);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not invoke a supported Waystones compatibility method", error);
        }
    }

    private static void invokeContextRequirement(
            Method method,
            WarpRequirement delegate,
            WaystoneTeleportContext context,
            Player player) {
        if (method == null) {
            throw new IllegalStateException("Waystones context requirement method is unavailable");
        }
        invokeOptional(method, delegate, context, player);
    }

    private static TeleportArrivalVerifier.Position positionOf(ServerPlayer player) {
        return new TeleportArrivalVerifier.Position(
                player.level().dimension().location().toString(),
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

    private record ExactRollbackRequirement(
            WarpRequirement delegate,
            ExperienceSnapshot snapshot) implements WarpRequirement {
        @Override
        public boolean canAfford(Player player) {
            return delegate.canAfford(player);
        }

        @Override
        public void consume(Player player) {
            try {
                delegate.consume(player);
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
        }

        // This overload intentionally compiles against early 1.21.x where it is not yet part of
        // WarpRequirement. On 21.10.2+, JVM interface dispatch selects it and preserves the newer
        // context-aware CombinedRequirement behavior without raising the minimum dependency.
        public void consume(WaystoneTeleportContext context, Player player) {
            try {
                invokeContextRequirement(CONSUME_WITH_CONTEXT, delegate, context, player);
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
        }

        @Override
        public void rollback(Player player) {
            snapshot.restore();
        }

        public void rollback(WaystoneTeleportContext context, Player player) {
            try {
                if (ROLLBACK_WITH_CONTEXT != null) {
                    invokeContextRequirement(ROLLBACK_WITH_CONTEXT, delegate, context, player);
                }
            } catch (RuntimeException | Error error) {
                restoreAfterFailure(snapshot, error);
                throw error;
            }
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
