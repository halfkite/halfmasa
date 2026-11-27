package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class FluidTransparencySodiumColorMixin {
    @Shadow @Final private int[] quadColors;

    @Inject(method = "updateQuad", at = @At("RETURN"), require = 1)
    private void halfmasa$alpha(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos,
                                LightPipeline lighter, Direction direction, ModelQuadFacing facing,
                                float brightness, ColorProvider<FluidState> colorProvider,
                                FluidState fluid, CallbackInfo ci) {
        int factor = FluidTransparency.factor(fluid);
        if (factor == 10000) return;
        for (int i = 0; i < 4; i++) quadColors[i] = FluidTransparency.scaleAlpha(quadColors[i], factor);
    }
}
//#endif
