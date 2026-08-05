package com.palosj.waystonesplayer.teleport;

import com.palosj.waystonesplayer.Config;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

public final class HungerCostService {

    private HungerCostService() {
    }

    public static boolean isEnabled() {
        return Config.ENABLE_HUNGER_COST.get();
    }

    public static int calculateFoodCost(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return HungerCostRules.calculateFoodCost(Math.sqrt(dx * dx + dz * dz), Config.FOOD_COST_PER_500_BLOCKS.get());
    }

    public static boolean canAfford(Player player, int foodPoints) {
        if (!HungerCostRules.shouldCharge(player.isCreative(), player.isSpectator())) {
            return true;
        }
        FoodData fd = player.getFoodData();
        return HungerCostRules.getAvailableFoodPoints(fd.getFoodLevel(), fd.getSaturationLevel()) >= foodPoints;
    }

    public static void consume(Player player, int foodPoints) {
        if (!HungerCostRules.shouldCharge(player.isCreative(), player.isSpectator())) {
            return;
        }

        FoodData foodData = player.getFoodData();
        HungerCostRules.FoodState result = HungerCostRules.consume(
                foodData.getFoodLevel(),
                foodData.getSaturationLevel(),
                foodPoints);
        foodData.setFoodLevel(result.foodLevel());
        foodData.setSaturation(result.saturationLevel());
    }
}
