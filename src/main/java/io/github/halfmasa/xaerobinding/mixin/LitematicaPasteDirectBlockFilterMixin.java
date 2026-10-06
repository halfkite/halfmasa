//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.SchematicPlacingUtils", remap = false)
public abstract class LitematicaPasteDirectBlockFilterMixin
{
    @Redirect(method = "placeBlocksWithinChunk", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/schematic/container/LitematicaBlockStateContainer;get(III)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState halfmasa$skipExcludedBlock(@Coerce Object container, int x, int y, int z)
    {
        BlockState state = ((LitematicaFilteredContainerAccessor) container).halfmasa$getState(x, y, z);
        // STRUCTURE_VOID is the native skip marker, checked before clearing existing block entities.
        // The underlying schematic container remains unchanged.
        return LitematicaBlockFilters.allowsPaste(state) ? state : Blocks.STRUCTURE_VOID.defaultBlockState();
    }

    @Redirect(method = "placeBlocksWithinChunk", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private static Set<Map.Entry<BlockPos, Object>> halfmasa$skipExcludedTicks(Map<BlockPos, Object> ticks,
            Level world, ChunkPos chunk, String name, @Coerce Object container, Map<?, ?> blockEntities,
            BlockPos origin, @Coerce Object placement, @Coerce Object subRegion,
            Map<?, ?> blockTicks, Map<?, ?> fluidTicks, @Coerce Object replace, @Coerce Object layer, boolean notifyNeighbors)
    {
        if (!Configs.LITEMATICA_PASTE_FILTER.getBooleanValue()) return ticks.entrySet();
        var accessor = (LitematicaFilteredContainerAccessor) container;
        var size = accessor.halfmasa$getSize();
        // Return a filtered view of entries without modifying saved block/fluid tick maps.
        return ticks.entrySet().stream().filter(entry -> {
            BlockPos pos = entry.getKey();
            return pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                    && pos.getX() < size.getX() && pos.getY() < size.getY() && pos.getZ() < size.getZ()
                    && LitematicaBlockFilters.allowsPaste(accessor.halfmasa$getState(pos.getX(), pos.getY(), pos.getZ()));
        }).collect(Collectors.toSet());
    }
}
//#endif
