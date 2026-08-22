package io.github.halfmasa.xaerobinding.mixin;

import java.io.File;
import java.util.function.Consumer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin
{
    //#if MC >= 26.0
    @Redirect(
            method = "grab(Lnet/minecraft/client/Minecraft;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"),
            require = 0)
    private static void halfmasa$markF2Screenshot(
            File gameDirectory,
            RenderTarget renderTarget,
            Consumer<Component> messageReceiver)
    {
        ScreenshotClipboard.requestCopy();
        Screenshot.grab(gameDirectory, renderTarget, messageReceiver);
    }
    //#endif

    //#if MC >= 1.21.8
    @ModifyVariable(
            method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0)
    private static Consumer<NativeImage> halfmasa$wrapScreenshotConsumer(Consumer<NativeImage> original)
    {
        //#if MC >= 26.0
        return ScreenshotClipboard.wrapIfRequested(original);
        //#else
        //$$ return ScreenshotClipboard.wrap(original);
        //#endif
    }
    //#else
    //$$ @Inject(
    //$$         method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lcom/mojang/blaze3d/platform/NativeImage;",
    //$$         at = @At("RETURN"),
    //$$         require = 0)
    //$$ private static void halfmasa$copyScreenshot(
    //$$         RenderTarget renderTarget,
    //$$         CallbackInfoReturnable<NativeImage> callback)
    //$$ {
    //$$     ScreenshotClipboard.copyIfEnabled(callback.getReturnValue());
    //$$ }
    //#endif
}
