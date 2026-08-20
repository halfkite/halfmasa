package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public abstract class ReiItemHistoryOverlayMixin
{
    @Dynamic
    @Inject(method = "init", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_initializeReiHistory(CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.initializeReiOverlay(this);
    }
}
