//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
import io.github.halfmasa.xaerobinding.picker.PickerPolicy;
import io.github.halfmasa.xaerobinding.picker.RegistryListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=WidgetListStringListEdit.class,remap=false)
public abstract class RegistryPickerListMixin {
    // Capture the real editor at construction: currentScreen need not be updated during init.
    @org.spongepowered.asm.mixin.Unique private GuiStringListEdit halfmasa$editor;
    @Inject(method="<init>",at=@At("RETURN"))
    private void halfmasa$rememberEditor(int x,int y,int width,int height,int configWidth,GuiStringListEdit editor,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){halfmasa$editor=editor;}
    @Inject(method="createListEntryWidget(IIIZLjava/lang/String;)Lfi/dy/masa/malilib/gui/widgets/WidgetStringListEditEntry;",at=@At("HEAD"),cancellable=true,require=0)
    private void halfmasa$replaceRow(int x,int y,int index,boolean odd,String entry,CallbackInfoReturnable<WidgetStringListEditEntry> cir){
        if(!PickerPolicy.enabled()||halfmasa$editor==null)return;
        var editor=(RegistryPickerStringEditorAccessor)halfmasa$editor;
        if(editor.halfmasa$pickerDialogHandler()!=null)return;
        String mod=editor.halfmasa$pickerConfigGui().getModId();
        var self=(WidgetListStringListEdit)(Object)this;
        var config=self.getConfig();var kinds=PickerPolicy.kinds(mod,config);
        boolean emptyRow = index == -1 && config.getStrings().isEmpty();
        if (kinds.isEmpty() || (!emptyRow && (index < 0 || index >= config.getStrings().size()))) return;
        var size=(RegistryPickerListSizeAccessor)(Object)this;
        if (!emptyRow && size.halfmasa$pickerWidth() < 220) return;
        var defaults=config.getDefaultStrings();
        var row = new RegistryListEntry(x, y, size.halfmasa$pickerWidth(), size.halfmasa$pickerHeight(), index, odd,
                entry, index >= 0 && index < defaults.size() ? defaults.get(index) : "", self, halfmasa$editor, kinds);
        io.github.halfmasa.xaerobinding.picker.PickerScrollMemory.restoreRow(halfmasa$editor, row);
        cir.setReturnValue(row);
    }
}
//#endif
