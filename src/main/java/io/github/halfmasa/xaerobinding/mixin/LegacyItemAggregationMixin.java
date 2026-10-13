package io.github.halfmasa.xaerobinding.mixin;

//#if MC < 1.21.10
import io.github.halfmasa.xaerobinding.feature.EntityRenderAggregation;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 1.21.4
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import net.minecraft.client.renderer.entity.ItemRenderer;
//$$ import net.minecraft.client.resources.model.BakedModel;
//$$ import net.minecraft.util.RandomSource;
//$$ import net.minecraft.world.item.ItemStack;
//#endif

@Mixin(ItemEntityRenderer.class)
public abstract class LegacyItemAggregationMixin
{
    //#if MC >= 1.21.4
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void halfmasa$singleItemModel(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci)
    {
        if (EntityRenderAggregation.isItemAggregationEnabled(entity)) state.count = 1;
    }
    //#else
    //$$ @ModifyVariable(method = "renderMultipleFromCount(Lnet/minecraft/client/renderer/entity/ItemRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/BakedModel;ZLnet/minecraft/util/RandomSource;)V", at = @At("STORE"), ordinal = 1)
    //$$ private static int halfmasa$singleItemModel(int models, ItemRenderer renderer, PoseStack pose,
    //$$         MultiBufferSource buffers, int light, ItemStack stack, BakedModel model,
    //$$         boolean gui3d, RandomSource random)
    //$$ {
    //$$     return EntityRenderAggregation.isItemAggregationEnabled(stack) ? 1 : models;
    //$$ }
    //#endif
}
//#endif
