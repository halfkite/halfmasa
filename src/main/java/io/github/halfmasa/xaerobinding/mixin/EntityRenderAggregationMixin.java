package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.EntityLabelPosition;
import io.github.halfmasa.xaerobinding.feature.EntityRenderAggregation;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class EntityRenderAggregationMixin
{
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void halfmasa_beginEntityAggregationFrame(CallbackInfo ci)
    {
        EntityRenderAggregation.getInstance().beginRenderFrame();
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private Iterable<Entity> halfmasa_filterAggregatedEntities(ClientLevel level)
    {
        return EntityRenderAggregation.getInstance().filterForRendering(level.entitiesForRendering());
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void halfmasa_hideGroupedEntity(
            Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta,
            PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci)
    {
        if (EntityRenderAggregation.getInstance().shouldHide(entity)) ci.cancel();
    }

    @Inject(method = "renderEntity", at = @At("TAIL"))
    private void halfmasa_renderAggregationLabel(
            Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta,
            PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci)
    {
        Component label = EntityRenderAggregation.getInstance().getLabel(entity);
        if (label == null) return;

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        boolean side = Configs.ENTITY_AGGREGATION_LABEL_POSITION.getOptionListValue() == EntityLabelPosition.SIDE;

        matrices.pushPose();
        matrices.translate(
                entity.getX() - cameraX + (side ? entity.getBbWidth() * 0.5D + 0.35D : 0.0D),
                entity.getY() - cameraY + (side ? entity.getBbHeight() * 0.5D : entity.getBbHeight() + 0.5D),
                entity.getZ() - cameraZ);
        matrices.mulPose(dispatcher.cameraOrientation());
        matrices.scale(0.025F, -0.025F, 0.025F);

        float textX = -font.width(label) / 2.0F;
        font.drawInBatch(
                label,
                textX,
                0.0F,
                0xFFFFFFFF,
                false,
                matrices.last().pose(),
                vertexConsumers,
                Font.DisplayMode.NORMAL,
                0,
                0xF000F0);
        matrices.popPose();
    }
}
