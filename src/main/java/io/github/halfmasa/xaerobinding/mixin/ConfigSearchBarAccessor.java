//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WidgetSearchBar.class, remap = false)
public interface ConfigSearchBarAccessor
{
    @Accessor("searchBox") GuiTextFieldGeneric halfmasa$getSearchBox();
}
//#endif
