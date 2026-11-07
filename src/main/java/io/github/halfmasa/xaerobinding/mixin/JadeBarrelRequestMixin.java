//#if MC >= 26.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import io.github.halfmasa.xaerobinding.feature.jade.JadeBarrelPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

@Pseudo
@Mixin(targets = "snownee.jade.impl.BlockAccessorClientHandler", remap = false)
public abstract class JadeBarrelRequestMixin
{
    @Inject(method = "requestData(Lsnownee/jade/api/BlockAccessor;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void halfmasa$requestHalves(BlockAccessor accessor, List<IServerDataProvider<BlockAccessor>> providers, CallbackInfo ci)
    {
        if (JadeBarrelPreview.request(accessor, providers)) ci.cancel();
    }
}
//#endif
