package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeService;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.widget.basewidgets.TextFieldWidget", remap = false)
public abstract class ImeReiTextFieldMixin
{
    @Dynamic
    @Inject(method = {"setFocused", "method_25365"}, at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_trackReiImeFocus(boolean focused, CallbackInfo ci)
    {
        ImeService.getInstance().onGenericEditFocus(this, focused);
    }
}
