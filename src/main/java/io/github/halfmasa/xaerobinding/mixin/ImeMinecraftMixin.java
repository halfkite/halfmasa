package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeService;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

@Mixin(Minecraft.class)
public abstract class ImeMinecraftMixin
{
    //#if MC < 26.2
    //$$ @Shadow public Screen screen;
    //#endif

    //#if MC >= 26.2
    @Inject(method = "setScreenAndShow", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "setScreen", at = @At("HEAD"))
    //#endif
    private void halfmasa_resetImeOnScreenChange(Screen screen, CallbackInfo ci)
    {
        //#if MC >= 26.2
        if (MinecraftClientCompat.getScreen((Minecraft) (Object) this) != screen)
        //#else
        //$$ if (this.screen != screen)
        //#endif
        {
            ImeService.getInstance().onScreenChanged();
        }
    }
}
