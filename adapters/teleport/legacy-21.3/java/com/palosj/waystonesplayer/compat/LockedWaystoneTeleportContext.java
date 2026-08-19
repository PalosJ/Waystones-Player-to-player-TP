package com.palosj.waystonesplayer.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Requirement-locking context for Waystones 21.3 through 21.9.
 *
 * <p>Those releases do not expose a warp-hand accessor, while modifier toggles
 * were added partway through the family. Optional methods therefore stay behind
 * reflection so one source remains valid across the real API range.</p>
 */
final class LockedWaystoneTeleportContext implements WaystoneTeleportContext {
    private static final Method SET_TARGET_WAYSTONE = findMethod(
            "setTargetWaystone", Waystone.class);
    private static final Method APPLIES_MODIFIERS = findMethod("appliesModifiers");
    private static final Method SET_APPLIES_MODIFIERS = findMethod(
            "setAppliesModifiers", boolean.class);

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
        invokeOptional(SET_TARGET_WAYSTONE, targetWaystone);
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
        delegate.setFromWaystone(fromWaystone);
        return this;
    }

    @Override
    public ItemStack getWarpItem() {
        return delegate.getWarpItem();
    }

    @Override
    public WaystoneTeleportContext setWarpItem(ItemStack warpItem) {
        delegate.setWarpItem(warpItem);
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
    public WaystoneTeleportContext setRequirements(WarpRequirement requirement) {
        if (!locked) {
            delegate.setRequirements(requirement);
        } else if (requirement != lockedRequirement) {
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

    public boolean appliesModifiers() {
        return (Boolean) invokeOptional(APPLIES_MODIFIERS);
    }

    public WaystoneTeleportContext setAppliesModifiers(boolean appliesModifiers) {
        invokeOptional(SET_APPLIES_MODIFIERS, appliesModifiers);
        return this;
    }

    @Override
    public Set<ResourceLocation> getFlags() {
        return delegate.getFlags();
    }

    @Override
    public WaystoneTeleportContext addFlag(ResourceLocation flag) {
        delegate.addFlag(flag);
        return this;
    }

    @Override
    public WaystoneTeleportContext removeFlag(ResourceLocation flag) {
        delegate.removeFlag(flag);
        return this;
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            return WaystoneTeleportContext.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Object invokeOptional(Method method, Object... arguments) {
        if (method == null) {
            throw new IllegalStateException("This Waystones API does not support the requested context operation");
        }
        try {
            return method.invoke(delegate, arguments);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeError) {
                throw runtimeError;
            }
            if (cause instanceof Error fatalError) {
                throw fatalError;
            }
            throw new IllegalStateException("Waystones context operation failed", cause);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Waystones context operation failed", error);
        }
    }
}
