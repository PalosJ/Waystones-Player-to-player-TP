package com.palosj.waystonesplayer.compat;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.datafixers.util.Either;

import net.blay09.mods.shogi.context.executor.EffectExecutor;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class LockedWaystoneTeleportContext implements WaystoneTeleportContext {
    @FunctionalInterface
    interface RequirementEvaluator {
        Either<List<Object>, List<Object>> evaluate(LockedWaystoneTeleportContext context);
    }

    private final WaystoneTeleportContext delegate;
    private final Entity entity;
    private final Waystone targetWaystone;
    private final ItemStack warpItem;
    private final InteractionHand warpHand;
    private final LockedShogiExecutor executor;
    private RequirementEvaluator evaluator;
    private boolean locked;
    private boolean requirementReplacementAttempted;
    private boolean feeContextMutationAttempted;
    private boolean executorOverrideAttempted;

    LockedWaystoneTeleportContext(
            WaystoneTeleportContext delegate,
            Runnable beforeExecute) {
        this.delegate = delegate;
        entity = delegate.getEntity();
        targetWaystone = delegate.getTargetWaystone();
        warpItem = delegate.getWarpItem();
        warpHand = delegate.getWarpHand();
        executor = new LockedShogiExecutor(beforeExecute, () -> executorOverrideAttempted = true);
    }

    void lock(RequirementEvaluator evaluator) {
        if (locked) {
            throw new IllegalStateException("Waystones Shogi context is already locked");
        }
        this.evaluator = evaluator;
        locked = true;
    }

    void resetExecutor(boolean creative) {
        executor.reset(creative);
    }

    boolean wasModified() {
        return requirementReplacementAttempted || feeContextMutationAttempted || executorOverrideAttempted;
    }

    void requireUnmodified() {
        if (requirementReplacementAttempted) {
            throw new TeleportRejectedException("a Waystones event replaced locked requirements");
        }
        if (feeContextMutationAttempted) {
            throw new TeleportRejectedException("a Waystones event changed a fee-sensitive teleport field");
        }
        if (executorOverrideAttempted) {
            throw new TeleportRejectedException("a Waystones event overrode the locked Shogi executor");
        }
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Waystone getTargetWaystone() {
        return targetWaystone;
    }

    @Override
    public List<Mob> getLeashedEntities() {
        return delegate.getLeashedEntities();
    }

    @Override
    public List<Entity> getAdditionalEntities() {
        return delegate.getAdditionalEntities();
    }

    @Override
    public WaystoneTeleportContext addAdditionalEntity(Entity additionalEntity) {
        delegate.addAdditionalEntity(additionalEntity);
        return this;
    }

    @Override
    public Optional<Waystone> getFromWaystone() {
        return delegate.getFromWaystone();
    }

    @Override
    public WaystoneTeleportContext setFromWaystone(@Nullable Waystone fromWaystone) {
        if (locked) {
            feeContextMutationAttempted = true;
        } else {
            delegate.setFromWaystone(fromWaystone);
        }
        return this;
    }

    @Override
    public ItemStack getWarpItem() {
        return warpItem;
    }

    @Override
    public WaystoneTeleportContext setWarpItem(ItemStack warpItem) {
        if (locked) {
            feeContextMutationAttempted = true;
        } else {
            delegate.setWarpItem(warpItem);
        }
        return this;
    }

    @Override
    public InteractionHand getWarpHand() {
        return warpHand;
    }

    @Override
    public WaystoneTeleportContext setWarpHand(InteractionHand warpHand) {
        if (locked) {
            feeContextMutationAttempted = true;
        } else {
            delegate.setWarpHand(warpHand);
        }
        return this;
    }

    @Override
    public boolean isDimensionalTeleport() {
        return delegate.isDimensionalTeleport();
    }

    @Override
    public boolean playsSound() {
        return delegate.playsSound();
    }

    @Override
    public WaystoneTeleportContext setPlaysSound(boolean playsSound) {
        delegate.setPlaysSound(playsSound);
        return this;
    }

    @Override
    public boolean playsEffect() {
        return delegate.playsEffect();
    }

    @Override
    public WaystoneTeleportContext setPlaysEffect(boolean playsEffect) {
        delegate.setPlaysEffect(playsEffect);
        return this;
    }

    @Override
    public Set<Identifier> getFlags() {
        return locked ? Set.copyOf(delegate.getFlags()) : delegate.getFlags();
    }

    @Override
    public WaystoneTeleportContext addFlag(Identifier flag) {
        if (locked) {
            feeContextMutationAttempted = true;
        } else {
            delegate.addFlag(flag);
        }
        return this;
    }

    @Override
    public WaystoneTeleportContext removeFlag(Identifier flag) {
        if (locked) {
            feeContextMutationAttempted = true;
        } else {
            delegate.removeFlag(flag);
        }
        return this;
    }

    @Override
    public Either<List<Object>, List<Object>> getRequirements() {
        if (!locked || evaluator == null) {
            return delegate.getRequirements();
        }
        requireUnmodified();
        return evaluator.evaluate(this);
    }

    @Override
    public void setRequirements(Either<List<Object>, List<Object>> requirements) {
        if (locked) {
            requirementReplacementAttempted = true;
        } else {
            delegate.setRequirements(requirements);
        }
    }

    @Override
    public EffectExecutor executor() {
        return executor;
    }

    @Override
    public Level level() {
        return delegate.level();
    }

    @Override
    public Entity entity() {
        return entity;
    }

    @Override
    public BlockPos blockPos() {
        return delegate.blockPos();
    }

    @Override
    public BlockState blockState() {
        return delegate.blockState();
    }

    @Override
    public BlockEntity blockEntity() {
        return delegate.blockEntity();
    }

    @Override
    public ItemStack itemStack() {
        return warpItem;
    }

    @Override
    public Optional<Object> getVariable(String path) {
        return delegate.getVariable(path);
    }
}
