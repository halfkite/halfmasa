package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerExitMixin
{
    @Inject(method = "halt", at = @At("HEAD"), cancellable = true)
    private void halfmasa_skipRepeatedHalt(boolean waitForServer, CallbackInfo ci)
    {
        IntegratedServer server = (IntegratedServer) (Object) this;
        Thread serverThread = server.getRunningThread();
        if (!server.isRunning() || server.isStopped() || serverThread == null || !serverThread.isAlive())
        {
            ci.cancel();
        }
    }
}
//#endif
