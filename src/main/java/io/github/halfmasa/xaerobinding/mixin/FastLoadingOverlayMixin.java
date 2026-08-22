package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.LoadingOverlay;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 26.1
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

@Mixin(LoadingOverlay.class)
public abstract class FastLoadingOverlayMixin
{
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private boolean fadeIn;
    @Shadow private long fadeOutStart;

    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void halfmasa_finishResourceReload(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("TAIL"))
    //$$ private void halfmasa_finishResourceReload(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        if (Configs.FAST_RESOURCE_PACK_LOADING_SCREEN.getBooleanValue() && this.fadeOutStart != -1L)
        {
            //#if MC >= 26.1
            MinecraftClientCompat.clearOverlay(this.minecraft);
            //#else
            //$$ this.minecraft.setOverlay(null);
            //#endif
        }
    }

    //#if MC >= 26.1
    @Redirect(
            method = "extractRenderState",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fadeIn:Z", opcode = Opcodes.GETFIELD),
            require = 0)
    //#else
    //$$ @Redirect(
    //$$         method = "render",
    //$$         at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fadeIn:Z", opcode = Opcodes.GETFIELD),
    //$$         require = 0)
    //#endif
    private boolean halfmasa_skipFadeIn(LoadingOverlay instance)
    {
        return Configs.FAST_RESOURCE_PACK_LOADING_SCREEN.getBooleanValue() ? false : this.fadeIn;
    }

    //#if MC >= 26.1
    @Inject(method = "isReadyToFadeOut", at = @At("RETURN"), cancellable = true)
    private void halfmasa_skipFadeWait(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.FAST_RESOURCE_PACK_LOADING_SCREEN.getBooleanValue())
        {
            cir.setReturnValue(true);
        }
    }
    //#endif
}
