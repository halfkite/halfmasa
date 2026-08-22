package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.LevelLoadingScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(LevelLoadingScreen.class)
public abstract class FastLevelLoadingScreenMixin
{
    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void halfmasa_closeWorldLoadingScreen(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa_closeWorldLoadingScreen(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        if (!Configs.FAST_WORLD_LOADING_SCREEN.getBooleanValue())
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null)
        {
            //#if MC >= 26.1
            ci.cancel();
            //#else
            //$$ client.setScreen(null);
            //$$ ci.cancel();
            //#endif
        }
    }
}
