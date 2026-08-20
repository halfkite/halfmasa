package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;

import io.github.halfmasa.xaerobinding.binding.WorldBindingStore;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class WorldMapProcessorMixin
{
    @Inject(
            method = "getMainId",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void xaeroWorldBinding$bindWorldMapRoot(CallbackInfoReturnable<String> cir)
    {
        cir.setReturnValue(WorldBindingStore.resolveWorldMapRoot(cir.getReturnValue()));
    }
}
