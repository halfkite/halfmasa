package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
//#if MC >= 26.1
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//#else
//$$ import net.minecraft.world.level.BlockAndTintGetter;
//#endif
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//#if MC >= 1.21.8
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
//#else
//$$ import net.minecraft.client.renderer.chunk.RenderChunkRegion;
//#endif
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ModelBlockRenderer.class)
public abstract class CustomBlockModelFilterMixin {
    //#if MC >= 26.1
    // Coerce output type to avoid binding to platform-specific vertex output implementations.
    @Inject(method="tesselateBlock",at=@At("HEAD"),cancellable=true)
    private void halfmasa$hide(@Coerce Object output,float x,float y,float z,BlockAndTintGetter level,BlockPos pos,BlockState state,@Coerce Object model,long seed,CallbackInfo ci){
        if(level instanceof net.minecraft.client.renderer.chunk.RenderSectionRegion && RenderFilterController.INSTANCE.hideBlock(state))ci.cancel();
    }
    @Inject(method="shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",at=@At("HEAD"),cancellable=true,require=0)
    private void halfmasa$face(BlockAndTintGetter level,BlockState state,Direction dir,BlockPos neighbor,CallbackInfoReturnable<Boolean> cir){
        if(level instanceof net.minecraft.client.renderer.chunk.RenderSectionRegion && RenderFilterController.INSTANCE.hideBlock(level.getBlockState(neighbor)))cir.setReturnValue(true);
    }
    //#elseif MC >= 1.21.5
    //$$ @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa$hide(BlockAndTintGetter level,
    //$$         java.util.List<net.minecraft.client.renderer.block.model.BlockModelPart> parts, BlockState state,
    //$$         BlockPos pos, com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer output,
    //$$         boolean checkSides, int overlay, CallbackInfo ci) {
    //#if MC >= 1.21.8
    //$$     if (level instanceof RenderSectionRegion
    //#else
    //$$     if (level instanceof RenderChunkRegion
    //#endif
    //$$             && RenderFilterController.INSTANCE.hideBlock(state)) ci.cancel();
    //$$ }
    //$$ @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    //$$ private static void halfmasa$face(BlockAndTintGetter level, BlockState state, boolean checkSides,
    //$$         Direction direction, BlockPos neighbor, CallbackInfoReturnable<Boolean> cir) {
    //#if MC >= 1.21.8
    //$$     if (level instanceof RenderSectionRegion
    //#else
    //$$     if (level instanceof RenderChunkRegion
    //#endif
    //$$             && RenderFilterController.INSTANCE.hideBlock(level.getBlockState(neighbor))) cir.setReturnValue(true);
    //$$ }
    //#else
    //$$ @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa$hide(BlockAndTintGetter level, net.minecraft.client.resources.model.BakedModel model,
    //$$         BlockState state, BlockPos pos, com.mojang.blaze3d.vertex.PoseStack pose,
    //$$         com.mojang.blaze3d.vertex.VertexConsumer output, boolean checkSides, net.minecraft.util.RandomSource random,
    //$$         long seed, int overlay, CallbackInfo ci) {
    //$$     if (level instanceof RenderChunkRegion && RenderFilterController.INSTANCE.hideBlock(state)) ci.cancel();
    //$$ }
    //$$
    //#if MC >= 1.21.3
    //$$ @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
    //$$         method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    //$$ private boolean halfmasa$face(BlockState state, BlockState neighbor, Direction face,
    //$$         com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original,
    //$$         BlockAndTintGetter level) {
    //$$     return level instanceof RenderChunkRegion && RenderFilterController.INSTANCE.hideBlock(neighbor)
    //$$             || original.call(state, neighbor, face);
    //$$ }
    //#else
    //$$ @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
    //$$         method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"))
    //$$ private boolean halfmasa$legacyFace(BlockState state, net.minecraft.world.level.BlockGetter level,
    //$$         BlockPos pos, Direction face, BlockPos neighbor,
    //$$         com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
    //$$     return level instanceof RenderChunkRegion && RenderFilterController.INSTANCE.hideBlock(level.getBlockState(neighbor))
    //$$             || original.call(state, level, pos, face, neighbor);
    //$$ }
    //#endif
    //#endif
}
//#endif
