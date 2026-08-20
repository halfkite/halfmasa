package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

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

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void halfmasa_skipAggregatedModel(
            E entity, double x, double y, double z, float yaw, float tickDelta,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci)
    {
        if (EntityRenderAggregation.getInstance().shouldHideModel(entity))
        {
            ci.cancel();
        }
    }
}
