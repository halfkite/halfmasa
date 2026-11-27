package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
//#if MC >= 26.2
import net.minecraft.client.renderer.extract.LevelExtractor;
//#else
//$$ import net.minecraft.client.renderer.LevelRenderer;
//#endif
//#if MC >= 26.1
import net.minecraft.client.renderer.state.level.LevelRenderState;
//#elseif MC >= 1.21.10
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//#endif
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
@Mixin(LevelExtractor.class)
//#else
//$$ @Mixin(LevelRenderer.class)
//#endif
public abstract class CustomBlockOutlineFilterMixin {
    //#if MC >= 1.21.10
    @Inject(method="extractBlockOutline",at=@At("RETURN"))
    private void halfmasa$hide(Camera camera,LevelRenderState state,CallbackInfo ci){
        Minecraft mc=Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && RenderFilterController.INSTANCE.hideOutline(mc.level.getBlockState(hit.getBlockPos())))
            state.blockOutlineRenderState = null;
    }
    //#else
    //$$ @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa$hide(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
    //$$         net.minecraft.world.entity.Entity selected, double cameraX, double cameraY, double cameraZ,
    //$$         net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
    //#if MC >= 1.21.3
    //$$         int color,
    //#endif
    //$$         CallbackInfo ci) {
    //$$     if (RenderFilterController.INSTANCE.hideOutline(state)) ci.cancel();
    //$$ }
    //#endif
}
//#endif
