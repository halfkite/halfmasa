package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryCompat;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.ClientHelperImpl", remap = false)
public abstract class ReiItemHistoryCaptureMixin
{
    @Dynamic
    @Inject(method = "tryCheatingEntry", at = @At("RETURN"), require = 0, remap = false)
    private void halfmasa_recordReiGive(
            @Coerce Object entry, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ()) ItemManagerHistoryCompat.recordReiEntry(entry);
    }

    @Dynamic
    @Inject(method = "tryCheatingEntryTo", at = @At("RETURN"), require = 0, remap = false)
    private void halfmasa_recordReiHotbarGive(
            @Coerce Object entry, int slot, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ()) ItemManagerHistoryCompat.recordReiEntry(entry);
    }

    @Dynamic
    @Inject(method = "openView", at = @At("RETURN"), require = 0, remap = false)
    private void halfmasa_recordReiView(
            @Coerce Object builder, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ()) ItemManagerHistoryCompat.recordReiView(builder);
    }
}
