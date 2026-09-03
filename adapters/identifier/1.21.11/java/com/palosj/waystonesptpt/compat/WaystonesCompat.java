package com.palosj.waystonesptpt.compat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.palosj.waystonesptpt.WaystonesPTPT;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class WaystonesCompat {
    private static final AtomicBoolean MENU_COMPAT_FAILURE_LOGGED = new AtomicBoolean();
    public static final Identifier WARP_STONE_ID = Identifier.fromNamespaceAndPath("waystones", "warp_stone");
    private static final Identifier WARP_STONE_MENU_ID = Identifier.fromNamespaceAndPath(
            "waystones",
            "warp_stone_selection");

    private WaystonesCompat() {
    }

    public static boolean isWarpStone(ItemStack stack) {
        return !stack.isEmpty() && WARP_STONE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static void stopUsingWarpStone(Player player) {
        if (player.isUsingItem() && isWarpStone(player.getUseItem())) {
            player.stopUsingItem();
        }
    }

    public static boolean isWarpStoneMenu(AbstractContainerMenu menu) {
        return menu != null && WARP_STONE_MENU_ID.equals(BuiltInRegistries.MENU.getKey(menu.getType()));
    }

    public static Optional<WarpStoneUse> resolveWarpStoneUse(ServerPlayer player, AbstractContainerMenu menu) {
        if (!isWarpStoneMenu(menu)) {
            return Optional.empty();
        }
        if (!(menu instanceof WarpStoneUseCarrier carrier)) {
            logMenuCompatibilityFailure(menu);
            return Optional.empty();
        }

        ItemStack stack = carrier.waystonesptpt$getWarpStone();
        InteractionHand hand = carrier.waystonesptpt$getWarpStoneHand();
        if (stack != null && hand != null && isWarpStone(stack) && player.getItemInHand(hand) == stack) {
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

    private static void logMenuCompatibilityFailure(AbstractContainerMenu menu) {
        if (MENU_COMPAT_FAILURE_LOGGED.compareAndSet(false, true)) {
            WaystonesPTPT.LOGGER.warn(
                    "Waystones menu item binding is unavailable for {}; player destinations will be disabled.",
                    menu == null ? "null" : menu.getClass().getName());
        }
    }

    public record WarpStoneUse(ItemStack stack, InteractionHand hand) {
    }
}
