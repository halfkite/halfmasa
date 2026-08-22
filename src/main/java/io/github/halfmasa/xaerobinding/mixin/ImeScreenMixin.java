package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeOverlay;

@Mixin(Screen.class)
public abstract class ImeScreenMixin
{
    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void halfmasa_renderImeOverlay(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("RETURN"))
    //$$ private void halfmasa_renderImeOverlay(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        ImeOverlay.render(graphics);
    }
}
