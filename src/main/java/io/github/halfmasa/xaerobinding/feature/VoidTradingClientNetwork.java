package io.github.halfmasa.xaerobinding.feature;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import io.github.halfmasa.xaerobinding.feature.VoidTradingPayloads.MaterialRequest;
import io.github.halfmasa.xaerobinding.feature.VoidTradingPayloads.MaterialResult;

/** Optional client-side networking for the server companion. */
public final class VoidTradingClientNetwork
{
    private static boolean initialized;
    private static int nextRequestId;

    private VoidTradingClientNetwork()
    {
    }

    public static void initialize()
    {
        if (initialized)
        {
            return;
        }

        io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay().register(MaterialRequest.TYPE, MaterialRequest.STREAM_CODEC);
        io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.clientboundPlay().register(MaterialResult.TYPE, MaterialResult.STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(
                MaterialResult.TYPE,
                (result, context) -> context.client().execute(() -> VoidTrading.onMaterialPreparationResult(result)));
        initialized = true;
    }

    public static int requestMaterials(boolean pullQuickShulker, boolean uncraftEmeraldBlocks)
    {
        if (!initialized || !ClientPlayNetworking.canSend(MaterialRequest.TYPE))
        {
            return 0;
        }

        if (++nextRequestId <= 0)
        {
            nextRequestId = 1;
        }

        ClientPlayNetworking.send(new MaterialRequest(nextRequestId, pullQuickShulker, uncraftEmeraldBlocks));
        return nextRequestId;
    }
}
