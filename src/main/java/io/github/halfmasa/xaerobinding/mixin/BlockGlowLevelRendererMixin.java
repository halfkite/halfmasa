package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

//#if MC < 26.0
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import io.github.halfmasa.xaerobinding.feature.BlockGlowService;
//#if MC >= 1.21.10
//$$ import net.minecraft.client.Camera;
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//#endif
//#endif
//#if MC >= 1.21.10 && MC < 26.0
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
//$$ import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import io.github.halfmasa.xaerobinding.feature.BlockGlowSubmitCollector;
//#endif
//#if MC >= 26.1
import net.minecraft.client.renderer.SubmitNodeCollector;
//#if MC < 26.2
import net.minecraft.client.renderer.SubmitNodeStorage;
//#endif
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.BlockGlowService;
import io.github.halfmasa.xaerobinding.feature.BlockGlowSubmitCollector;
//#endif

/** Submit actual baked block geometry to the same outline pass used by glowing entities. */
@Mixin(LevelRenderer.class)
public abstract class BlockGlowLevelRendererMixin
{
    //#if MC < 26.0
    //$$ @Inject(
    //#if MC >= 1.21.3
    //$$         method = "method_62214",
    //#else
    //$$         method = "renderLevel",
    //#endif
    //$$         at = @At(value = "INVOKE", target =
            //$$ "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    //$$ private void halfmasa$renderLegacyBlockGlow(CallbackInfo ci)
    //$$ {
        //$$ BlockGlowService.getInstance().renderLegacyOutlines();
    //$$ }

    //#if MC >= 1.21.10
    //$$ @Inject(method = "extractVisibleBlockEntities", at = @At("TAIL"))
    //$$ private void halfmasa$enableLegacyOutlinePass(Camera camera, float partialTick,
                                                   //$$ LevelRenderState state, CallbackInfo ci)
    //$$ {
        //$$ if (BlockGlowService.getInstance().hasMatches()) state.haveGlowingEntities = true;
    //$$ }
    //#endif
    //#endif
    //#if MC >= 1.21.10 && MC < 26.0
    //$$ @Redirect(method = "submitBlockEntities", at = @At(value = "INVOKE",
            //$$ target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"))
    //$$ private void halfmasa$submitLegacyGlowingBlockEntity(BlockEntityRenderDispatcher dispatcher,
            //$$ BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
            //$$ CameraRenderState camera)
    //$$ {
        //$$ int color = BlockGlowService.getInstance().colorAt(state.blockPos);
        //$$ dispatcher.submit(state, pose, color == 0 ? collector : BlockGlowSubmitCollector.wrap(collector, color), camera);
    //$$ }
    //#endif
    //#if MC >= 26.1
    //#if MC >= 26.2
    @Inject(method = "submitFeatures", at = @At("HEAD"))
    private void halfmasa$submitBlockGlow(LevelRenderState state, SubmitNodeCollector collector,
                                          boolean blockOutline, CallbackInfo ci)
    {
        BlockGlowService.getInstance().submitOutlines(state, collector);
    }
    //#else
    //$$ @Inject(method = "submitBlockEntities", at = @At("HEAD"))
    //$$ private void halfmasa$submitBlockGlow(PoseStack pose, LevelRenderState state,
    //$$                                       SubmitNodeStorage collector, CallbackInfo ci)
    //$$ {
    //$$     BlockGlowService.getInstance().submitOutlines(state, collector);
    //$$ }
    //#endif

    @Redirect(method = "submitBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void halfmasa$submitGlowingBlockEntity(BlockEntityRenderDispatcher dispatcher,
            BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
            CameraRenderState camera)
    {
        int color = BlockGlowService.getInstance().colorAt(state.blockPos);
        dispatcher.submit(state, pose, color == 0 ? collector : BlockGlowSubmitCollector.wrap(collector, color), camera);
    }
    //#endif
}
