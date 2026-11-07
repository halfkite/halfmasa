//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.AutoOpenChat;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.platform.InputConstants;

@Mixin(ChatScreen.class)
public abstract class AutoOpenChatScreenMixin
{
    @Inject(method = "removed", at = @At("HEAD"))
    private void halfmasa$rememberPointerBeforeMouseGrab(CallbackInfo ci)
    {
        AutoOpenChat.getInstance().rememberPointer((ChatScreen) (Object) this);
    }

    //#if MC >= 1.21.10
    @Inject(method = "onClose", at = @At("TAIL"))
    private void halfmasa$stopAfterManualClose(CallbackInfo ci)
    {
        AutoOpenChat.getInstance().onIntentionalClose((ChatScreen) (Object) this);
    }
    //#else
    //$$ @Inject(method = "keyPressed", at = @At("RETURN"))
    //$$ private void halfmasa$stopAfterLegacyEscape(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> cir)
    //$$ {
    //$$     if (key == InputConstants.KEY_ESCAPE)
    //$$         AutoOpenChat.getInstance().onIntentionalClose((ChatScreen) (Object) this);
    //$$ }
    //#endif

    @Inject(method = "handleChatInput", at = @At("TAIL"))
    private void halfmasa$stopAfterMatchingSubmission(String message, boolean addToHistory, CallbackInfo ci)
    {
        AutoOpenChat.getInstance().onSubmitted((ChatScreen) (Object) this, message);
    }
}
//#endif
