package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.BlockPos;
//#if MC >= 26.1
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//#else
//$$ import net.minecraft.client.renderer.block.LiquidBlockRenderer;
//$$ import net.minecraft.world.level.BlockAndTintGetter;
//#endif
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

//#if MC >= 26.1
@Mixin(FluidRenderer.class)
//#else
//$$ @Mixin(LiquidBlockRenderer.class)
//#endif
public abstract class LiquidBlockRendererMixin
{
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void halfmasa_disableConfiguredFluidRendering(
            BlockAndTintGetter level, BlockPos pos,
            //#if MC >= 26.1
            FluidRenderer.Output vertexConsumer,
            //#else
            //$$ VertexConsumer vertexConsumer,
            //#endif
            BlockState blockState,
            FluidState fluidState, CallbackInfo ci)
    {
        if (Configs.DISABLE_FLUID_RENDERING.getBooleanValue() ||
                (Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue() && !fluidState.isSource()))
        {
            ci.cancel();
        }
    }
}
