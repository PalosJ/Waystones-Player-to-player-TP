package com.palosj.waystonesptpt.mixin;

import com.palosj.waystonesptpt.compat.WarpStoneUseCarrier;

import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WaystoneSelectionMenu.class)
public abstract class WaystoneSelectionMenuMixin implements WarpStoneUseCarrier {
    @Unique
    private ItemStack waystonesptpt$warpStone = ItemStack.EMPTY;
    @Unique
    private InteractionHand waystonesptpt$warpStoneHand;

    @Override
    public void waystonesptpt$bindWarpStone(ItemStack stack, InteractionHand hand) {
        waystonesptpt$warpStone = stack;
        waystonesptpt$warpStoneHand = hand;
    }

    @Override
    public ItemStack waystonesptpt$getWarpStone() {
        return waystonesptpt$warpStone;
    }

    @Override
    public InteractionHand waystonesptpt$getWarpStoneHand() {
        return waystonesptpt$warpStoneHand;
    }
}
