//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskFillArea", remap = false)
public abstract class LitematicaAreaDeleteBlockFilterMixin
{
    @Shadow protected abstract void queueFillCommandForBox(int x1, int y1, int z1, int x2, int y2, int z2);

    @Redirect(method = "directFillBox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState halfmasa$protectDirectDelete(Level level, BlockPos position)
    {
        BlockState state = level.getBlockState(position);
        // Returning the fill state skips the original loop before it clears container contents.
        return LitematicaBlockFilters.isFilteredAreaDelete(this) && !LitematicaBlockFilters.allowsDelete(state)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Redirect(method = "queueFillCommandsForBox", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/scheduler/tasks/TaskFillArea;queueFillCommandForBox(IIIIII)V"))
    private void halfmasa$protectCommandDelete(@Coerce Object task, int x1, int y1, int z1, int x2, int y2, int z2)
    {
        if (!LitematicaBlockFilters.isFilteredAreaDelete(this))
        {
            queueFillCommandForBox(x1, y1, z1, x2, y2, z2);
            return;
        }
        // Keep the native command queue, rate limit and WorldEdit path. Only emit allowed runs.
        Level world = ((LitematicaTaskWorldAccessor) this).halfmasa$getWorld();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int z = z1; z <= z2; z++)
            for (int y = y1; y <= y2; y++)
            {
                int start = -1;
                boolean inRun = false;
                for (int x = x1; x <= x2; x++)
                {
                    BlockState state = world.getBlockState(position.set(x, y, z));
                    boolean allowed = !state.isAir() && LitematicaBlockFilters.allowsDelete(state);
                    if (allowed && !inRun) { start = x; inRun = true; }
                    if (!allowed && inRun)
                    {
                        queueFillCommandForBox(start, y, z, x - 1, y, z);
                        inRun = false;
                    }
                }
                if (inRun) queueFillCommandForBox(start, y, z, x2, y, z);
            }
    }
}
//#endif
