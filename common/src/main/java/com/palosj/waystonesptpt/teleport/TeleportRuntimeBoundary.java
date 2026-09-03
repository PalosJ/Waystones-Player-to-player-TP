package com.palosj.waystonesptpt.teleport;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.compat.WaystonesCompat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

interface TeleportRuntimeBoundary {
    int currentTick(ServerPlayer sender);

    UUID playerId(ServerPlayer player);

    String playerName(ServerPlayer player);

    Optional<WaystonesCompat.WarpStoneUse> resolveWarpStoneUse(ServerPlayer sender);

    Optional<TargetPlayer> resolveOnlineTarget(ServerPlayer sender, UUID targetPlayerId);

    PlayerRotation captureRotation(ServerPlayer player);

    CompletionStage<Optional<TeleportOutcome>> tryTeleport(
            ServerPlayer sender,
            ServerPlayer target,
            WaystonesCompat.WarpStoneUse warpStoneUse,
            PlayerTeleportExperienceMode experienceMode);

    void executeOnServerThread(ServerPlayer sender, Runnable action);

    Optional<DurabilityTarget> resolveDurabilityTarget(
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse);

    void damageWarpStone(
            DurabilityTarget target,
            ServerPlayer sender,
            WaystonesCompat.WarpStoneUse warpStoneUse);

    void resetFallDistance(ServerPlayer sender);

    void restoreRotation(ServerPlayer sender, PlayerRotation rotation);

    void closeContainer(ServerPlayer sender);

    void displayMessage(ServerPlayer sender, String translationKey);

    boolean isWarpStoneUseBound(ServerPlayer sender, WaystonesCompat.WarpStoneUse warpStoneUse);

    record TargetPlayer(ServerPlayer player, UUID id) {
    }

    record DurabilityTarget(ItemStack stack) {
    }

    record PlayerRotation(float yaw, float pitch) {
    }
}
