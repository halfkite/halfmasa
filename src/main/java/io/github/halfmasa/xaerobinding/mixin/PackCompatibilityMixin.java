package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ResourcePackCompatibilityBypass;

@Mixin(PackCompatibility.class)
public abstract class PackCompatibilityMixin
{
    @Inject(method = "isCompatible", at = @At("HEAD"), cancellable = true)
    private void halfmasa$skipCompatibilityCheck(CallbackInfoReturnable<Boolean> callback)
    {
        if (ResourcePackCompatibilityBypass.isEnabled())
        {
            callback.setReturnValue(true);
        }
    }
}
