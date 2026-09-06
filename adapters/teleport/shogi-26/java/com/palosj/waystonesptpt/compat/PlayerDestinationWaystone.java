package com.palosj.waystonesptpt.compat;

import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.WaystoneKinds;
import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystoneVisibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class PlayerDestinationWaystone implements Waystone {
    private static final Method DEFAULT_RESOLVER = findDefaultResolver();
    private static final List<Direction> DIRECTIONS = List.of(
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST);

    private final MinecraftServer server;
    private final UUID playerId;
    private final String playerName;
    private final ResourceKey<Level> dimension;
    private final BlockPos position;
    private final Component name;

    PlayerDestinationWaystone(ServerPlayer target) {
        server = target.level().getServer();
        playerId = target.getUUID();
        playerName = target.getGameProfile().name();
        dimension = target.level().dimension();
        position = target.blockPosition().immutable();
        name = Component.literal(playerName);
    }

    @Override
    public UUID getWaystoneUid() {
        return playerId;
    }

    @Override
    public Component getName() {
        return name;
    }

    public Component getEffectiveName() {
        return name;
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    public WaystoneOrigin getOrigin() {
        return WaystoneOrigin.PLAYER;
    }

    @Override
    public boolean isOwner(Player player) {
        return playerId.equals(player.getUUID());
    }

    @Override
    public BlockPos getPos() {
        return position;
    }

    @Override
    public boolean isValid() {
        return liveTarget().isPresent();
    }

    @Override
    public Optional<UUID> getOwnerUid() {
        return Optional.of(playerId);
    }

    public Optional<String> getOwnerUsername() {
        return Optional.of(playerName);
    }

    @Override
    public Identifier getWaystoneKind() {
        return WaystoneKinds.WAYSTONE;
    }

    @Override
    public boolean isValidInLevel(ServerLevel level) {
        return liveTarget()
                .filter(player -> player.level() == level)
                .isPresent();
    }

    public Optional<TeleportDestination> resolveDestination(ServerLevel level) {
        if (!isValidInLevel(level)) {
            return Optional.empty();
        }
        if (DEFAULT_RESOLVER != null) {
            try {
                Object result = DEFAULT_RESOLVER.invoke(null, level, this);
                if (result instanceof Optional<?> optional && optional.orElse(null) instanceof TeleportDestination destination) {
                    return Optional.of(destination);
                }
                throw new IllegalStateException("Waystones default destination resolver returned an unexpected result");
            } catch (IllegalAccessException | InvocationTargetException error) {
                throw new IllegalStateException("Could not resolve the native player destination", error);
            }
        }
        for (Direction direction : DIRECTIONS) {
            BlockPos body = position.relative(direction);
            BlockPos head = body.above();
            if (!level.getBlockState(body).isSuffocating(level, body)
                    && !level.getBlockState(head).isSuffocating(level, head)) {
                return Optional.of(new TeleportDestination(
                        level,
                        new Vec3(body.getX() + 0.5, body.getY() + 0.5, body.getZ() + 0.5),
                        direction));
            }
        }
        return Optional.of(new TeleportDestination(level,
                new Vec3(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5),
                Direction.NORTH));
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public WaystoneVisibility getVisibility() {
        return WaystoneVisibility.ACTIVATION;
    }

    public Set<Identifier> getWaystoneGroups() {
        return Set.of();
    }

    private static Method findDefaultResolver() {
        try {
            return WaystonesAPI.class.getMethod("resolveDefaultDestination", ServerLevel.class, Waystone.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    Optional<ServerPlayer> onlineTarget() {
        if (server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.getPlayerList().getPlayer(playerId))
                .filter(player -> !player.isRemoved());
    }

    boolean hasMoved() {
        return onlineTarget().filter(player -> !dimension.equals(player.level().dimension())
                || !position.equals(player.blockPosition())).isPresent();
    }

    Optional<ServerPlayer> liveTarget() {
        return onlineTarget().filter(player -> dimension.equals(player.level().dimension())
                && position.equals(player.blockPosition()));
    }
}
