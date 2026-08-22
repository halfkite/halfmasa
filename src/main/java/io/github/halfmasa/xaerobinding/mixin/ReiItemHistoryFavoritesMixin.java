//#if MC < 26.1
package io.github.halfmasa.xaerobinding.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.widget.favorites.FavoritesListWidget", remap = false)
public abstract class ReiItemHistoryFavoritesMixin
{
    @Dynamic
    @Inject(
            method = {"render", "method_25394"},
            at = @At(
                    value = "FIELD",
                    target = "Lme/shedaniel/rei/impl/client/gui/widget/favorites/FavoritesListWidget;favoritesBounds:Lme/shedaniel/math/Rectangle;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 0,
            remap = false)
    private void halfmasa_reserveReiFavorites(
            GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.reserveReiFavoritesArea(this);
    }
}
//#endif
