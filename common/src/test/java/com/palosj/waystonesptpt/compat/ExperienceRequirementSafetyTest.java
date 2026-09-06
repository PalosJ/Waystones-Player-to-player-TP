package com.palosj.waystonesptpt.compat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class ExperienceRequirementSafetyTest {
    @Test
    void acceptsZeroBoundaryAndOfficialDefaultArithmetic() {
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "$xp_points_cost = if(condition = is_interdimensional, then = 27, else = $distance * 0.01)"));
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "source(is_warp_plate()), target(is_global()) -> $xp_points_cost = 0"));
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "$xp_points_cost = clamp($xp_points_cost, 0, 2147483647)"));
    }

    @Test
    void allowsSignedPredicatesAndArithmeticButRejectsInvalidEvaluatedCosts() {
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "is_above_y(-64) -> $xp_points_cost = $distance - 1"));
        assertDoesNotThrow(() -> ShogiExperienceRuleSafety.validateNumericLiterals(
                "name_equals(\"Infinity-NaN\") -> $xp_points_cost = 0"));
        for (Object invalid : new Object[] { -1, -0.01, Double.NaN, Double.POSITIVE_INFINITY,
                2147483648L, "NaN", "2147483648", "-1", "80.5" }) {
            assertThrows(IllegalArgumentException.class, () -> ShogiExperienceRuleSafety.checkedAmount(invalid));
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, ShogiExperienceRuleSafety.checkedAmount(0));
        org.junit.jupiter.api.Assertions.assertEquals(80, ShogiExperienceRuleSafety.checkedAmount(80.9));
        org.junit.jupiter.api.Assertions.assertEquals(80, ShogiExperienceRuleSafety.checkedAmount(new com.google.gson.JsonPrimitive(80)));
        org.junit.jupiter.api.Assertions.assertEquals(80, ShogiExperienceRuleSafety.checkedAmount("80"));
        org.junit.jupiter.api.Assertions.assertEquals(Integer.MAX_VALUE,
                ShogiExperienceRuleSafety.checkedAmount(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> ShogiExperienceRuleSafety.validateNumericLiterals("$xp_points_cost = Infinity"));
    }

    @Test
    void compilesTheCompleteInstalledWaystonesDefaultRules() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var ops = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.EMPTY);
        var rules = ShogiExperienceRuleSafety.rulesWithSettings(
                new net.blay09.mods.waystones.config.WaystonesConfig().rules);
        for (boolean xp : new boolean[] { false, true }) {
            for (boolean durability : new boolean[] { false, true }) {
                assertDoesNotThrow(() -> ShogiExperienceRuleSafety.compile(ops, rules, xp, durability));
            }
        }
    }

    @Test
    void refreshesPreparedRulesAfterReloadAndNeverReusesCostsAfterAnInvalidReload() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var cache = new ShogiExperienceRuleSafety.RuleCache(net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.EMPTY));
        var config = new net.blay09.mods.waystones.config.WaystonesConfig().rules;
        var mode = com.palosj.waystonesptpt.PlayerTeleportExperienceMode.FOLLOW_WAYSTONES;
        config.warpRequirements = new java.util.ArrayList<>(java.util.List.of("damage_item(80)"));
        var first = cache.get(config, mode);
        org.junit.jupiter.api.Assertions.assertSame(first, cache.get(config, mode));
        config.warpRequirements.set(0, "damage_item(40)");
        var changed = cache.get(config, mode);
        org.junit.jupiter.api.Assertions.assertNotSame(first, changed);
        config.enableDurability = !config.enableDurability;
        org.junit.jupiter.api.Assertions.assertNotSame(changed, cache.get(config, mode));
        config.warpRequirements.set(0, "unknown_player_teleport_cost()");
        assertThrows(IllegalArgumentException.class, () -> cache.get(config, mode));
        assertThrows(IllegalArgumentException.class, () -> cache.get(config, mode));
    }

    @Test
    void validatesCalculatedCostsAndDefersNativeDamageWithoutMutatingTheStack() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var ops = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.EMPTY);
        var stack = new net.minecraft.world.item.ItemStack(
                net.minecraft.core.Holder.direct(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE, 1561);
        stack.set(net.minecraft.core.component.DataComponents.DAMAGE, 0);
        var player = org.mockito.Mockito.mock(net.minecraft.world.entity.player.Player.class,
                org.mockito.Mockito.withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        org.mockito.Mockito.when(player.getMainHandItem()).thenReturn(stack);
        org.mockito.Mockito.when(player.getAbilities()).thenReturn(new net.minecraft.world.entity.player.Abilities());
        var delegate = (net.blay09.mods.waystones.api.WaystoneTeleportContext) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { net.blay09.mods.waystones.api.WaystoneTeleportContext.class },
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getEntity", "entity" -> player;
                    case "getWarpItem", "itemStack" -> stack;
                    case "getWarpHand" -> net.minecraft.world.InteractionHand.MAIN_HAND;
                    case "getVariable", "getFromWaystone" -> java.util.Optional.empty();
                    case "getFlags" -> java.util.Set.of();
                    default -> null;
                });
        var context = new LockedWaystoneTeleportContext(delegate, () -> { });
        for (String calculation : new String[] { "-1", "1 / 0", "2147483647 + 1" }) {
            assertThrows(IllegalArgumentException.class, () -> {
                var rules = ShogiExperienceRuleSafety.compile(ops,
                        java.util.List.of("$xp_points_cost = " + calculation), true, true);
                rules.evaluate(context, false);
            });
        }
        var overflowingAggregate = ShogiExperienceRuleSafety.compile(ops,
                java.util.List.of("xp_points_cost(2147483647)", "xp_points_cost(2147483647)"), true, false);
        assertThrows(IllegalArgumentException.class, () -> overflowingAggregate.evaluate(context, false));
        var rules = ShogiExperienceRuleSafety.compile(ops,
                java.util.List.of("$damage_item = 100 - 20"), false, true);
        assertTrue(rules.evaluate(context, false).left().isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(80, rules.damage().value());
        ((net.blay09.mods.shogi.context.executor.DeferredEffectExecutor) context.executor()).execute();
        org.junit.jupiter.api.Assertions.assertEquals(0, stack.getDamageValue());
        var disabled = ShogiExperienceRuleSafety.compile(ops,
                java.util.List.of("damage_item(80)", "xp_points_cost(1)"), false, false);
        assertTrue(disabled.evaluate(context, false).left().isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(0, disabled.damage().value());
    }

    @Test
    void classifiesOnlyKnownExperiencePureAndExcludedEffects() {
        Identifier points = id("shogi", "xp_points_cost");
        Identifier levels = id("shogi", "xp_level_cost");
        assertTrue(ShogiExperienceRuleSafety.isExperienceEffect(points));
        assertTrue(ShogiExperienceRuleSafety.isExperienceEffect(levels));
        assertFalse(ShogiExperienceRuleSafety.isExcludedCostEffect(points));
        assertFalse(ShogiExperienceRuleSafety.isExcludedCostEffect(id("shogi", "damage_item")));
        assertTrue(ShogiExperienceRuleSafety.isKnownPureEffect(id("waystones", "is_fleeting_memorial")));
        assertTrue(ShogiExperienceRuleSafety.isKnownPureEffect(id("waystones", "is_warp_stone")));
        assertFalse(ShogiExperienceRuleSafety.isKnownPureEffect(id("example", "free_teleport")));
    }

    @Test
    void acceptsOnlyMonotonicNonNegativeIntegerAggregates() {
        assertDoesNotThrow(() -> LockedShogiExecutor.requireCost(0, "zero"));
        assertDoesNotThrow(() -> LockedShogiExecutor.requireCost(Integer.MAX_VALUE, "maximum"));
        assertDoesNotThrow(() -> LockedShogiExecutor.validateAggregateProgress(5, 5));
        assertDoesNotThrow(() -> LockedShogiExecutor.validateAggregateProgress(5, 6));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(-1, "negative"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(Double.NaN, "nan"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.requireCost(Double.POSITIVE_INFINITY, "infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> LockedShogiExecutor.validateAggregateProgress(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
