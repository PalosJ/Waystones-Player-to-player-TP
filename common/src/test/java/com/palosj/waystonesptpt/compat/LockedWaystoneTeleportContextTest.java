package com.palosj.waystonesptpt.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.blay09.mods.waystones.requirement.ExperiencePointsRequirement;
import net.blay09.mods.waystones.requirement.NoRequirement;

class LockedWaystoneTeleportContextTest {
    @Test
    void locksFeeInputsAndDoesNotExposeAMutableFlagSet() {
        for (Consumer<LockedWaystoneTeleportContext> mutation : java.util.List.<Consumer<LockedWaystoneTeleportContext>>of(
                context -> context.setWarpItem(null),
                context -> context.setWarpHand(null),
                context -> context.setFromWaystone(null),
                context -> context.setAppliesModifiers(true),
                context -> context.addFlag(null),
                context -> context.removeFlag(null))) {
            LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate(new AtomicReference<>()));
            context.lockRequirements(NoRequirement.INSTANCE);
            mutation.accept(context);
            assertTrue(context.replacementAttempted());
        }
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate(new AtomicReference<>()));
        context.lockRequirements(NoRequirement.INSTANCE);
        assertThrows(UnsupportedOperationException.class, () -> context.getFlags().clear());
    }

    @Test
    void keepsTheLockedRequirementAndRecordsReplacementAttempts() {
        AtomicReference<WarpRequirement> activeRequirement = new AtomicReference<>();
        WaystoneTeleportContext delegate = delegate(activeRequirement);
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate);

        context.lockRequirements(NoRequirement.INSTANCE);
        assertSame(NoRequirement.INSTANCE, context.getRequirements());
        assertFalse(context.replacementAttempted());

        context.setRequirements(new ExperiencePointsRequirement(1));
        assertSame(NoRequirement.INSTANCE, context.getRequirements());
        assertTrue(context.replacementAttempted());
    }

    @Test
    void delegatesTargetRedirectionOnlyWhenTheRuntimeApiSupportsIt() {
        AtomicReference<WarpRequirement> activeRequirement = new AtomicReference<>();
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate(activeRequirement));
        boolean apiSupportsRedirection = Arrays.stream(WaystoneTeleportContext.class.getMethods())
                .anyMatch(method -> method.getName().equals("setTargetWaystone"));

        if (apiSupportsRedirection) {
            context.setTargetWaystone(null);
        } else {
            assertThrows(IllegalStateException.class, () -> context.setTargetWaystone(null));
        }
    }

    private static WaystoneTeleportContext delegate(AtomicReference<WarpRequirement> activeRequirement) {
        return (WaystoneTeleportContext) Proxy.newProxyInstance(
                WaystoneTeleportContext.class.getClassLoader(),
                new Class<?>[] { WaystoneTeleportContext.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("setRequirements")) {
                        activeRequirement.set((WarpRequirement) arguments[0]);
                        return proxy;
                    }
                    if (method.getName().equals("getRequirements")) {
                        return activeRequirement.get();
                    }
                    if (method.getName().equals("getFlags")) {
                        return Set.of();
                    }
                    if (method.getReturnType().isInstance(proxy)) {
                        return proxy;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
    }
}
