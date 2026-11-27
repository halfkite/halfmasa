//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import io.github.halfmasa.xaerobinding.picker.PickerScrollMemory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
@Mixin(value=GuiStringListEdit.class,remap=false)
public abstract class RegistryPickerScrollMixin {
    @Inject(method="initGui",at=@At("RETURN"),require=0)
    private void halfmasa$restorePickerScroll(CallbackInfo ci){PickerScrollMemory.restore((GuiStringListEdit)(Object)this);}

    @Inject(method = {"removed", "method_25432"}, at = @At("HEAD"), cancellable = true)
    private void halfmasa$keepPendingText(CallbackInfo ci)
    {
        if (PickerScrollMemory.suspended((GuiStringListEdit) (Object) this)) ci.cancel();
    }

    @WrapOperation(method = {"removed", "method_25432"}, at = @At(value = "INVOKE", target =
            "Lfi/dy/masa/malilib/gui/widgets/WidgetListStringListEdit;wereConfigsModified()Z"))
    private boolean halfmasa$includeHiddenDrafts(WidgetListStringListEdit list, Operation<Boolean> original)
    {
        boolean hadHiddenDrafts = PickerScrollMemory.applyHidden((GuiStringListEdit) (Object) this, list.getConfig());
        return original.call(list) || hadHiddenDrafts;
    }
}
//#endif
