package io.github.halfmasa.xaerobinding.mixin;

//#if MC < 1.21.10
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 1.21.4
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(ItemEntityRenderer.class)
public abstract class LegacyItemAggregationMixin
{
    //#if MC >= 1.21.4
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void halfmasa$singleItemModel(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci)
    {
        if (Configs.ITEM_RENDER_AGGREGATION.getBooleanValue()) state.count = 1;
    }
    //#else
    //$$ @Inject(method = "getRenderedAmount", at = @At("HEAD"), cancellable = true)
    //$$ private static void halfmasa$singleItemModel(int count, CallbackInfoReturnable<Integer> cir)
    //$$ {
    //$$     if (Configs.ITEM_RENDER_AGGREGATION.getBooleanValue()) cir.setReturnValue(1);
    //$$ }
    //#endif
}
//#endif
