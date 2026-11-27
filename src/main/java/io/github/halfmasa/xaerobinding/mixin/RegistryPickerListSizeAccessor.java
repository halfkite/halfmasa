//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=WidgetListBase.class,remap=false)
public interface RegistryPickerListSizeAccessor {
    @Accessor("browserEntryWidth") int halfmasa$pickerWidth();
    @Accessor("browserEntryHeight") int halfmasa$pickerHeight();
}
//#endif
