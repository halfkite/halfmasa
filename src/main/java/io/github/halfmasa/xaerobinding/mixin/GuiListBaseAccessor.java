package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiListBase.class, remap = false)
public interface GuiListBaseAccessor
{
    @Accessor(value = "widget", remap = false)
    WidgetListBase<?, ?> halfmasa$getListWidget();
}
