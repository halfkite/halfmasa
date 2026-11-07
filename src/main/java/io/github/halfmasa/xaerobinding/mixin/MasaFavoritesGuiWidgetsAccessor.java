//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiBase.class, remap = false)
public interface MasaFavoritesGuiWidgetsAccessor
{
    @Accessor("widgets") List<WidgetBase> halfmasa$getWidgets();
}
//#endif
