package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.mojang.blaze3d.vertex.PoseStack;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.SubmitNodeCollector;
//#if MC >= 26.0
import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//#endif
//#else
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
//#if MC >= 1.21.3
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
//#endif
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnLabelState;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnTimerService;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnLabelRenderer;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemDespawnLabelMixin
{
    //#if MC >= 1.21.3
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void halfmasa$extractTimer(ItemEntity item, ItemEntityRenderState state, float partialTick, CallbackInfo ci)
    {
        ((ItemDespawnLabelState) state).halfmasa$setDespawnLabel(ItemDespawnLabelRenderer.snapshot(item));
    }

    //#if MC >= 1.21.10
    //#if MC >= 26.0
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    //#else
    //$$ @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("TAIL"))
    //#endif
    private void halfmasa$submitTimer(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci)
    {
        var label = ((ItemDespawnLabelState) state).halfmasa$getDespawnLabel();
        if (label != null && Configs.ITEM_DESPAWN_TIMER.getBooleanValue())
            //#if MC >= 26.2
            collector.submitNameTag(pose, label.attachment(), 0, label.text(), label.throughWalls(), state.lightCoords, camera);
            //#else
            //$$ collector.submitNameTag(pose, label.attachment(), 0, label.text(), label.throughWalls(), state.lightCoords, state.distanceToCameraSq, camera);
            //#endif
    }
    //#else
    //$$ @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    //$$ private void halfmasa$renderTimer(ItemEntityRenderState state, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci)
    //$$ {
    //$$     ItemDespawnLabelRenderer.render(((ItemDespawnLabelState) state).halfmasa$getDespawnLabel(), pose, buffers, light);
    //$$ }
    //#endif
    //#else
    //$$ @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    //$$ private void halfmasa$renderTimer(ItemEntity item, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci)
    //$$ {
    //$$     ItemDespawnLabelRenderer.render(ItemDespawnLabelRenderer.snapshot(item), pose, buffers, light);
    //$$ }
    //#endif
}
//#endif
