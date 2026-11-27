package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(BlockItem.class)
public interface PlacementPreviewBlockItemInvoker {
    @Invoker("getPlacementState") BlockState halfmasa$placementState(BlockPlaceContext context);
    @Invoker("canPlace") boolean halfmasa$canPlace(BlockPlaceContext context, BlockState state);
}
//#endif
