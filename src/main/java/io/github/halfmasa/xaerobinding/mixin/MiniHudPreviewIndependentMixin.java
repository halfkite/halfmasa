package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
import io.github.halfmasa.xaerobinding.feature.minihud.MiniHudPreviewBridge;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Desc(id = "onExtractGuiOverlayPost", value = "onExtractGuiOverlayPost", args = {GuiContext.class, float.class, ProfilerFiller.class})
@Desc(id = "onRenderGameOverlayPostAdvanced", value = "onRenderGameOverlayPostAdvanced", args = {GuiContext.class, float.class, ProfilerFiller.class})
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler", remap = false)
public abstract class MiniHudPreviewIndependentMixin
{
    //#if MC >= 26.1
    @Inject(method = "@Desc(onExtractGuiOverlayPost)",
    //#else
    //$$ @Inject(method = "@Desc(onRenderGameOverlayPostAdvanced)",
    //#endif
            at = @At("RETURN"), require = 1)
    private void halfmasa$independentPreview(GuiContext context, float partialTicks, ProfilerFiller profiler, CallbackInfo ci)
    {
        MiniHudPreviewBridge.render(context, profiler);
    }
}
//#endif
