package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.Screen;
//#if MC >= 1.21.5
import net.minecraft.client.player.LocalPlayer;
//#else
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.network.chat.ClickEvent;
//$$ import net.minecraft.network.chat.Style;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC < 1.21.5
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(Screen.class)
public abstract class ClickAndSendMixin
{
    //#if MC >= 1.21.5
    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void halfmasa_sendChatFromClick(
            LocalPlayer player, String command, Screen screen, CallbackInfo ci)
    {
        if (Configs.CLICK_AND_SEND.getBooleanValue() && !command.startsWith("/"))
        {
            player.connection.sendChat(command);
            ci.cancel();
        }
    }
    //#else
    //$$ @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa_sendChatFromClick(Style style, CallbackInfoReturnable<Boolean> cir)
    //$$ {
    //$$     if (!Configs.CLICK_AND_SEND.getBooleanValue() || style == null)
    //$$     {
    //$$         return;
    //$$     }
    //$$     ClickEvent event = style.getClickEvent();
    //$$     if (event != null && event.getAction() == ClickEvent.Action.RUN_COMMAND &&
    //$$         event.getValue() != null && !event.getValue().startsWith("/"))
    //$$     {
    //$$         Minecraft client = Minecraft.getInstance();
    //$$         if (client.player != null)
    //$$         {
    //$$             client.player.connection.sendChat(event.getValue());
    //$$             cir.setReturnValue(true);
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
