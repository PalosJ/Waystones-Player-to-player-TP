package com.palosj.waystonesplayer.mixin;

import com.palosj.waystonesplayer.compat.WarpStoneUseCarrier;

import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WaystoneSelectionMenu.class)
public abstract class WaystoneSelectionMenuMixin implements WarpStoneUseCarrier {
    @Unique
    private ItemStack waystonesplayer$warpStone = ItemStack.EMPTY;
    @Unique
    private InteractionHand waystonesplayer$warpStoneHand;

    @Override
    public void waystonesplayer$bindWarpStone(ItemStack stack, InteractionHand hand) {
        waystonesplayer$warpStone = stack;
        waystonesplayer$warpStoneHand = hand;
    }

    @Override
    public ItemStack waystonesplayer$getWarpStone() {
        return waystonesplayer$warpStone;
    }

    @Override
    public InteractionHand waystonesplayer$getWarpStoneHand() {
        return waystonesplayer$warpStoneHand;
    }
}
