package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;

@Mixin(KeyMapping.class)
public abstract class KeybindPieKeyMappingMixin
{
    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void halfmasa_selectConflictingMapping(
            InputConstants.Key key, boolean pressed, CallbackInfo ci)
    {
        if (KeybindPieManager.getInstance().handleSet(key, pressed))
        {
            ci.cancel();
        }
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void halfmasa_blockConflictingClick(InputConstants.Key key, CallbackInfo ci)
    {
        if (KeybindPieManager.getInstance().handleClick(key))
        {
            ci.cancel();
        }
    }

    @Inject(method = "setAll", at = @At("TAIL"))
    private static void halfmasa_refreshCustomCombinations(CallbackInfo ci)
    {
        KeybindPieManager.getInstance().refreshCustomCombos();
    }

    @Inject(method = "releaseAll", at = @At("TAIL"))
    private static void halfmasa_restoreMovementAfterScreenChange(CallbackInfo ci)
    {
        KeybindPieManager.getInstance().restoreMovementKeys(Minecraft.getInstance());
    }
}
