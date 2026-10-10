package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.SpawnerPickCapture;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SpawnerPickReplyMixin
{
    @Inject(method = "handleSetHeldSlot", at = @At("TAIL"))
    private void halfmasa_acknowledgeSpawnerPick(ClientboundSetHeldSlotPacket packet, CallbackInfo ci)
    {
        //#if MC >= 1.21.4
        SpawnerPickCapture.getInstance().acknowledge(packet.slot());
        //#else
        //$$ SpawnerPickCapture.getInstance().acknowledge(packet.getSlot());
        //#endif
    }
}
//#endif
