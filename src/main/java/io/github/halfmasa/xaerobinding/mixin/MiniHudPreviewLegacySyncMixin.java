package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import fi.dy.masa.malilib.interfaces.IDataSyncer;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Desc(id = "handleEntityData", value = "handleEntityData", args = {int.class, net.minecraft.nbt.CompoundTag.class}, ret = net.minecraft.world.entity.Entity.class)
@Desc(id = "handleBlockEntityData", value = "handleBlockEntityData", args = {net.minecraft.core.BlockPos.class, net.minecraft.nbt.CompoundTag.class, net.minecraft.resources.ResourceLocation.class}, ret = net.minecraft.world.level.block.entity.BlockEntity.class)
@Mixin(value = IDataSyncer.class, remap = false)
public interface MiniHudPreviewLegacySyncMixin
{
    @Inject(method = {
            "@Desc(handleEntityData)",
            "@Desc(handleBlockEntityData)"
    }, at = @At("RETURN"))
    default void halfmasa$receivedPreviewData(CallbackInfoReturnable<?> ci)
    {
        if (this.getClass().getName().startsWith("fi.dy.masa.minihud.")) PreviewCache.receivedData();
    }
}
//#endif
