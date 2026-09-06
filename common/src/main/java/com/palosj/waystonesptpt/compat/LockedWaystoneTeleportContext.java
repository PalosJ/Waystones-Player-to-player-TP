package com.palosj.waystonesptpt.compat;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

final class LockedWaystoneTeleportContext implements WaystoneTeleportContext {
    private final WaystoneTeleportContext delegate;
    private WarpRequirement lockedRequirement;
    private boolean locked;
    private boolean replacementAttempted;

    LockedWaystoneTeleportContext(WaystoneTeleportContext delegate) {
        this.delegate = delegate;
    }

    void lockRequirements(WarpRequirement requirement) {
        if (locked) {
            throw new IllegalStateException("Waystones requirements are already locked");
        }
        delegate.setRequirements(requirement);
        lockedRequirement = delegate.getRequirements();
        locked = true;
    }

    boolean replacementAttempted() {
        return replacementAttempted;
    }

    @Override
    public Entity getEntity() {
        return delegate.getEntity();
    }

    @Override
    public Waystone getTargetWaystone() {
        return delegate.getTargetWaystone();
    }

    public WaystoneTeleportContext setTargetWaystone(Waystone targetWaystone) {
        try {
            WaystoneTeleportContext.class
                    .getMethod("setTargetWaystone", Waystone.class)
                    .invoke(delegate, targetWaystone);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeError) {
                throw runtimeError;
            }
            if (cause instanceof Error fatalError) {
                throw fatalError;
            }
            throw new IllegalStateException("Waystones target redirection failed", cause);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("This Waystones API does not support target redirection", error);
        }
        return this;
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
    public WaystoneTeleportContext setFromWaystone(Waystone fromWaystone) {
        if (locked) {
            replacementAttempted = true;
        } else {
            delegate.setFromWaystone(fromWaystone);
        }
        return this;
    }

    @Override
    public ItemStack getWarpItem() {
        return delegate.getWarpItem();
    }

    @Override
    public WaystoneTeleportContext setWarpItem(ItemStack warpItem) {
        if (locked) {
            replacementAttempted = true;
        } else {
            delegate.setWarpItem(warpItem);
        }
        return this;
    }

    @Override
    public InteractionHand getWarpHand() {
        return delegate.getWarpHand();
    }

    @Override
    public WaystoneTeleportContext setWarpHand(InteractionHand warpHand) {
        if (locked) {
            replacementAttempted = true;
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
    public WarpRequirement getRequirements() {
        return delegate.getRequirements();
    }

    @Override
    public WaystoneTeleportContext setRequirements(WarpRequirement warpRequirement) {
        if (!locked) {
            delegate.setRequirements(warpRequirement);
        } else if (warpRequirement != lockedRequirement) {
            replacementAttempted = true;
        }
        return this;
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
    public boolean appliesModifiers() {
        return delegate.appliesModifiers();
    }

    @Override
    public WaystoneTeleportContext setAppliesModifiers(boolean appliesModifiers) {
        if (locked) {
            replacementAttempted = true;
        } else {
            delegate.setAppliesModifiers(appliesModifiers);
        }
        return this;
    }

    @Override
    public Set<ResourceLocation> getFlags() {
        return locked ? Set.copyOf(delegate.getFlags()) : delegate.getFlags();
    }

    @Override
    public WaystoneTeleportContext addFlag(ResourceLocation flag) {
        if (locked) {
            replacementAttempted = true;
        } else {
            delegate.addFlag(flag);
        }
        return this;
    }

    @Override
    public WaystoneTeleportContext removeFlag(ResourceLocation flag) {
        if (locked) {
            replacementAttempted = true;
        } else {
            delegate.removeFlag(flag);
        }
        return this;
    }
}
