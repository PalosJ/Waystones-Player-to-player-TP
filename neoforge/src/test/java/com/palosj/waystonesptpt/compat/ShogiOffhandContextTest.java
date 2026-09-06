package com.palosj.waystonesptpt.compat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.mojang.serialization.JsonOps;
import net.blay09.mods.shogi.common.context.aggregate.ShogiAggregateContext;
import net.blay09.mods.shogi.context.internal.ShogiContextImpl;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockMakers;
import static org.junit.jupiter.api.Assertions.*;

/** Runs through FML's JUnit launcher, so the production Shogi mixin is actually applied. */
class ShogiOffhandContextTest {
    @Test
    void inheritedOffhandRemainsBoundAndExplicitNestedOverridesRemainObservable() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ItemStack offhand = new ItemStack(Holder.direct(Items.DIAMOND_PICKAXE));
        offhand.set(DataComponents.MAX_DAMAGE, 1561);
        offhand.set(DataComponents.DAMAGE, 0);
        Player player = Mockito.mock(Player.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        Mockito.when(player.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        WaystoneTeleportContext delegate = (WaystoneTeleportContext) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { WaystoneTeleportContext.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEntity", "entity" -> player;
                    case "getWarpItem", "itemStack" -> offhand;
                    case "getWarpHand" -> InteractionHand.OFF_HAND;
                    case "getVariable", "getFromWaystone" -> Optional.empty();
                    case "getFlags" -> Set.of();
                    default -> null;
                });
        LockedWaystoneTeleportContext context = new LockedWaystoneTeleportContext(delegate, () -> { });
        var nested = new ShogiAggregateContext(context);
        assertTrue(nested instanceof PlayerTeleportShogiContext);
        assertTrue(((PlayerTeleportShogiContext) nested).waystonesptpt$isPlayerTeleport());
        assertSame(offhand, nested.itemStack());
        var rules = ShogiExperienceRuleSafety.compile(RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.EMPTY),
                List.of("damage_item(80)"), false, true);
        assertTrue(rules.evaluate(context, false).left().isPresent());
        assertEquals(80, rules.damage().value());
        nested.withItemStack(ItemStack.EMPTY);
        assertSame(ItemStack.EMPTY, nested.itemStack());
        var ordinary = new ShogiAggregateContext(new ShogiContextImpl().withEntity(player));
        assertFalse(((PlayerTeleportShogiContext) ordinary).waystonesptpt$isPlayerTeleport());
    }
}
