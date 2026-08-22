package io.github.halfmasa.xaerobinding.mixin;

//#if MC < 1.21.10
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.EntityRenderAggregation;

/**
 * Uses the same visibility boundary as Tweakeroo's entity rendering disable
 */
@Mixin(value = EntityRenderDispatcher.class, priority = 900)
public abstract class EntityRenderDispatcherAggregationMixin
{
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void halfmasa_skipAggregatedEntity(
            Entity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (EntityRenderAggregation.getInstance().shouldHide(entity))
        {
            cir.setReturnValue(false);
        }
    }

    //#if MC >= 1.21.10
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void halfmasa_applyAggregationState(
            E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir)
    {
        EntityRenderState state = cir.getReturnValue();
        Component label = EntityRenderAggregation.getInstance().getLabel(entity);
        if (label != null)
        {
            boolean side = io.github.halfmasa.xaerobinding.config.Configs.ENTITY_AGGREGATION_LABEL_POSITION
                    .getOptionListValue() == io.github.halfmasa.xaerobinding.config.EntityLabelPosition.SIDE;
            state.nameTag = label;
            state.nameTagAttachment = new Vec3(
                    side ? entity.getBbWidth() * 0.5D + 0.35D : 0.0D,
                    side ? entity.getBbHeight() * 0.5D : entity.getBbHeight() + 0.5D,
                    0.0D);
        }
        if (EntityRenderAggregation.getInstance().shouldHideModel(entity))
        {
            state.isInvisible = true;
        }
    }
    //#elseif MC >= 1.21.4
    //$$ @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    //$$ private <E extends Entity> void halfmasa_skipAggregatedModel(
    //$$         E entity, double x, double y, double z, float tickDelta,
    //$$         PoseStack poseStack, MultiBufferSource buffers, int packedLight,
    //$$         org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci)
    //$$ {
    //$$     if (EntityRenderAggregation.getInstance().shouldHideModel(entity))
    //$$     {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#else
    //$$ @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    //$$ private <E extends Entity> void halfmasa_skipAggregatedModel(
    //$$         E entity, double x, double y, double z, float yaw, float tickDelta,
    //$$         PoseStack poseStack, MultiBufferSource buffers, int packedLight,
    //$$         org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci)
    //$$ {
    //$$     if (EntityRenderAggregation.getInstance().shouldHideModel(entity))
    //$$     {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
