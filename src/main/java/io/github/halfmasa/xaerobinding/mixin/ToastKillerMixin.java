package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.2
import net.minecraft.client.gui.components.toasts.ToastManager;
//#else
//$$ import net.minecraft.client.gui.components.toasts.ToastComponent;
//#endif
import net.minecraft.client.gui.components.toasts.Toast;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

//#if MC >= 1.21.2
@Mixin(ToastManager.class)
//#else
//$$ @Mixin(ToastComponent.class)
//#endif
public abstract class ToastKillerMixin
{
    @Shadow public abstract void clear();

    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void halfmasa_blockNewToast(Toast toast, CallbackInfo ci)
    {
        if (Configs.TOAST_KILLER.getBooleanValue())
        {
            ci.cancel();
        }
    }

    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void halfmasa_hideToasts(GuiGraphicsExtractor graphics, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa_hideToasts(GuiGraphics graphics, CallbackInfo ci)
    //#endif
    {
        if (Configs.TOAST_KILLER.getBooleanValue())
        {
            this.clear();
            ci.cancel();
        }
    }
}
