package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
//#if MC >= 1.21.10
import net.minecraft.client.Minecraft;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.feature.FastScrolling;

@Mixin(VideoSettingsScreen.class)
public abstract class FastScrollingVideoSettingsMixin
{
    //#if MC >= 1.21.10
    @Redirect(
            method = "mouseScrolled",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;hasControlDown()Z"))
    private boolean halfmasa$preserveGuiScaleShortcut(Minecraft client)
    {
        return !FastScrolling.isActive() && client.hasControlDown();
    }
    //#else
    //$$ @Redirect(
    //$$         method = "mouseScrolled",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/gui/screens/Screen;hasControlDown()Z"))
    //$$ private boolean halfmasa$preserveGuiScaleShortcut()
    //$$ {
    //$$     return !FastScrolling.isActive() && Screen.hasControlDown();
    //$$ }
    //#endif
}
