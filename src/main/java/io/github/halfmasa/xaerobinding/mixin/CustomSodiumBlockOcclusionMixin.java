package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class CustomSodiumBlockOcclusionMixin
{
    @WrapOperation(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            //#if MC >= 26.1
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"))
            //#elseif MC >= 1.21.3
            //$$ at = {
            //$$     @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"),
            //$$     @At(value = "INVOKE", target = "Lnet/minecraft/class_2680;method_26216()Z")
            //$$ })
            //#else
            //$$ at = {
            //$$     @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"),
            //$$     @At(value = "INVOKE", target = "Lnet/minecraft/class_2680;method_26216(Lnet/minecraft/class_1922;Lnet/minecraft/class_2338;)Z")
            //$$ })
            //#endif
    //#if MC >= 1.21.3
    private boolean halfmasa$omitHiddenOccluder(BlockState state, Operation<Boolean> original)
    {
        return original.call(state) && !RenderFilterController.INSTANCE.hideBlock(state);
    }
    //#else
    //$$ private boolean halfmasa$omitHiddenOccluder(BlockState state, net.minecraft.world.level.BlockGetter level,
    //$$         net.minecraft.core.BlockPos pos, Operation<Boolean> original)
    //$$ {
    //$$     return original.call(state, level, pos) && !RenderFilterController.INSTANCE.hideBlock(state);
    //$$ }
    //#endif
}
//#endif
