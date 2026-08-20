package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.MaLiLibConfigScrollAccess;

@Mixin(value = GuiListBase.class, remap = false)
public abstract class GuiListBaseScrollMixin
{
    @Inject(method = "reCreateListWidget", at = @At("HEAD"), remap = false)
    private void halfmasa_saveBeforeListRebuild(CallbackInfo ci)
    {
        if ((Object) this instanceof MaLiLibConfigScrollAccess access) access.halfmasa$saveConfigScroll();
    }

    @Inject(method = "reCreateListWidget", at = @At("RETURN"), remap = false)
    private void halfmasa_restoreAfterListRebuild(CallbackInfo ci)
    {
        if ((Object) this instanceof MaLiLibConfigScrollAccess access) access.halfmasa$restoreConfigScroll();
    }
}
