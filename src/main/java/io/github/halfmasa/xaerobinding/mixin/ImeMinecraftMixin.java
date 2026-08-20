package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeService;

@Mixin(Minecraft.class)
public abstract class ImeMinecraftMixin
{
    @Shadow public Screen screen;

    //#if MC >= 26.2
    @Inject(method = "setScreenAndShow", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "setScreen", at = @At("HEAD"))
    //#endif
    private void halfmasa_resetImeOnScreenChange(Screen screen, CallbackInfo ci)
    {
        if (this.screen != screen)
        {
            ImeService.getInstance().onScreenChanged();
        }
    }
}
