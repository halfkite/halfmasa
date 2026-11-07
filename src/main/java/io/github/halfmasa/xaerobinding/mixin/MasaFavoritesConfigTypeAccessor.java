//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigTypeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ConfigTypeWrapper.class, remap = false)
public interface MasaFavoritesConfigTypeAccessor
{
    @Accessor("wrappedConfig") IConfigBase halfmasa$getWrappedConfig();
}
//#endif
