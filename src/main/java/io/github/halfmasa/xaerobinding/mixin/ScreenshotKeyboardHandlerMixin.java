package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import java.io.File;
import java.util.function.Consumer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard;

/** Marks the direct F2 screenshot call used by Minecraft 26.1.x. */
@Mixin(KeyboardHandler.class)
public abstract class ScreenshotKeyboardHandlerMixin
{
    @Redirect(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"),
            require = 1)
    private static void halfmasa$markF2Screenshot(
            File gameDirectory,
            RenderTarget renderTarget,
            Consumer<Component> messageReceiver)
    {
        ScreenshotClipboard.requestCopy();
        Screenshot.grab(gameDirectory, renderTarget, messageReceiver);
    }
}
//#endif
