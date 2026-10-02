package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#endif

//#if MC < 26.3
//$$ import org.lwjgl.glfw.GLFW;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerComboMixin
{
    @Inject(method = "keyPress", at = @At("HEAD"))
    //#if MC >= 1.21.10
    private void halfmasa_trackCombinationInput(long window, int action, KeyEvent event, CallbackInfo ci)
    //#else
    //$$ private void halfmasa_trackCombinationInput(long window, int action, int keyCode, int scanCode, int modifiers, CallbackInfo ci)
    //#endif
    {
        //#if MC < 26.3
        //$$ if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)
        //#else
        if (action == InputConstants.PRESS || action == InputConstants.RELEASE)
        //#endif
        {
            //#if MC >= 1.21.10
            KeybindPieManager.getInstance().handleKeyboardEvent(
                    InputConstants.getKey(event),
                    //#if MC < 26.3
                    //$$ action == GLFW.GLFW_PRESS);
                    //#else
                    action == InputConstants.PRESS);
                    //#endif
            //#else
            //$$ KeybindPieManager.getInstance().handleKeyboardEvent(
            //$$         InputConstants.getKey(keyCode, scanCode), action == GLFW.GLFW_PRESS);
            //#endif
        }
    }
}
