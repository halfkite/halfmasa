package io.github.halfmasa.xaerobinding.compat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Fabric renamed the directional play registries in Minecraft 26.x. */
public final class FabricPlayCompat
{
    private FabricPlayCompat() {}

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> serverboundPlay()
    {
        //#if MC >= 26.0
        return PayloadTypeRegistry.serverboundPlay();
        //#else
        //$$ return PayloadTypeRegistry.playC2S();
        //#endif
    }

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> clientboundPlay()
    {
        //#if MC >= 26.0
        return PayloadTypeRegistry.clientboundPlay();
        //#else
        //$$ return PayloadTypeRegistry.playS2C();
        //#endif
    }
}
