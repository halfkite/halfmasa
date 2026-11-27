//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.interfaces.IStringListConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=GuiStringListSelection.class,remap=false)
public interface RegistryPickerSelectionAccessor {
    @Accessor("consumer") IStringListConsumer halfmasa$pickerConsumer();
}
//#endif
