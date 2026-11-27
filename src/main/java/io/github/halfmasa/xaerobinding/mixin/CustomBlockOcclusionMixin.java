package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
public abstract class CustomBlockOcclusionMixin
{
    //#if MC >= 1.21.3
    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"))
    private boolean halfmasa$omitHiddenOccluder(BlockState state, Operation<Boolean> original)
    {
        return original.call(state) && !RenderFilterController.INSTANCE.hideBlock(state);
    }
    //#else
    //$$ @WrapOperation(method = "compile", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    //$$ private boolean halfmasa$omitHiddenOccluder(BlockState state, net.minecraft.world.level.BlockGetter level,
    //$$         net.minecraft.core.BlockPos pos, Operation<Boolean> original) {
    //$$     return original.call(state, level, pos) && !RenderFilterController.INSTANCE.hideBlock(state);
    //$$ }
    //#endif
}
//#endif
