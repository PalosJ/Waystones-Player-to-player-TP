package com.palosj.waystonesplayer.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int waystonesplayer$getLeftPos();

    @Accessor("leftPos")
    void waystonesplayer$setLeftPos(int leftPos);

    @Accessor("imageWidth")
    int waystonesplayer$getImageWidth();

    @Accessor("imageWidth")
    void waystonesplayer$setImageWidth(int imageWidth);
}
