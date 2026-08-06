package com.palosj.waystonesplayer.teleport;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.palosj.waystonesplayer.Config;
import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.WaystonesPlayer;
import com.palosj.waystonesplayer.compat.WaystonesCompat;
import com.palosj.waystonesplayer.compat.WaystonesExperienceCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;

public final class PlayerTeleportService {
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final RequestRateLimiter REQUEST_LIMITER = new RequestRateLimiter(REQUEST_COOLDOWN_TICKS);

    private PlayerTeleportService() {
    }

    public static void handleRequest(ServerPlayer sender, UUID targetPlayerId) {
        int currentTick = sender.server.getTickCount();
        RequestRateLimiter.Result rateLimitResult = REQUEST_LIMITER.acquire(sender.getUUID(), currentTick);
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_NOTIFY) {
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_cooling_down"), false);
            return;
        }
        if (rateLimitResult == RequestRateLimiter.Result.REJECTED_SILENT) {
            return;
        }

        AbstractContainerMenu menu = sender.containerMenu;
        Optional<WaystonesCompat.WarpStoneUse> warpStoneUse = WaystonesCompat.resolveWarpStoneUse(sender, menu);
        if (warpStoneUse.isEmpty()) {
            WaystonesPlayer.LOGGER.debug("{} sent an invalid warp stone player teleport request.", sender.getGameProfile().getName());
            sender.displayClientMessage(translatable("message.waystonesplayer.invalid_context"), false);
            return;
        }

        ServerPlayer target = sender.server.getPlayerList().getPlayer(targetPlayerId);
        if (target == null) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_offline"), false);
            return;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.displayClientMessage(translatable("message.waystonesplayer.target_self"), false);
            return;
        }

        PlayerTeleportExperienceMode experienceMode = Config.PLAYER_TELEPORT_EXPERIENCE_MODE.get();
        TeleportCost experienceCost = TeleportCost.NONE;
        if (experienceMode != PlayerTeleportExperienceMode.NEVER) {
            Optional<TeleportCost> resolvedCost = WaystonesExperienceCompat.resolveExperienceCost(
                    sender,
                    target,
                    warpStoneUse.orElseThrow(),
                    experienceMode);
            if (resolvedCost.isEmpty()) {
                sender.displayClientMessage(translatable("message.waystonesplayer.experience_cost_unavailable"), false);
                return;
            }
            experienceCost = resolvedCost.orElseThrow();
        }

        Vec3 targetPos = target.position();
        ServerLevel targetLevel = target.serverLevel();
        float targetYRot = target.getYRot();
        float targetXRot = target.getXRot();

        TeleportTransaction.Result result;
        try {
            result = TeleportTransaction.execute(experienceCost, () -> sender.teleportTo(
                    targetLevel,
                    targetPos.x,
                    targetPos.y,
                    targetPos.z,
                    Set.of(),
                    targetYRot,
                    targetXRot));
        } catch (RuntimeException e) {
            WaystonesPlayer.LOGGER.error("Failed to teleport {} to {}.", sender.getGameProfile().getName(), target.getGameProfile().getName(), e);
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_failed"), false);
            return;
        }

        if (result == TeleportTransaction.Result.UNAFFORDABLE) {
            sender.displayClientMessage(translatable("message.waystonesplayer.insufficient_experience"), false);
            return;
        }
        if (result == TeleportTransaction.Result.FAILED) {
            sender.displayClientMessage(translatable("message.waystonesplayer.teleport_failed"), false);
            return;
        }

        sender.resetFallDistance();
        WaystonesCompat.WarpStoneUse successfulUse = warpStoneUse.orElseThrow();
        successfulUse.stack().hurtAndBreak(1, sender, LivingEntity.getSlotForHand(successfulUse.hand()));
        sender.closeContainer();
    }

    public static void clearCooldown(UUID playerId) {
        REQUEST_LIMITER.clear(playerId);
    }

    private static Component translatable(String key) {
        return Component.translatable(key).copy().withStyle(ChatFormatting.RED);
    }
}
