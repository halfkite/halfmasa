package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.event.InputEventHandler;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;

@Mixin(value = InputEventHandler.class, remap = false)
public abstract class MaLiLibKeybindPieMixin
{
    @Inject(method = "onMouseClick", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/malilib/event/InputEventHandler;checkKeyBindsForChanges(I)Z"))
    //#if MC >= 1.21.10
    private void halfmasa_trackMasaMouse(MouseButtonEvent event, int action,
    //#else
    //$$ private void halfmasa_trackMasaMouse(int mouseX, int mouseY, int button, int action, net.minecraft.client.Minecraft client,
    //#endif
            CallbackInfoReturnable<Boolean> cir)
    {
        if (action == InputConstants.PRESS || action == InputConstants.RELEASE)
        {
            KeybindPieManager.getInstance().handleMasaMouseInput(
                    //#if MC >= 1.21.10
                    InputConstants.Type.MOUSE.getOrCreate(event.button()),
                    //#else
                    //$$ InputConstants.Type.MOUSE.getOrCreate(button),
                    //#endif
                    action == InputConstants.PRESS);
        }
    }

    @Inject(method = "checkKeyBindsForChanges", at = @At("HEAD"), cancellable = true)
    private void halfmasa_blockMasaHotkeysDuringSelection(int inputCode,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (KeybindPieManager.getInstance().blockMasaDispatch(inputCode))
        {
            // Suppress MaLiLib hotkey callbacks while the selector is open,
            // but let the release event reach Screen.keyReleased/mouseReleased.
            cir.setReturnValue(false);
        }
    }
}
//#endif
