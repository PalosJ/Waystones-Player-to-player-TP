package com.palosj.waystonesplayer.teleport;

import java.util.Optional;
import java.util.UUID;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.compat.WaystonesTeleportCompat;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TeleportRuntime {
    private static final TeleportRuntimeBoundary LIVE = new LiveBoundary();

    private TeleportRuntime() {
    }

    static TeleportRuntimeBoundary live() {
        return LIVE;
    }

    public static boolean isWarpStoneUseBound(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse) {
        return LIVE.isWarpStoneUseBound(sender, warpStoneUse);
    }

    public static boolean hasAdjacentNonSuffocatingSpace(
            ServerPlayer sender,
            WaystoneTeleportContext context) {
        return LIVE.hasAdjacentNonSuffocatingSpace(sender, context);
    }

    private static final class LiveBoundary implements TeleportRuntimeBoundary {
        @Override
        public int currentTick(ServerPlayer sender) {
            return sender.level().getServer().getTickCount();
        }

        @Override
        public UUID playerId(ServerPlayer player) {
            return player.getUUID();
        }

        @Override
        public String playerName(ServerPlayer player) {
            return player.getScoreboardName();
        }

        @Override
        public Optional<WaystonesCompat.WarpStoneUse> resolveWarpStoneUse(ServerPlayer sender) {
            return WaystonesCompat.resolveWarpStoneUse(sender, sender.containerMenu);
        }

        @Override
        public Optional<TargetPlayer> resolveListedTarget(ServerPlayer sender, UUID targetPlayerId) {
            ServerPlayer target = sender.level().getServer().getPlayerList().getPlayer(targetPlayerId);
            if (target == null || !target.allowsListing()) {
                return Optional.empty();
            }
            return Optional.of(new TargetPlayer(target, target.getUUID()));
        }

        @Override
        public Optional<TeleportOutcome> tryTeleport(
                ServerPlayer sender,
                ServerPlayer target,
                WaystonesCompat.WarpStoneUse warpStoneUse,
                PlayerTeleportExperienceMode experienceMode) {
            return WaystonesTeleportCompat.tryTeleport(sender, target, warpStoneUse, experienceMode);
        }

        @Override
        public Optional<DurabilityTarget> resolveDurabilityTarget(
                ServerPlayer sender,
                WaystonesCompat.WarpStoneUse warpStoneUse) {
            return WaystonesCompat.resolveDurabilityTarget(sender, warpStoneUse).map(DurabilityTarget::new);
        }

        @Override
        public void damageWarpStone(
                DurabilityTarget target,
                ServerPlayer sender,
                WaystonesCompat.WarpStoneUse warpStoneUse) {
            DurabilityCompat.hurtAndBreak(target.stack(), sender, warpStoneUse.hand());
        }

        @Override
        public void resetFallDistance(ServerPlayer sender) {
            sender.resetFallDistance();
        }

        @Override
        public void closeContainer(ServerPlayer sender) {
            sender.closeContainer();
        }

        @Override
        public void displayMessage(ServerPlayer sender, String translationKey) {
            sender.displayClientMessage(
                    Component.translatable(translationKey).copy().withStyle(ChatFormatting.RED),
                    false);
        }

        @Override
        public boolean isWarpStoneUseBound(
                ServerPlayer sender,
                WaystonesCompat.WarpStoneUse warpStoneUse) {
            return WaystonesCompat.isWarpStoneUseBound(sender, warpStoneUse);
        }

        @Override
        public boolean hasAdjacentNonSuffocatingSpace(
                ServerPlayer sender,
                WaystoneTeleportContext context) {
            return WaystonesCompat.hasAdjacentNonSuffocatingSpace(sender, context);
        }
    }
}
