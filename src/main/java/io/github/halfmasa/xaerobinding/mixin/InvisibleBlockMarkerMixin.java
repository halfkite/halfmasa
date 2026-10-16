package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.sugar.Local;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.HoldToShow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds vanilla markers at vanilla sample positions, leaving held-item behavior intact. */
@Mixin(ClientLevel.class)
public abstract class InvisibleBlockMarkerMixin
{
    @Inject(method = "doAnimateTick", at = @At("TAIL"), require = 1)
    private void halfmasa$invisibleMarkers(CallbackInfo ci, @Local BlockState state,
            @Local(argsOnly = true) Block markerParticleTarget,
            @Local(argsOnly = true) BlockPos.MutableBlockPos pos)
    {
        if (!Configs.INVISIBLE_BLOCKS.getBooleanValue() || !HoldToShow.invisibleVisible()) return;
        Block block = state.getBlock();
        if (block == markerParticleTarget) return;
        boolean show = block == Blocks.BARRIER && Configs.INVISIBLE_SHOW_BARRIER.getBooleanValue()
                || block == Blocks.LIGHT && Configs.INVISIBLE_SHOW_LIGHT.getBooleanValue()
                || block == Blocks.STRUCTURE_VOID && Configs.INVISIBLE_SHOW_STRUCTURE_VOID.getBooleanValue();
        if (show)
        {
            ((ClientLevel)(Object)this).addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }
}
//#endif
