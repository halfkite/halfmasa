package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;

@Mixin(value = AbstractContainerScreen.class, priority = 1200)
public abstract class JeiItemHistoryInputMixin
{
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void halfmasa_clickJeiHistory(
            double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir)
    {
        if (ItemManagerHistoryOverlay.handleJeiClick(mouseX, mouseY, button))
        {
            cir.setReturnValue(true);
        }
    }
}
