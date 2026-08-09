package com.palosj.waystonesplayer.compat;

import java.util.List;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.teleport.TeleportCost;

import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.requirement.RequirementFunction;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.blay09.mods.waystones.requirement.ConfiguredRequirementModifier;
import net.blay09.mods.waystones.requirement.RequirementModifierParser;
import net.blay09.mods.waystones.requirement.WarpRequirementsContextImpl;
import net.minecraft.server.level.ServerPlayer;

final class WaystonesExperienceEvaluator {
    private WaystonesExperienceEvaluator() {
    }

    static TeleportCost resolveExperienceCost(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode mode) {
        var teleportsConfig = WaystonesConfig.getActive().teleports;
        if (!mode.shouldEvaluateWaystonesExperience(teleportsConfig.enableCosts)) {
            return TeleportCost.NONE;
        }

        WaystoneImpl targetWaystone = new WaystoneImpl(
                WaystoneTypes.WAYSTONE,
                target.getUUID(),
                target.serverLevel().dimension(),
                target.blockPosition(),
                WaystoneOrigin.PLAYER,
                target.getUUID(),
                target.getGameProfile().getName());
        targetWaystone.setTransient(true);

        WaystoneTeleportContext teleportContext = WaystonesAPI.createUnboundTeleportContext(sender, targetWaystone)
                .setWarpItem(warpStoneUse.stack())
                .setWarpHand(warpStoneUse.hand());
        WarpRequirementsContextImpl requirementsContext = new WarpRequirementsContextImpl(teleportContext);

        for (String configuredRule : teleportsConfig.warpRequirements) {
            if (configuredRule.isBlank()) {
                continue;
            }
            List<? extends ConfiguredRequirementModifier<?, ?>> modifiers = RequirementModifierParser.parse(configuredRule);
            for (ConfiguredRequirementModifier<?, ?> modifier : modifiers) {
                RequirementFunction<?, ?> function = modifier.requirement().modifier();
                if (shouldApply(function, mode)) {
                    apply(requirementsContext, modifier);
                }
            }
        }

        WarpRequirement requirement = requirementsContext.resolve();
        return TeleportCost.exemptWhen(
                sender.getAbilities().instabuild,
                new WaystonesRequirementCost(sender, requirement));
    }

    private static boolean shouldApply(RequirementFunction<?, ?> function, PlayerTeleportExperienceMode mode) {
        return ExperienceRequirementRules.shouldApply(
                function.getRequirementType().toString(),
                function.getId().toString(),
                function.isEnabled(),
                mode == PlayerTeleportExperienceMode.ALWAYS);
    }

    private static <T extends WarpRequirement, P> void apply(
            WarpRequirementsContextImpl context,
            ConfiguredRequirementModifier<T, P> modifier) {
        context.apply(modifier);
    }

    private record WaystonesRequirementCost(ServerPlayer player, WarpRequirement requirement) implements TeleportCost {
        @Override
        public boolean canAfford() {
            return requirement.canAfford(player);
        }

        @Override
        public void consume() {
            requirement.consume(player);
        }

        @Override
        public void rollback() {
            requirement.rollback(player);
        }
    }
}
