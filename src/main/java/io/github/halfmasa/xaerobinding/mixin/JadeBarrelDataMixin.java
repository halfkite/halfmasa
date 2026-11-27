//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.jade.JadeBarrelPreview;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
//#if MC >= 1.21.10
@Mixin(targets = "snownee.jade.overlay.WailaTickHandler", remap = false)
//#else
//$$ @Mixin(targets = "snownee.jade.impl.ObjectDataCenter", remap = false)
//#endif
public abstract class JadeBarrelDataMixin
{
    //#if MC >= 1.21.10
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
    //#else
    //$$ @Inject(method = "setServerData", at = @At("HEAD"), cancellable = true)
    //$$ private static void halfmasa$routeLegacyReply(CompoundTag data, CallbackInfo ci)
    //$$ {
    //$$     if (JadeBarrelPreview.receive(data)) ci.cancel();
    //$$ }
    //#endif
}
//#endif
