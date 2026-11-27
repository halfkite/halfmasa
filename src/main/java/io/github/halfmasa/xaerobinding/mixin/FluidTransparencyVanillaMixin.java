package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
//#if MC >= 26.1
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
//$$ import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import net.minecraft.client.renderer.block.LiquidBlockRenderer;
//$$ import net.minecraft.world.level.BlockAndTintGetter;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//#if MC >= 26.1
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
//#endif

//#if MC >= 26.1
@Mixin(FluidRenderer.class)
//#else
//$$ @Mixin(LiquidBlockRenderer.class)
//#endif
public abstract class FluidTransparencyVanillaMixin {
    //#if MC >= 26.1
    @ModifyArgs(method = "tesselate", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/renderer/block/FluidRenderer$Output;getBuilder(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 1)
    private void halfmasa$layer(Args args, BlockAndTintGetter level, BlockPos pos,
                               FluidRenderer.Output output, BlockState block, FluidState fluid) {
        if (FluidTransparency.factor(fluid) != 10000) args.set(0, ChunkSectionLayer.TRANSLUCENT);
    }

    @ModifyArgs(method = "tesselate", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/renderer/block/FluidRenderer;addFace(Lcom/mojang/blaze3d/vertex/VertexConsumer;" +
        "FFFFF" + "FFFFF" + "FFFFF" + "FFFFF" + "IIZ)V"), require = 3)
    private void halfmasa$alpha(Args args, BlockAndTintGetter level, BlockPos pos,
                                FluidRenderer.Output output, BlockState block, FluidState fluid) {
        int factor = FluidTransparency.factor(fluid);
        if (factor != 10000) args.set(21, FluidTransparency.scaleAlpha(args.<Integer>get(21), factor));
    }
    //#else
    //$$ @Unique private static final ThreadLocal<Integer> halfmasa$alpha = new ThreadLocal<>();
    //$$
    //$$ @WrapMethod(method = "tesselate")
    //$$ private void halfmasa$withAlpha(BlockAndTintGetter level, BlockPos pos, VertexConsumer vertices,
    //$$         BlockState state, FluidState fluid, Operation<Void> original) {
    //$$     Integer previous = halfmasa$alpha.get();
    //$$     halfmasa$alpha.set(FluidTransparency.factor(fluid));
    //$$     try {
    //$$         original.call(level, pos, vertices, state, fluid);
    //$$     } finally {
    //$$         if (previous == null) halfmasa$alpha.remove();
    //$$         else halfmasa$alpha.set(previous);
    //$$     }
    //$$ }
    //$$
    //$$ @ModifyArg(method = "vertex", at = @At(value = "INVOKE", target =
    //$$         "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 3)
    //$$ private float halfmasa$adjustAlpha(float original) {
    //$$     Integer factor = halfmasa$alpha.get();
    //$$     return factor == null ? original : original * factor / 10000F;
    //$$ }
    //#endif
}
//#endif
