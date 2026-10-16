package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.HoldToShow;
import io.github.halfmasa.xaerobinding.feature.MarkerBlock;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class BlockMarkerVisibilityMixin
{
    //#if MC >= 1.21.10
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //#endif
    private void halfmasa$hideReleasedMarkers(CallbackInfo ci)
    {
        if ((Object)this instanceof MarkerBlock marker && HoldToShow.hideMarker(marker.halfmasa$markerBlock())) ci.cancel();
    }
}
//#endif
