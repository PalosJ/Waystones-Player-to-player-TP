package com.palosj.waystonesptpt.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.datafixers.util.Either;
import org.junit.jupiter.api.Test;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.world.InteractionHand;

class LockedWaystoneTeleportContextTest {
    @Test
    void keepsLockedRequirementsAndRejectsReplacement() {
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate(new AtomicInteger()), () -> {
        });
        Either<List<Object>, List<Object>> locked = Either.left(List.of("locked"));
        context.lock(ignored -> locked);

        assertSame(locked, context.getRequirements());
        assertFalse(context.wasModified());

        context.setRequirements(Either.left(List.of("replacement")));
        assertTrue(context.wasModified());
        assertThrows(TeleportRejectedException.class, context::requireUnmodified);
        assertThrows(TeleportRejectedException.class, context::getRequirements);
    }

    @Test
    void locksFeeSensitiveFieldsButAllowsObservableEventChanges() {
        AtomicInteger additionalEntities = new AtomicInteger();
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(
                delegate(additionalEntities),
                () -> {
                });
        assertEquals(InteractionHand.MAIN_HAND, context.getWarpHand());
        context.lock(ignored -> Either.left(List.of()));

        context.addAdditionalEntity(null);
        context.setPlaysSound(false);
        context.setPlaysEffect(false);
        assertEquals(1, additionalEntities.get());
        assertFalse(context.wasModified());

        context.setWarpHand(InteractionHand.OFF_HAND);
        assertEquals(InteractionHand.MAIN_HAND, context.getWarpHand());
        assertTrue(context.wasModified());
        assertThrows(TeleportRejectedException.class, context::requireUnmodified);
    }

    @Test
    void canOnlyBeLockedOnce() {
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate(new AtomicInteger()), () -> {
        });
        context.lock(ignored -> Either.left(List.of()));
        assertThrows(IllegalStateException.class, () -> context.lock(ignored -> Either.left(List.of())));
    }

    private static WaystoneTeleportContext delegate(AtomicInteger additionalEntities) {
        return (WaystoneTeleportContext) Proxy.newProxyInstance(
                WaystoneTeleportContext.class.getClassLoader(),
                new Class<?>[] { WaystoneTeleportContext.class },
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getWarpHand" -> InteractionHand.MAIN_HAND;
                    case "getAdditionalEntities", "getLeashedEntities" -> List.of();
                    case "getFlags" -> Set.of();
                    case "getFromWaystone", "getVariable" -> Optional.empty();
                    case "getRequirements" -> Either.left(List.of());
                    case "addAdditionalEntity" -> {
                        additionalEntities.incrementAndGet();
                        yield proxy;
                    }
                    default -> {
                        if (method.getReturnType().isInstance(proxy)) {
                            yield proxy;
                        }
                        if (method.getReturnType() == boolean.class) {
                            yield true;
                        }
                        yield null;
                    }
                });
    }
}
