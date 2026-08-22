package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.multiplayer.ServerData;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ServerIconCache;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerIconCacheMixin
{
    @Shadow @Final private ServerData serverData;

    //#if MC >= 26.1
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void halfmasa_synchronizeServerIcon(
            GuiGraphicsExtractor graphics, int x, int y, boolean hovered, float partialTick, CallbackInfo ci)
    //#elseif MC >= 1.21.10
    //$$ @Inject(method = "renderContent", at = @At("HEAD"))
    //$$ private void halfmasa_synchronizeServerIcon(
    //$$         GuiGraphics graphics, int x, int y, boolean hovered, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"))
    //$$ private void halfmasa_synchronizeServerIcon(
    //$$         GuiGraphics graphics, int index, int y, int x, int width, int height,
    //$$         int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci)
    //#endif
    {
        ServerIconCache.synchronize(this.serverData);
    }
}
