package com.palosj.waystonesptpt.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int waystonesptpt$getLeftPos();

    @Accessor("leftPos")
    void waystonesptpt$setLeftPos(int leftPos);

    @Accessor("imageWidth")
    int waystonesptpt$getImageWidth();

    @Accessor("imageWidth")
    void waystonesptpt$setImageWidth(int imageWidth);
}
