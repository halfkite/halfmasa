package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.AutoEatInventorySync;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
//#if MC >= 1.21.3
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class AutoEatInventorySyncMixin
{
    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void halfmasa$confirmFoodSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci)
    {
        AutoEatInventorySync.getInstance().menuSlot(packet.getContainerId(), packet.getSlot(), packet.getItem());
    }
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void halfmasa$confirmFoodMenu(ClientboundContainerSetContentPacket packet, CallbackInfo ci)
    {
        //#if MC >= 1.21.5
        AutoEatInventorySync.getInstance().fullMenu(packet.containerId(), packet.items());
        //#else
        //$$ AutoEatInventorySync.getInstance().fullMenu(packet.getContainerId(), packet.getItems());
        //#endif
    }
    //#if MC >= 1.21.3
    @Inject(method = "handleSetPlayerInventory", at = @At("TAIL"))
    private void halfmasa$confirmPlayerFoodSlot(ClientboundSetPlayerInventoryPacket packet, CallbackInfo ci)
    {
        AutoEatInventorySync.getInstance().playerSlot(packet.slot(), packet.contents());
    }
    //#endif
}
//#endif
