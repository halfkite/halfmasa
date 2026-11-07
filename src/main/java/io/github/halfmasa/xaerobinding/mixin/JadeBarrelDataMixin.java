//#if MC >= 26.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.jade.JadeBarrelPreview;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "snownee.jade.overlay.WailaTickHandler", remap = false)
public abstract class JadeBarrelDataMixin
{
    @Inject(method = "setData", at = @At("HEAD"), cancellable = true)
    private void halfmasa$routeReply(CompoundTag data, CallbackInfo ci)
    {
        if (JadeBarrelPreview.receive(data)) ci.cancel();
    }

    @Inject(method = "tickClient", at = @At("HEAD"))
    private void halfmasa$validateContext(CallbackInfo ci)
    {
        JadeBarrelPreview.tick();
    }
}
//#endif
