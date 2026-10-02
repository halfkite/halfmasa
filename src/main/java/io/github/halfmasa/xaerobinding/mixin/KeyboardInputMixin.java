package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.gui.KeybindPieScreen;

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
        boolean pieScreen = currentScreen instanceof KeybindPieScreen;
        boolean containerMove = Configs.INVENTORY_MOVE.getBooleanValue()
                && currentScreen instanceof AbstractContainerScreen<?>;
        if (!pieScreen && !containerMove)
        {
            return keyMapping.isDown();
        }

        InputConstants.Key key = ((KeyMappingAccessor) keyMapping).halfmasa$getBoundKey();
        if (key.getType() == InputConstants.Type.MOUSE)
        {
            return InputCompat.isMouseButtonDown(minecraft, key.getValue());
        }
        return InputCompat.isKeyDown(minecraft, key.getValue());
    }
}
