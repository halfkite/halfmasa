package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterController;
//#if MC >= 26.1
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//#else
//$$ import net.minecraft.world.level.BlockAndTintGetter;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
//#if MC >= 1.21.11
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext")
//#else
//$$ @Mixin(targets="net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext")
//#endif
public abstract class CustomSodiumBlockFaceFilterMixin {
    @Shadow protected BlockAndTintGetter level;
    @Shadow protected BlockPos pos;
    @Inject(method="isFaceCulled",at=@At("HEAD"),cancellable=true)
    private void halfmasa$visibleNeighbor(Direction face,CallbackInfoReturnable<Boolean> cir){
        if(face!=null && level!=null && pos!=null && RenderFilterController.INSTANCE.blockFiltering()
            && level.getClass().getName().equals("net.caffeinemc.mods.sodium.client.world.LevelSlice")
            && RenderFilterController.INSTANCE.hideBlock(level.getBlockState(pos.relative(face))))cir.setReturnValue(false);
    }
}
//#endif
