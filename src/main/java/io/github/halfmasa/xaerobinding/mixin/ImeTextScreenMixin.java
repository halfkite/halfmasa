package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeService;

@Mixin({BookEditScreen.class, AbstractSignEditScreen.class})
public abstract class ImeTextScreenMixin
{
    @Inject(method = "init", at = @At("TAIL"))
    private void halfmasa_openScreenIme(CallbackInfo ci)
    {
        ImeService.getInstance().onGenericEditFocus(this, true);
    }
}
