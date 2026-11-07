package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.*;

@Mixin(ServerStatusPinger.class)
public abstract class BetterMultiplayerPingMixin {
    @ModifyVariable(method = "pingServer", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private Runnable halfmasa$observePong(Runnable onPong, ServerData data) {
        if (!BetterMultiplayerConfig.active()) return onPong;
        var probe = ServerLatencyOverlay.begin(data);
        return () -> { ServerLatencyOverlay.complete(data, probe); onPong.run(); };
    }
}
