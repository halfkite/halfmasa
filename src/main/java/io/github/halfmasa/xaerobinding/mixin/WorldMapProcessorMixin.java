package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.multiplayer.ClientPacketListener;
import xaero.map.MapProcessor;

import io.github.halfmasa.xaerobinding.binding.WorldBindingStore;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class WorldMapProcessorMixin
{
    @Inject(
            method = {
                    "getMainId(ILnet/minecraft/client/multiplayer/ClientPacketListener;)Ljava/lang/String;",
                    "getMainId(ILnet/minecraft/class_634;)Ljava/lang/String;"
            },
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false)
    private void xaeroWorldBinding$bindWorldMapRoot(
            int mode,
            ClientPacketListener connection,
            CallbackInfoReturnable<String> cir)
    {
        if (mode != 5)
        {
            return;
        }

        cir.setReturnValue(WorldBindingStore.resolveWorldMapRoot(cir.getReturnValue()));
    }
}
