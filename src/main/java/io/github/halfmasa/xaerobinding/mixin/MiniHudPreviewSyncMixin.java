package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import fi.dy.masa.malilib.interfaces.IDataSyncer;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IDataSyncer.class, remap = false)
public interface MiniHudPreviewSyncMixin
{
    @Inject(method = {
            "handleEntityData(ILfi/dy/masa/malilib/util/data/tag/CompoundData;)Lnet/minecraft/world/entity/Entity;",
            "handleBlockEntityData(Lnet/minecraft/core/BlockPos;Lfi/dy/masa/malilib/util/data/tag/CompoundData;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
    }, at = @At("RETURN"))
    default void halfmasa$receivedPreviewData(CallbackInfoReturnable<?> ci)
    {
        if (this.getClass().getName().startsWith("fi.dy.masa.minihud.")) PreviewCache.receivedData();
    }
}
//#endif
