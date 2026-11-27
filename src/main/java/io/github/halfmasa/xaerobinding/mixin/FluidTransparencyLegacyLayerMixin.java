package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 26.1
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
//#if MC >= 1.21.8
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//$$ import net.minecraft.client.renderer.chunk.SectionCompiler;
//$$ import net.minecraft.world.level.material.FluidState;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$
//$$ @Mixin(SectionCompiler.class)
//$$ public abstract class FluidTransparencyLegacyLayerMixin {
//#if MC >= 1.21.8
//$$     @WrapOperation(method = "compile", at = @At(value = "INVOKE", target =
//$$             "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"))
//$$     private ChunkSectionLayer halfmasa$translucent(FluidState fluid, Operation<ChunkSectionLayer> original) {
//$$         return FluidTransparency.factor(fluid) == 10000 ? original.call(fluid) : ChunkSectionLayer.TRANSLUCENT;
//$$     }
//#else
//$$     @WrapOperation(method = "compile", at = @At(value = "INVOKE", target =
//$$             "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/RenderType;"))
//$$     private RenderType halfmasa$translucent(FluidState fluid, Operation<RenderType> original) {
//$$         return FluidTransparency.factor(fluid) == 10000 ? original.call(fluid) : RenderType.translucent();
//$$     }
//#endif
//$$ }
//#endif
