package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif

/** The wire format shared by the client mod and the optional server support mod. */
public final class VoidTradingPayloads
{
    private VoidTradingPayloads()
    {
    }

    public record MaterialRequest(int requestId, boolean pullQuickShulker, boolean uncraftEmeraldBlocks)
            implements CustomPacketPayload
    {
        public static final Type<MaterialRequest> TYPE = new Type<>(
//#if MC >= 1.21.11
                Identifier.fromNamespaceAndPath("halfmasa", "void_trade_material_request"));
//#else
//$$                 ResourceLocation.fromNamespaceAndPath("halfmasa", "void_trade_material_request"));
//#endif
        public static final StreamCodec<RegistryFriendlyByteBuf, MaterialRequest> STREAM_CODEC =
                StreamCodec.of((buffer, request) -> request.write(buffer), MaterialRequest::read);

        private void write(RegistryFriendlyByteBuf buffer)
        {
            buffer.writeVarInt(this.requestId);
            buffer.writeBoolean(this.pullQuickShulker);
            buffer.writeBoolean(this.uncraftEmeraldBlocks);
        }

        private static MaterialRequest read(RegistryFriendlyByteBuf buffer)
        {
            return new MaterialRequest(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record MaterialResult(
            int requestId,
            boolean quickShulkerAvailable,
            int emeraldsPulled,
            int emeraldBlocksPulled,
            int emeraldBlocksUncrafted) implements CustomPacketPayload
    {
        public static final Type<MaterialResult> TYPE = new Type<>(
//#if MC >= 1.21.11
                Identifier.fromNamespaceAndPath("halfmasa", "void_trade_material_result"));
//#else
//$$                 ResourceLocation.fromNamespaceAndPath("halfmasa", "void_trade_material_result"));
//#endif
        public static final StreamCodec<RegistryFriendlyByteBuf, MaterialResult> STREAM_CODEC =
                StreamCodec.of((buffer, result) -> result.write(buffer), MaterialResult::read);

        private void write(RegistryFriendlyByteBuf buffer)
        {
            buffer.writeVarInt(this.requestId);
            buffer.writeBoolean(this.quickShulkerAvailable);
            buffer.writeVarInt(this.emeraldsPulled);
            buffer.writeVarInt(this.emeraldBlocksPulled);
            buffer.writeVarInt(this.emeraldBlocksUncrafted);
        }

        private static MaterialResult read(RegistryFriendlyByteBuf buffer)
        {
            return new MaterialResult(
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }
}
