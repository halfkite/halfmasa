package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice")
public abstract class SodiumLevelSliceFluidMixin
{
    //#if MC >= 26.1
    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true, require = 0)
    //#else
    //$$ @Inject(method = "method_8316", at = @At("RETURN"), cancellable = true, require = 0)
    //#endif
    private void halfmasa_hideDisabledNeighborFluid(
            BlockPos pos, CallbackInfoReturnable<FluidState> cir)
    {
        FluidState fluidState = cir.getReturnValue();
        if (Configs.DISABLE_FLUID_RENDERING.getBooleanValue() ||
                (Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue() && !fluidState.isSource()))
        {
            cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }
}
