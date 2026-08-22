package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.2
import net.minecraft.client.renderer.extract.LevelExtractor;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.LevelRenderer;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import net.minecraft.client.renderer.debug.DebugRenderer;
//$$ import net.minecraft.client.renderer.culling.Frustum;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.bridging.BridgingAssist;

//#if MC >= 26.2
@Mixin(value = LevelExtractor.class, priority = 900)
//#else
//$$ @Mixin(value = LevelRenderer.class, priority = 900)
//#endif
public abstract class BridgingLevelRendererMixin
{
    //#if MC >= 26.2
    @Inject(method = "extractGizmos", at = @At("HEAD"), require = 0)
    private void halfmasa$emitBridgingOutline(CallbackInfo ci)
    {
        BridgingAssist.getInstance().emitOutlineGizmo();
    }
    //#elseif MC >= 26.1
    //$$ @Redirect(
    //$$         method = "extractLevel",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V"),
    //$$         require = 0)
    //$$ private void halfmasa$emitBridgingOutline(
    //$$         DebugRenderer debugRenderer,
    //$$         Frustum frustum,
    //$$         double cameraX,
    //$$         double cameraY,
    //$$         double cameraZ,
    //$$         float partialTick)
    //$$ {
    //$$     debugRenderer.emitGizmos(frustum, cameraX, cameraY, cameraZ, partialTick);
    //$$     BridgingAssist.getInstance().emitOutlineGizmo();
    //$$ }
    //#elseif MC >= 1.21.11
    //$$ @Redirect(
    //$$         method = "renderLevel",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V"),
    //$$         require = 0)
    //$$ private void halfmasa$emitBridgingOutline(
    //$$         DebugRenderer debugRenderer,
    //$$         Frustum frustum,
    //$$         double cameraX,
    //$$         double cameraY,
    //$$         double cameraZ,
    //$$         float partialTick)
    //$$ {
    //$$     debugRenderer.emitGizmos(frustum, cameraX, cameraY, cameraZ, partialTick);
    //$$     BridgingAssist.getInstance().emitOutlineGizmo();
    //$$ }
    //#elseif MC >= 1.21.10
    //$$ @Redirect(
    //$$         method = "renderLevel",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDDZ)V"),
    //$$         require = 0)
    //$$ private void halfmasa$renderBridgingOutline(
    //$$         DebugRenderer debugRenderer,
    //$$         PoseStack poseStack,
    //$$         Frustum frustum,
    //$$         MultiBufferSource.BufferSource bufferSource,
    //$$         double cameraX,
    //$$         double cameraY,
    //$$         double cameraZ,
    //$$         boolean reducedDebugInfo)
    //$$ {
    //$$     debugRenderer.render(poseStack, frustum, bufferSource, cameraX, cameraY, cameraZ, reducedDebugInfo);
    //$$     BridgingAssist.getInstance().renderOutline(poseStack, bufferSource, cameraX, cameraY, cameraZ);
    //$$ }
    //#else
    //$$ @Inject(method = "renderLevel", at = @At("TAIL"), require = 0)
    //$$ private void halfmasa$renderBridgingOutline(CallbackInfo ci)
    //$$ {
    //$$ }
    //#endif
}
