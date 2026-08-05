package com.palosj.waystonesplayer.teleport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungerCostRulesTest {

    @Test
    void calculatesDistanceBoundariesAndClamp() {
        assertEquals(1, HungerCostRules.calculateFoodCost(-1.0, 1));
        assertEquals(1, HungerCostRules.calculateFoodCost(0.0, 1));
        assertEquals(1, HungerCostRules.calculateFoodCost(500.0, 1));
        assertEquals(2, HungerCostRules.calculateFoodCost(500.01, 1));
        assertEquals(2, HungerCostRules.calculateFoodCost(500.0, 2));
        assertEquals(18, HungerCostRules.calculateFoodCost(100_000.0, 20));
    }

    @Test
    void countsFractionalSaturationAsOneAvailablePoint() {
        assertEquals(13, HungerCostRules.getAvailableFoodPoints(10, 2.5F));
        assertEquals(11, HungerCostRules.getAvailableFoodPoints(10, 0.5F));
    }

    @Test
    void consumesSaturationBeforeFood() {
        HungerCostRules.FoodState result = HungerCostRules.consume(10, 2.5F, 2);
        assertEquals(10, result.foodLevel());
        assertEquals(0.5F, result.saturationLevel());

        result = HungerCostRules.consume(result.foodLevel(), result.saturationLevel(), 1);
        assertEquals(10, result.foodLevel());
        assertEquals(0.0F, result.saturationLevel());

        result = HungerCostRules.consume(result.foodLevel(), result.saturationLevel(), 2);
        assertEquals(8, result.foodLevel());
    }

    @Test
    void consumesMaximumCostImmediately() {
        HungerCostRules.FoodState result = HungerCostRules.consume(20, 0.0F, 18);

        assertEquals(2, result.foodLevel());
        assertEquals(0.0F, result.saturationLevel());
    }

    @Test
    void neverUnderflowsFoodOrSaturation() {
        HungerCostRules.FoodState result = HungerCostRules.consume(1, 0.5F, 18);

        assertEquals(0, result.foodLevel());
        assertEquals(0.0F, result.saturationLevel());
    }

    @Test
    void ignoresNonPositiveConsumption() {
        assertEquals(new HungerCostRules.FoodState(10, 2.5F), HungerCostRules.consume(10, 2.5F, 0));
        assertEquals(new HungerCostRules.FoodState(10, 2.5F), HungerCostRules.consume(10, 2.5F, -1));
    }

    @Test
    void exemptsCreativeAndSpectatorModes() {
        assertTrue(HungerCostRules.shouldCharge(false, false));
        assertFalse(HungerCostRules.shouldCharge(true, false));
        assertFalse(HungerCostRules.shouldCharge(false, true));
        assertFalse(HungerCostRules.shouldCharge(true, true));
    }
}
