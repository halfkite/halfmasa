//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.schematic.LitematicaSchematic", remap = false)
public abstract class LitematicaSaveBlockFilterMixin
{
    @Shadow @Final private Map<String, Object> blockContainers;
    @Shadow @Final private Map<String, Map<BlockPos, Object>> pendingBlockTicks;
    @Shadow @Final private Map<String, Map<BlockPos, Object>> pendingFluidTicks;

    // Substitute before Litematica counts blocks or reads block entity data.
    @Redirect(method = {"takeBlocksFromWorld", "takeBlocksFromWorldWithinChunk"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = true))
    private BlockState halfmasa$filterSavedBlock(Level world, BlockPos position)
    {
        BlockState state = world.getBlockState(position);
        return LitematicaBlockFilters.allowsSave(state) ? state : Blocks.AIR.defaultBlockState();
    }

    @Inject(method = {"takeBlocksFromWorld", "takeBlocksFromWorldWithinChunk"}, at = @At("RETURN"))
    private void halfmasa$filterSavedTicks(CallbackInfo ci)
    {
        if (!Configs.LITEMATICA_SAVE_FILTER.getBooleanValue()) return;
        halfmasa$removeAirTicks(pendingBlockTicks);
        halfmasa$removeAirTicks(pendingFluidTicks);
    }

    @Unique
    private void halfmasa$removeAirTicks(Map<String, Map<BlockPos, Object>> regions)
    {
        regions.forEach((name, ticks) -> {
            Object container = blockContainers.get(name);
            if (container instanceof LitematicaFilteredContainerAccessor accessor)
            {
                var size = accessor.halfmasa$getSize();
                ticks.keySet().removeIf(pos -> pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0
                        || pos.getX() >= size.getX() || pos.getY() >= size.getY() || pos.getZ() >= size.getZ()
                        || accessor.halfmasa$getState(pos.getX(), pos.getY(), pos.getZ()).isAir());
            }
        });
    }
}
//#endif
