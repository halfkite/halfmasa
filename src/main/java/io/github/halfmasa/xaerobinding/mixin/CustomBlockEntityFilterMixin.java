package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//#endif
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
//#if MC >= 1.21.10
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class CustomBlockEntityFilterMixin {
    //#if MC >= 1.21.10
    //#if MC >= 26.2
    @Inject(method="tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Z)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",at=@At("HEAD"),cancellable=true)
    private void halfmasa$hide(BlockEntity blockEntity,float partialTicks,@Coerce Object breakProgress,boolean isGloballyRendered,CallbackInfoReturnable<BlockEntityRenderState> cir){
    //#else
    //$$ @Inject(method="tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",at=@At("HEAD"),cancellable=true)
    //$$ private void halfmasa$hide(BlockEntity blockEntity,float partialTicks,@Coerce Object breakProgress,CallbackInfoReturnable<BlockEntityRenderState> cir){
    //#endif
        if(blockEntity.getLevel()!=null && RenderFilterController.INSTANCE.hideBlockEntity(blockEntity.getBlockState()))cir.setReturnValue(null);
    }
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa$hide(BlockEntity blockEntity, float delta, com.mojang.blaze3d.vertex.PoseStack pose,
    //$$         net.minecraft.client.renderer.MultiBufferSource buffers, CallbackInfo ci) {
    //$$     if (blockEntity.getLevel() != null && RenderFilterController.INSTANCE.hideBlockEntity(blockEntity.getBlockState())) ci.cancel();
    //$$ }
    //#endif
}
//#endif
