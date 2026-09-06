package com.palosj.waystonesptpt.teleport;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.compat.WaystonesCompat;
import com.palosj.waystonesptpt.compat.WaystonesTeleportCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
        public Optional<TargetPlayer> resolveOnlineTarget(ServerPlayer sender, UUID targetPlayerId) {
            ServerPlayer target = sender.level().getServer().getPlayerList().getPlayer(targetPlayerId);
            if (target == null || target.isRemoved()) {
                return Optional.empty();
            }
            return Optional.of(new TargetPlayer(target, target.getUUID()));
        }

        @Override
        public boolean allowsReceiving(ServerPlayer target) {
            return PlayerReceivingService.allows(target.level().getServer(), target.getUUID());
        }

        @Override
        public PlayerRotation captureRotation(ServerPlayer player) {
            return new PlayerRotation(player.getYRot(), player.getXRot());
        }

        @Override
        public BooleanSupplier captureRequestValidity(
                ServerPlayer sender, UUID targetId, WaystonesCompat.WarpStoneUse use) {
            var server = sender.level().getServer();
            var menu = sender.containerMenu;
            ItemStack snapshot = use.stack().copy();
            return () -> server != null && server.isSameThread()
                    && server.getPlayerList().getPlayer(sender.getUUID()) == sender
                    && sender.isAlive() && !sender.isRemoved()
                    && sender.containerMenu == menu
                    && WaystonesCompat.isWarpStoneUseBound(sender, use)
                    && ItemStack.isSameItemSameComponents(snapshot, use.stack())
                    && resolveOnlineTarget(sender, targetId).isPresent()
                    && PlayerReceivingService.allows(server, targetId);
        }

        @Override
        public boolean isSameSession(ServerPlayer sender) {
            var server = sender.level().getServer();
            return server != null && server.getPlayerList().getPlayer(sender.getUUID()) == sender;
        }

        @Override
        public void executeOnServerThread(ServerPlayer sender, Runnable action) {
            var server = sender.level().getServer();
            if (server.isSameThread()) {
                action.run();
            } else {
                server.execute(action);
            }
        }

        @Override
        public CompletionStage<Optional<TeleportOutcome>> tryTeleport(
                ServerPlayer sender,
                ServerPlayer target,
                WaystonesCompat.WarpStoneUse warpStoneUse,
                PlayerTeleportExperienceMode experienceMode,
                TeleportAttempt attempt) {
            return CompletableFuture.completedFuture(
                    WaystonesTeleportCompat.tryTeleport(sender, target, warpStoneUse, experienceMode, attempt));
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
                WaystonesCompat.WarpStoneUse warpStoneUse,
                int damage) {
            DurabilityCompat.hurtAndBreak(target.stack(), sender, warpStoneUse.hand(), damage);
        }

        @Override
        public void resetFallDistance(ServerPlayer sender) {
            sender.resetFallDistance();
        }

        @Override
        public void restoreRotation(ServerPlayer sender, PlayerRotation rotation) {
            sender.connection.teleport(
                    sender.getX(),
                    sender.getY(),
                    sender.getZ(),
                    rotation.yaw(),
                    rotation.pitch());
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
    }
}
