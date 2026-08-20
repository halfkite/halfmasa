package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

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
}
