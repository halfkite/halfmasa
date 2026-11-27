package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.util.List;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WidgetListBase.class, remap = false)
public interface RegistryPickerVisibleRowsAccessor
{
    @Accessor("listWidgets") List<? extends WidgetListEntryBase<?>> halfmasa$pickerRows();
    @Accessor("lastScrollbarPosition") void halfmasa$setPickerScroll(int value);
    @Invoker("reCreateListEntryWidgets") void halfmasa$rebuildPickerRows();
}
//#endif
