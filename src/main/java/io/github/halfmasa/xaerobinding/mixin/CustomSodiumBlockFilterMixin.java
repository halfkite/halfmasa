package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer")
public abstract class CustomSodiumBlockFilterMixin {
    @Inject(method="renderModel",at=@At("HEAD"),cancellable=true)
    private void halfmasa$hide(@Coerce Object model,BlockState state,BlockPos pos,BlockPos origin,CallbackInfo ci){
        if(RenderFilterController.INSTANCE.hideBlock(state))ci.cancel();
    }
}
//#endif
