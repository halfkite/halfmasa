package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import io.github.halfmasa.xaerobinding.feature.FastScrolling;

@Mixin(value = WidgetListBase.class, remap = false)
public abstract class WidgetListBaseFastScrollingMixin
{
    @ModifyArg(
            method = "onMouseScrolled",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/widgets/WidgetListBase;offsetSelectionOrScrollbar(IZ)V"),
            index = 0,
            remap = false)
    private int halfmasa$accelerateMaLiLibScrolling(int amount)
    {
        return amount * FastScrolling.getMultiplier();
    }
}
