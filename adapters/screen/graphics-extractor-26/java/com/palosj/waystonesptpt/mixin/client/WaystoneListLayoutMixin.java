package com.palosj.waystonesptpt.mixin.client;

import com.palosj.waystonesptpt.client.WaystonePlayerScreenInjector;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase", remap = false)
public abstract class WaystoneListLayoutMixin {
    @Inject(method = "updateList", at = @At("TAIL"), remap = false)
    private void waystonesptpt$realignRebuiltRows(CallbackInfo ci) {
        WaystonePlayerScreenInjector.onWaystonesListUpdated((Screen) (Object) this);
    }
}
