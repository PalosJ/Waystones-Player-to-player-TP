package com.palosj.waystonesplayer.teleport;

final class HungerCostRules {
    private static final int MIN_FOOD_COST = 1;
    private static final int MAX_FOOD_COST = 18;
    private static final double BLOCKS_PER_FOOD_POINT = 500.0;

    private HungerCostRules() {
    }

    static int calculateFoodCost(double distance, int costPer500) {
        if (distance <= 0) {
            return MIN_FOOD_COST;
        }

        int cost = (int) Math.ceil(distance / BLOCKS_PER_FOOD_POINT * costPer500);
        return Math.max(MIN_FOOD_COST, Math.min(MAX_FOOD_COST, cost));
    }

    static int getAvailableFoodPoints(int foodLevel, float saturationLevel) {
        return foodLevel + (saturationLevel > 0.0F ? (int) Math.ceil(saturationLevel) : 0);
    }

    static FoodState consume(int foodLevel, float saturationLevel, int foodPoints) {
        if (foodPoints <= 0) {
            return new FoodState(foodLevel, saturationLevel);
        }

        int saturationPoints = Math.min(foodPoints, (int) Math.ceil(saturationLevel));
        float remainingSaturation = Math.max(0.0F, saturationLevel - saturationPoints);
        int remainingCost = foodPoints - saturationPoints;
        int remainingFood = Math.max(0, foodLevel - remainingCost);
        return new FoodState(remainingFood, remainingSaturation);
    }

    static boolean shouldCharge(boolean creative, boolean spectator) {
        return !creative && !spectator;
    }

    record FoodState(int foodLevel, float saturationLevel) {
    }
}
