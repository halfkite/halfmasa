//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiListBase;
import io.github.halfmasa.xaerobinding.feature.MaLiLibConfigScrollAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiListBase.class, remap = false)
public abstract class MaLiLibConfigListRecreationMixin
{
    @Inject(method = "reCreateListWidget", at = @At("HEAD"))
    private void halfmasa$saveBeforeRecreation(CallbackInfo ci)
    {
        if ((Object) this instanceof MaLiLibConfigScrollAccess access)
            access.halfmasa$beforeConfigCategoryChange();
    }
}
//#endif
