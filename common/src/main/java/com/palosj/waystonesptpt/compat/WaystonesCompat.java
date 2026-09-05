package com.palosj.waystonesptpt.compat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesptpt.WaystonesPTPT;

import net.blay09.mods.waystones.item.WarpStoneItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class WaystonesCompat {
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
        return stack.getItem() instanceof WarpStoneItem;
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
        return WARP_STONE_MENU_ID.equals(BuiltInRegistries.MENU.getKey(menu.getType()));
    }

    public static Optional<WarpStoneUse> resolveWarpStoneUse(ServerPlayer player, AbstractContainerMenu menu) {
        if (!isWarpStoneMenu(menu)) {
            return Optional.empty();
        }
        if (!(menu instanceof WarpStoneUseCarrier carrier)) {
            logMenuCompatibilityFailure(menu, null);
            return Optional.empty();
        }

        ItemStack stack = carrier.waystonesptpt$getWarpStone();
        InteractionHand hand = carrier.waystonesptpt$getWarpStoneHand();
        if (stack != null
                && hand != null
                && isWarpStone(stack)
                && player.getItemInHand(hand) == stack) {
            return Optional.of(new WarpStoneUse(stack, hand));
        }
        return Optional.empty();
    }

    public static boolean isWarpStoneUseBound(ServerPlayer player, WarpStoneUse use) {
        return use != null
                && isWarpStone(use.stack())
                && player.getItemInHand(use.hand()) == use.stack();
    }

    public static Optional<ItemStack> resolveDurabilityTarget(ServerPlayer player, WarpStoneUse use) {
        ItemStack current = player.getItemInHand(use.hand());
        if (current == use.stack()
                || isWarpStone(current) && ItemStack.isSameItemSameComponents(current, use.stack())) {
            return Optional.of(current);
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate == use.stack()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static void logMenuCompatibilityFailure(AbstractContainerMenu menu, Throwable error) {
        if (!MENU_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            return;
        }

        String message = "Waystones menu item binding is unavailable for {}; player destinations will be disabled.";
        String menuClass = menu == null ? "null" : menu.getClass().getName();
        if (error == null) {
            WaystonesPTPT.LOGGER.warn(message, menuClass);
        } else {
            WaystonesPTPT.LOGGER.warn(message, menuClass, error);
        }
    }

    public record WarpStoneUse(ItemStack stack, InteractionHand hand) {
    }
}
