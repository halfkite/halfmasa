//#if MC >= 1.21.8
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.picker.RegistryPickerScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
//#if MC >= 26.2
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
//#else
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC >= 26.1
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
//#else
//$$ import net.minecraft.client.gui.render.state.GuiRenderState;
//$$ import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
//$$ import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class RegistryPickerEntityRenderMixin
{
    @Shadow @Final private GuiRenderState renderState;
    //#if MC >= 26.2
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    //#else
    //$$ @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    //#endif

    @Unique private final List<GuiEntityRenderer> halfmasa$pickerPreviews = new ArrayList<>();
    @Unique private boolean halfmasa$preparingPicker;
    @Unique private int halfmasa$previewIndex;

    @Inject(method = "preparePictureInPicture", at = @At("HEAD"))
    private void halfmasa$beginPickerFrame(CallbackInfo ci)
    {
        halfmasa$previewIndex = 0;
        halfmasa$preparingPicker = MinecraftClientCompat.getScreen(Minecraft.getInstance()) instanceof RegistryPickerScreen
                && Configs.REGISTRY_PICKER_ENTITY_MODELS.getBooleanValue();
        if (!halfmasa$preparingPicker && !halfmasa$pickerPreviews.isEmpty()) halfmasa$clearPickerPreviews();
    }

    @Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true)
    private void halfmasa$preparePickerPreview(PictureInPictureRenderState state, int guiScale, CallbackInfo ci)
    {
        if (!halfmasa$preparingPicker || !(state instanceof GuiEntityRenderState entity)) return;
        // Each prepared quad keeps its texture until the GUI draws it later in the frame.
        if (halfmasa$previewIndex == halfmasa$pickerPreviews.size())
        {
            //#if MC >= 26.2
            halfmasa$pickerPreviews.add(new GuiEntityRenderer(Minecraft.getInstance().getEntityRenderDispatcher()));
            //#else
            //$$ halfmasa$pickerPreviews.add(new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()));
            //#endif
        }
        //#if MC >= 26.2
        halfmasa$pickerPreviews.get(halfmasa$previewIndex++).prepare(entity, renderState, featureRenderDispatcher, guiScale);
        //#else
        //$$ halfmasa$pickerPreviews.get(halfmasa$previewIndex++).prepare(entity, renderState, guiScale);
        //#endif
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void halfmasa$releasePickerPreviews(CallbackInfo ci)
    {
        halfmasa$clearPickerPreviews();
    }

    @Unique
    private void halfmasa$clearPickerPreviews()
    {
        halfmasa$pickerPreviews.forEach(GuiEntityRenderer::close);
        halfmasa$pickerPreviews.clear();
    }
}
//#endif
