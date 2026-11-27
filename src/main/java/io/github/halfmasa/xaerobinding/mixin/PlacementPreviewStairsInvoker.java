package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(StairBlock.class)
public interface PlacementPreviewStairsInvoker {
    @Invoker("getStairsShape") static StairsShape halfmasa$shape(BlockState state, BlockGetter view, BlockPos pos) {
        throw new AssertionError("Mixin not applied");
    }
}
//#endif
