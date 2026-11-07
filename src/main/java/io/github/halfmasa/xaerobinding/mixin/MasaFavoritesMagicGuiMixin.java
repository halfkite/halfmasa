//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import io.github.halfmasa.xaerobinding.favorites.MasaSwitcherAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MagicLib redraws its own dropdowns after MaLiLib's normal widget pass. */
@Pseudo
@Mixin(targets = "top.hendrixshen.magiclib.impl.malilib.config.gui.MagicConfigGui", remap = false)
public abstract class MasaFavoritesMagicGuiMixin
{
    @Inject(method = "renderDropDownList", at = @At("TAIL"))
    //#if MC >= 1.21.11
    private void halfmasa$renderSwitcherLast(GuiContext context, int mouseX, int mouseY, CallbackInfo ci)
    //#else
    //$$ private void halfmasa$renderSwitcherLast(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci)
    //#endif
    {
        ((MasaSwitcherAccess) this).halfmasa$renderSwitcherOverlay(context, mouseX, mouseY);
    }
}
//#endif
