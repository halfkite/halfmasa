package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin
{
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void halfmasa_disableConfiguredFluidRendering(
            BlockAndTintGetter level, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState,
            FluidState fluidState, CallbackInfo ci)
    {
        if (Configs.DISABLE_FLUID_RENDERING.getBooleanValue() ||
                (Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue() && !fluidState.isSource()))
        {
            ci.cancel();
        }
    }
}
