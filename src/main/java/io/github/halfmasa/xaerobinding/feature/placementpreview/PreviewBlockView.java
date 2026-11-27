package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
/** Read-only virtual world. Never writes to ClientLevel. */
record PreviewBlockView(BlockGetter base, Map<BlockPos, BlockState> states) implements BlockGetter {
    @Override public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, base.getBlockState(pos)); }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return states.containsKey(pos) ? null : base.getBlockEntity(pos); }
    @Override public int getHeight() { return base.getHeight(); }
    //#if MC >= 1.21.3
    @Override public int getMinY() { return base.getMinY(); }
    //#else
    //$$ @Override public int getMinBuildHeight() { return base.getMinBuildHeight(); }
    //#endif
}
//#endif
