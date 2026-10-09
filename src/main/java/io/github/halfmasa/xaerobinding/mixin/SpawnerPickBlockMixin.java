package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import io.github.halfmasa.xaerobinding.feature.SpawnerPickCapture;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class SpawnerPickBlockMixin
{
    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"))
    private void halfmasa_captureSpawnerState(BlockPos pos, boolean includeData, CallbackInfo ci)
    {
        SpawnerPickCapture.getInstance().capture(pos, includeData);
    }
}
//#endif
