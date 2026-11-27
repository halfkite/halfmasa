//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=GuiStringListEdit.class,remap=false)
public interface RegistryPickerStringEditorAccessor {
    @Accessor("configGui") IConfigGui halfmasa$pickerConfigGui();
    @Accessor("dialogHandler") IDialogHandler halfmasa$pickerDialogHandler();
}
//#endif
