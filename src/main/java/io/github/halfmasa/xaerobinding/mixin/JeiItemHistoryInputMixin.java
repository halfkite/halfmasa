package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;

@Mixin(value = AbstractContainerScreen.class, priority = 1200)
public abstract class JeiItemHistoryInputMixin
{
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    //#if MC >= 1.21.10
    private void halfmasa_clickJeiHistory(
            MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir)
    {
        if (ItemManagerHistoryOverlay.handleJeiClick(event.x(), event.y(), event.button()))
    //#else
    //$$ private void halfmasa_clickJeiHistory(
    //$$         double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir)
    //$$ {
    //$$     if (ItemManagerHistoryOverlay.handleJeiClick(mouseX, mouseY, button))
    //#endif
        {
            cir.setReturnValue(true);
        }
    }
}
