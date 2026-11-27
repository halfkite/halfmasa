package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FlowingFluid.class)
public interface PlacementPreviewFluidInvoker {
    @Invoker("getDropOff")
    int halfmasa$dropOff(LevelReader level);
    @Invoker("getSlopeFindDistance")
    int halfmasa$slopeDistance(LevelReader level);

    //#if MC >= 1.21.3
    @Invoker("canPassThroughWall")
    static boolean halfmasa$pass(Direction direction, BlockGetter level, BlockPos from, BlockState fromState, BlockPos to, BlockState toState) { throw new AssertionError(); }
    @Invoker("canHoldFluid")
    static boolean halfmasa$hold(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { throw new AssertionError(); }
    //#else
    //$$ @Invoker("canPassThroughWall")
    //$$ boolean halfmasa$pass(Direction direction, BlockGetter level, BlockPos from, BlockState fromState, BlockPos to, BlockState toState);
    //$$ @Invoker("canHoldFluid")
    //$$ boolean halfmasa$hold(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid);
    //#endif
}
//#endif
