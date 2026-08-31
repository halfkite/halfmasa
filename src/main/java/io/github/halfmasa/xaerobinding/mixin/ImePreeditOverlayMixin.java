package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.PreeditEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.Ime261Compat;

@Mixin(IMEPreeditOverlay.class)
public abstract class ImePreeditOverlayMixin
{
    @Inject(
            method = "<init>(Lnet/minecraft/client/input/PreeditEvent;Lnet/minecraft/client/gui/Font;I)V",
            at = @At("TAIL"))
    private void halfmasa_capturePreedit(PreeditEvent event, Font font, int offset, CallbackInfo ci)
    {
        Ime261Compat.onPreedit(event.fullText(), event.caretPosition(), event.blocks(), event.focusedBlock());
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void halfmasa_replacePreeditRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float tickDelta, CallbackInfo ci)
    {
        if (Ime261Compat.cancelVanillaPreeditRender())
        {
            ci.cancel();
        }
    }
}
//#endif
