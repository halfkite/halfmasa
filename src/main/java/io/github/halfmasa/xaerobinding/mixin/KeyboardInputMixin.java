package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.KeyboardInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin
{
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean halfmasa$readMovementKeysInContainer(KeyMapping keyMapping)
    {
        Minecraft minecraft = Minecraft.getInstance();
        //#if MC >= 26.2
        var currentScreen = minecraft.gui.screen();
        //#else
        //$$ var currentScreen = minecraft.screen;
        //#endif
        if (!Configs.INVENTORY_MOVE.getBooleanValue()
                || !(currentScreen instanceof AbstractContainerScreen<?>))
        {
            return keyMapping.isDown();
        }

        InputConstants.Key key = ((KeyMappingAccessor) keyMapping).halfmasa$getBoundKey();
        //#if MC >= 1.21.10
        var window = minecraft.getWindow();
        long windowHandle = window.handle();
        //#else
        //$$ long windowHandle = minecraft.getWindow().getWindow();
        //#endif
        if (key.getType() == InputConstants.Type.MOUSE)
        {
            return GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        //#if MC >= 1.21.10
        return InputConstants.isKeyDown(window, key.getValue());
        //#else
        //$$ return InputConstants.isKeyDown(windowHandle, key.getValue());
        //#endif
    }
}
