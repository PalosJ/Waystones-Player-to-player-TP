package com.palosj.waystonesplayer.compat;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class WaystonesCompat {
    private static final String WAYSTONES_NAMESPACE = "waystones";
    private static final String GET_WARP_ITEM_METHOD = "getWarpItem";
    private static final AtomicBoolean MENU_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    public static final ResourceLocation WARP_STONE_ID = ResourceLocation.fromNamespaceAndPath("waystones", "warp_stone");

    private WaystonesCompat() {
    }

    public static boolean isWarpStone(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return WARP_STONE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static void stopUsingWarpStone(Player player) {
        if (player.isUsingItem() && isWarpStone(player.getUseItem())) {
            player.stopUsingItem();
        }
    }

    public static boolean isWarpStoneMenu(AbstractContainerMenu menu) {
        if (menu == null) {
            return false;
        }

        try {
            ResourceLocation menuId = BuiltInRegistries.MENU.getKey(menu.getType());
            if (menuId == null || !WAYSTONES_NAMESPACE.equals(menuId.getNamespace())) {
                return false;
            }

            Method getWarpItem = menu.getClass().getMethod(GET_WARP_ITEM_METHOD);
            Object warpItem = getWarpItem.invoke(menu);
            if (warpItem instanceof ItemStack stack) {
                return isWarpStone(stack);
            }
            logMenuCompatibilityFailure(menu, null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            logMenuCompatibilityFailure(menu, e);
        }
        return false;
    }

    private static void logMenuCompatibilityFailure(AbstractContainerMenu menu, Throwable error) {
        if (!MENU_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            return;
        }

        String message = "Waystones menu compatibility is unavailable for {}; player destinations will be disabled.";
        if (error == null) {
            WaystonesPlayer.LOGGER.warn(message, menu.getClass().getName());
        } else {
            WaystonesPlayer.LOGGER.warn(message, menu.getClass().getName(), error);
        }
    }
}
