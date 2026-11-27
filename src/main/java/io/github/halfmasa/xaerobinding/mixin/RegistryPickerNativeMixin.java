//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import io.github.halfmasa.xaerobinding.picker.*;
import java.util.EnumSet;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(value=GuiBase.class,remap=false)
public abstract class RegistryPickerNativeMixin {
    @ModifyVariable(method="openGui",at=@At("HEAD"),argsOnly=true,require=0)
    private static Screen halfmasa$replaceNativePicker(Screen screen){
        if (!PickerPolicy.enabled()) return screen;
        if(screen==null||screen.getClass()!=GuiStringListSelection.class)return screen;
        var original=(GuiStringListSelection)screen;
        var candidates=PickerPolicy.registryCandidates(original.getStrings());
        if(candidates.isEmpty())return screen;
        var consumer=((RegistryPickerSelectionAccessor)original).halfmasa$pickerConsumer();
        return new RegistryPickerScreen(original.getParent(),EnumSet.allOf(RegistryIndex.Kind.class),e->candidates.containsKey(e.id()),null,selected->consumer.consume(selected));
    }
}
//#endif
