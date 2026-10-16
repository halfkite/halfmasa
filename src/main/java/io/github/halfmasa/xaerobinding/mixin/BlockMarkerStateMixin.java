package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.MarkerBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BlockMarker;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockMarker.class)
public abstract class BlockMarkerStateMixin implements MarkerBlock
{
    @Unique private Block halfmasa$block;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void halfmasa$rememberBlock(ClientLevel level, double x, double y, double z, BlockState state, CallbackInfo ci)
    { this.halfmasa$block = state.getBlock(); }

    @Override public Block halfmasa$markerBlock() { return this.halfmasa$block; }
}
//#endif
