package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import fi.dy.masa.malilib.render.GuiContext;
import io.github.halfmasa.xaerobinding.feature.minihud.MiniHudPreviewBridge;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler", remap = false)
public abstract class MiniHudPreviewIndependentMixin
{
    @Inject(method = "onExtractGuiOverlayPost(Lfi/dy/masa/malilib/render/GuiContext;FLnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN"), require = 1)
    private void halfmasa$independentPreview(GuiContext context, float partialTicks, ProfilerFiller profiler, CallbackInfo ci)
    {
        MiniHudPreviewBridge.render(context, profiler);
    }
}
//#endif
