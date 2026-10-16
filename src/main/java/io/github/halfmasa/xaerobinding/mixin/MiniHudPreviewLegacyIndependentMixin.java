package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import net.minecraft.client.gui.GuiGraphics;
import io.github.halfmasa.xaerobinding.feature.minihud.MiniHudPreviewBridge;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Desc(id = "onExtractGuiOverlayPost", value = "onExtractGuiOverlayPost", args = {GuiGraphics.class, float.class, ProfilerFiller.class, Minecraft.class})
@Desc(id = "onRenderGameOverlayPostAdvanced", value = "onRenderGameOverlayPostAdvanced", args = {GuiGraphics.class, float.class, ProfilerFiller.class, Minecraft.class})
@Desc(id = "onRenderGameOverlayPost", value = "onRenderGameOverlayPost", args = {GuiGraphics.class})
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler", remap = false)
public abstract class MiniHudPreviewLegacyIndependentMixin
{
    //#if MC > 1.21.1
    @Inject(method = "@Desc(onRenderGameOverlayPostAdvanced)",
            at = @At("RETURN"), require = 1)
    private void halfmasa$independentPreview(GuiGraphics context, float partialTicks, ProfilerFiller profiler, Minecraft client, CallbackInfo ci)
    {
        MiniHudPreviewBridge.render(context, profiler);
    }
    //#else
    //$$ @Inject(method = "@Desc(onRenderGameOverlayPost)", at = @At("RETURN"), require = 1)
    //$$ private void halfmasa$independentPreview(GuiGraphics context, CallbackInfo ci)
    //$$ {
    //$$     MiniHudPreviewBridge.render(context, Minecraft.getInstance().getProfiler());
    //$$ }
    //#endif
}
//#endif
