package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.PlantCenteringConfigs;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PlantModelOffsetMixin
{
    @Inject(method = "getOffset", at = @At("HEAD"), cancellable = true)
    //#if MC >= 26.1
    private void halfmasa_centerConfiguredPlantModels(
            BlockPos pos, CallbackInfoReturnable<Vec3> cir)
    //#elseif MC >= 1.21.3
    //$$ private void halfmasa_centerConfiguredPlantModels(
    //$$         BlockPos pos, CallbackInfoReturnable<Vec3> cir)
    //#else
    //$$ private void halfmasa_centerConfiguredPlantModels(
    //$$         BlockGetter level, BlockPos pos, CallbackInfoReturnable<Vec3> cir)
    //#endif
    {
        BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;
        if (PlantCenteringConfigs.isEnabled(state.getBlock()))
        {
            cir.setReturnValue(Vec3.ZERO);
        }
    }
}
