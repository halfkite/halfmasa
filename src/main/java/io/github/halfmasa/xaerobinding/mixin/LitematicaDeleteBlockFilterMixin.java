//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskDeleteBlocksByPlacement", remap = false)
public abstract class LitematicaDeleteBlockFilterMixin
{
    @Redirect(method = "removeBlocksInBox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = true))
    private BlockState halfmasa$filterDeletedBlock(Level world, BlockPos position)
    {
        BlockState state = world.getBlockState(position);
        return LitematicaBlockFilters.allowsDelete(state) ? state : Blocks.AIR.defaultBlockState();
    }
}
//#endif
