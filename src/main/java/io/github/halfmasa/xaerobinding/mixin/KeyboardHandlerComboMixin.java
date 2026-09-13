package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#endif

import org.lwjgl.glfw.GLFW;
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
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)
        {
            //#if MC >= 1.21.10
            KeybindPieManager.getInstance().handleKeyboardEvent(
                    InputConstants.getKey(event), action == GLFW.GLFW_PRESS);
            //#else
            //$$ KeybindPieManager.getInstance().handleKeyboardEvent(
            //$$         InputConstants.getKey(keyCode, scanCode), action == GLFW.GLFW_PRESS);
            //#endif
        }
    }
}
