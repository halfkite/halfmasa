package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.VoidTrading;

//#if MC >= 26.2
@Mixin(Gui.class)
//#else
//$$ @Mixin(Minecraft.class)
//#endif
public abstract class VoidTradeGuiMixin
{
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void halfmasa$handleVoidTradingScreen(Screen screen, CallbackInfo ci)
    {
        VoidTrading.onScreenChanged(Minecraft.getInstance(), screen);
    }
}
