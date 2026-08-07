package com.palosj.waystonesplayer.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesplayer.WaystonesPlayer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class WaystonesCompat {
    private static final String GET_WARP_ITEM_METHOD = "getWarpItem";
    private static final AtomicBoolean MENU_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    public static final ResourceLocation WARP_STONE_ID = ResourceLocation.fromNamespaceAndPath("waystones", "warp_stone");
    private static final ResourceLocation WARP_STONE_MENU_ID = ResourceLocation.fromNamespaceAndPath(
            "waystones",
            "warp_stone_selection");

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
        return getWarpItem(menu).filter(WaystonesCompat::isWarpStone).isPresent();
    }

    public static Optional<WarpStoneUse> resolveWarpStoneUse(ServerPlayer player, AbstractContainerMenu menu) {
        Optional<ItemStack> warpItem = getWarpItem(menu).filter(WaystonesCompat::isWarpStone);
        if (warpItem.isEmpty()) {
            return Optional.empty();
        }

        ItemStack stack = warpItem.orElseThrow();
        if (player.getItemInHand(InteractionHand.MAIN_HAND) == stack) {
            return Optional.of(new WarpStoneUse(stack, InteractionHand.MAIN_HAND));
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND) == stack) {
            return Optional.of(new WarpStoneUse(stack, InteractionHand.OFF_HAND));
        }
        return Optional.empty();
    }

    private static Optional<ItemStack> getWarpItem(AbstractContainerMenu menu) {
        if (menu == null) {
            return Optional.empty();
        }

        try {
            ResourceLocation menuId = BuiltInRegistries.MENU.getKey(menu.getType());
            if (!WARP_STONE_MENU_ID.equals(menuId)) {
                return Optional.empty();
            }

            Method getWarpItem = menu.getClass().getMethod(GET_WARP_ITEM_METHOD);
            Object warpItem = getWarpItem.invoke(menu);
            if (warpItem instanceof ItemStack stack) {
                return Optional.of(stack);
            }
            logMenuCompatibilityFailure(menu, null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            logMenuCompatibilityFailure(menu, e);
        }
        return Optional.empty();
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

    public record WarpStoneUse(ItemStack stack, InteractionHand hand) {
    }
}
