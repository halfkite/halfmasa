package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl")
public abstract class SodiumFluidRendererMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void halfmasa_disableConfiguredSodiumFluidRendering(
            @Coerce Object levelSlice, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos origin,
            @Coerce Object translucentCollector, @Coerce Object buffers, CallbackInfo ci)
    {
        if (Configs.DISABLE_FLUID_RENDERING.getBooleanValue() ||
                (Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue() && !fluidState.isSource()))
        {
            ci.cancel();
        }
    }
}
