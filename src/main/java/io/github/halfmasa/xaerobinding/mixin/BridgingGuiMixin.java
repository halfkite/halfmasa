package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
//#if MC >= 26.2
import net.minecraft.client.gui.Hud;
//#endif
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.bridging.BridgingAssist;

//#if MC >= 26.2
@Mixin(Hud.class)
//#else
//$$ @Mixin(Gui.class)
//#endif
public abstract class BridgingGuiMixin
{
    //#if MC < 26.2
    //$$ @Shadow @Final private DebugScreenOverlay debugOverlay;
    //#endif

    //#if MC >= 26.1
    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void halfmasa$renderBridgingIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "renderCrosshair", at = @At("TAIL"))
    //$$ private void halfmasa$renderBridgingIndicator(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci)
    //#endif
    {
        //#if MC >= 26.2
        boolean debugScreenVisible = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        //#else
        //$$ boolean debugScreenVisible = this.debugOverlay.showDebugScreen();
        //#endif
        BridgingAssist.getInstance().renderIndicator(graphics, deltaTracker, debugScreenVisible);
    }
}
