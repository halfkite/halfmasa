package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin
{
    //#if MC >= 1.21.10
    @Redirect(
            method = "loadLevels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;)V"))
    private void halfmasa$keepWorldSelectionOnEmpty(Minecraft minecraft, Runnable callback)
    //#else
    //$$ @Redirect(
    //$$         method = "loadLevels",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V"))
    //$$ private void halfmasa$keepWorldSelectionOnEmpty(Minecraft minecraft, Screen callback)
    //#endif
    {
        if (!Configs.KEEP_WORLD_SELECTION_ON_EMPTY.getBooleanValue())
        {
            CreateWorldScreen.openFresh(minecraft, callback);
        }
    }
}
