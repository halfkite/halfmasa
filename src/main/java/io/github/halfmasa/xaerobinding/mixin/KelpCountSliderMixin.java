//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.gui.SliderCallbackInteger;
import fi.dy.masa.malilib.gui.interfaces.ISliderCallback;
import fi.dy.masa.malilib.gui.widgets.WidgetSlider;
import io.github.halfmasa.xaerobinding.config.Configs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WidgetSlider.class, remap = false)
public abstract class KelpCountSliderMixin
{
    @Shadow protected int sliderWidth;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void halfmasa$useNormalWidth(int x, int y, int width, int height, ISliderCallback callback, CallbackInfo ci)
    {
        if (callback instanceof IntegerCallbackAccessor integer
                && integer.halfmasa$getConfig() == Configs.AUTO_KELP_BLOCKS_PER_TICK)
        {
            // Match MaLiLib's minimum handle width without changing its four value steps.
            this.sliderWidth = Math.min(this.sliderWidth, 8);
        }
    }

    @Mixin(value = SliderCallbackInteger.class, remap = false)
    public interface IntegerCallbackAccessor
    {
        @Accessor("config") IConfigInteger halfmasa$getConfig();
    }
}
//#endif
