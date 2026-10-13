//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.core.BlockPos;
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

    // Filter the state after other mods supply it, before Litematica counts or stores it.
    @ModifyVariable(method = {"takeBlocksFromWorld", "takeBlocksFromWorldWithinChunk"}, at = @At("STORE"), ordinal = 0)
    private BlockState halfmasa$filterSavedBlock(BlockState state)
    {
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
