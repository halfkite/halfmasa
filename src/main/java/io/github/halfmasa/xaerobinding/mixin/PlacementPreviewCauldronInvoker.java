package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractCauldronBlock.class)
public interface PlacementPreviewCauldronInvoker {
    @Invoker("getContentHeight")
    double halfmasa$contentHeight(BlockState state);
}
//#endif
