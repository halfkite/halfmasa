package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.client.renderer.LevelRenderer;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.SubmitNodeCollector;
//#if MC >= 26.1
import net.minecraft.client.renderer.state.level.LevelRenderState;
//#else
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//#endif
//#endif
import io.github.halfmasa.xaerobinding.feature.placementpreview.PlacementPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class PlacementPreviewRenderMixin {
    //#if MC >= 26.2
    @Inject(method="submitFeatures",at=@At("HEAD")) private void halfmasa$submit(LevelRenderState state,SubmitNodeCollector collector,boolean blockOutline,CallbackInfo ci){PlacementPreview.submit(state,collector);}
    //#elseif MC >= 1.21.10
    //$$ @Inject(method = "submitEntities", at = @At("HEAD"))
    //$$ private void halfmasa$submit(com.mojang.blaze3d.vertex.PoseStack pose, LevelRenderState state,
    //$$         SubmitNodeCollector collector, CallbackInfo ci) {
    //$$     PlacementPreview.submit(state, collector);
    //$$ }
    //#else
    //$$ @Inject(
    //#if MC >= 1.21.3
    //$$         method = "method_62214",
    //#else
    //$$         method = "renderLevel",
    //#endif
    //$$         at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    //$$ private void halfmasa$renderLegacy(CallbackInfo ci) { PlacementPreview.renderLegacy(); }
    //#endif
}
//#endif
