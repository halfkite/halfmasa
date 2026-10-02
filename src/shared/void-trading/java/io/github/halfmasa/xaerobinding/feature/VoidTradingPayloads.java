//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
                Identifier.fromNamespaceAndPath("halfmasa", "void_trade_material_request"));
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
                Identifier.fromNamespaceAndPath("halfmasa", "void_trade_material_result"));
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

//#endif
