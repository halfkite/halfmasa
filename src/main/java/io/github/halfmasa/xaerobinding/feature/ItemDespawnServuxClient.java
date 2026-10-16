package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
//#if MC >= 1.21.11
import net.minecraft.nbt.NbtIo;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
//#endif
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.FabricPlayCompat;

/** Servux entity_data v2. Ordinary replies use length-prefixed MaLiLib data, not raw NBT. */
public final class ItemDespawnServuxClient
{
    //#if MC >= 1.21.11
    private static final int PROTOCOL_VERSION = 2;
    //#else
    //$$ private static final int PROTOCOL_VERSION = 1;
    //#endif
    private static final int HELLO_INTERVAL = 100;
    private static final long MAX_ENTITY_DATA_BYTES = 2 * 1024 * 1024L;
    private static boolean ready;
    private static boolean connected;
    private static boolean warnedPacket;
    private static ClientPacketListener connection;
    private static long lastHello = Long.MIN_VALUE;

    private ItemDespawnServuxClient() {}

    public static void initialize()
    {
        // Wait for all mod initialization, so a pre-existing codec/receiver is visible.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> register());
    }

    private static void register()
    {
        try
        {
            var inbound = FabricPlayCompat.clientboundPlay();
            var outbound = FabricPlayCompat.serverboundPlay();
            if (hasCodec(inbound) || hasCodec(outbound) || ClientPlayNetworking.getGlobalReceivers().contains(Payload.TYPE.id()))
            {
                XaeroWorldBinding.LOGGER.warn("Item despawn timer: servux:entity_data is owned by another mod; using fallback");
                return;
            }
            inbound.register(Payload.TYPE, Payload.CODEC);
            outbound.register(Payload.TYPE, Payload.CODEC);
            ready = ClientPlayNetworking.registerGlobalReceiver(Payload.TYPE, (packet, context) -> {
                var client = context.client();
                var sender = client.getConnection();
                long generation = ItemDespawnTimerService.getInstance().generation();
                client.execute(() -> {
                    if (client.getConnection() == sender && generation == ItemDespawnTimerService.getInstance().generation()) receive(packet);
                });
            });
        }
        catch (ReflectiveOperationException e)
        {
            XaeroWorldBinding.LOGGER.warn("Item despawn timer: cannot inspect Fabric channel registration; using fallback", e);
        }
    }

    private static boolean hasCodec(PayloadTypeRegistry<?> registry) throws ReflectiveOperationException
    {
        return registry.getClass().getMethod("get", CustomPacketPayload.Type.class).invoke(registry, Payload.TYPE) != null;
    }

    public static void tick(Minecraft client, long tick)
    {
        if (!ready) return;
        if (connection != client.getConnection())
        {
            connection = client.getConnection();
            connected = false;
            warnedPacket = false;
            lastHello = Long.MIN_VALUE;
        }
        if (connection == null || connected || !ClientPlayNetworking.canSend(Payload.TYPE)) return;
        if (lastHello != Long.MIN_VALUE && tick - lastHello < HELLO_INTERVAL) return;
        CompoundTag metadata = new CompoundTag();
        metadata.putInt("version", PROTOCOL_VERSION);
        ClientPlayNetworking.send(new Payload(2, -1, metadata, null, null));
        lastHello = tick;
    }

    public static boolean request(int id)
    {
        if (!ready || !connected || connection != Minecraft.getInstance().getConnection() || !ClientPlayNetworking.canSend(Payload.TYPE)) return false;
        ClientPlayNetworking.send(new Payload(4, id, null, null, null));
        return true;
    }

    private static void receive(Payload packet)
    {
        if (packet.problem != null)
        {
            if (!warnedPacket)
            {
                warnedPacket = true;
                XaeroWorldBinding.LOGGER.warn("Item despawn timer: ignoring Servux packet: {}. Missing Age samples use fallback.", packet.problem);
            }
            return;
        }
        if (packet.kind == 1 && packet.metadata != null)
        {
            connected = CageNbt.integer(packet.metadata, "version", -1) == PROTOCOL_VERSION
                    && CageNbt.string(packet.metadata, "servux", "").startsWith("servux-");
        }
        else if (packet.kind == 6 && connected && packet.data != null)
            ItemDespawnDataBridge.receive(packet.entityId, packet.data);
    }

    //#if MC >= 1.21.11
    public record Payload(int kind, int entityId, CompoundTag metadata, CompoundData data, String problem) implements CustomPacketPayload
    //#else
    //$$ public record Payload(int kind, int entityId, CompoundTag metadata, CompoundTag data, String problem) implements CustomPacketPayload
    //#endif
    {
        //#if MC >= 1.21.11
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("servux", "entity_data"));
        //#else
        //$$ public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("servux", "entity_data"));
        //#endif
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec(Payload::write, Payload::decode);

        private static Payload invalid(FriendlyByteBuf input, String reason)
        {
            input.skipBytes(input.readableBytes());
            return new Payload(-1, -1, null, null, reason);
        }

        private static Integer readVarInt(FriendlyByteBuf input)
        {
            int value = 0;
            for (int part = 0; part < 5 && input.isReadable(); part++)
            {
                int next = input.readUnsignedByte();
                if (part == 4 && (next & 0xF0) != 0) return null;
                value |= (next & 0x7F) << (part * 7);
                if ((next & 0x80) == 0) return value;
            }
            return null;
        }

        private static Payload decode(FriendlyByteBuf input)
        {
            Integer kind = readVarInt(input);
            if (kind == null) return invalid(input, "invalid packet type VarInt");
            if (kind == 1)
            {
                // Explicitly bound untrusted metadata; decoding failures are surfaced to the receiver.
                try
                {
                    net.minecraft.nbt.Tag tag = input.readNbt(NbtAccounter.create(1024 * 1024L));
                    if (tag instanceof CompoundTag metadata && !input.isReadable()) return new Payload(kind, -1, metadata, null, null);
                }
                catch (RuntimeException e) { return invalid(input, "invalid metadata: " + e.getClass().getSimpleName()); }
                return invalid(input, "invalid metadata compound or trailing data");
            }
            if (kind == 5 || kind == 6)
            {
                Integer id = -1;
                if (kind == 6) id = readVarInt(input);
                else
                {
                    if (input.readableBytes() < Long.BYTES) return invalid(input, "truncated block position");
                    input.readLong(); // Other users of the channel may request block entities.
                }
                if (id == null || (kind == 6 && id < 0)) return invalid(input, "invalid entity ID");
                //#if MC >= 1.21.11
                // Protocol v2 existed before MaLiLib added its length-prefixed GZIP envelope.
                // Both variants contain a named vanilla NBT compound; decode with vanilla's size/depth limits.
                if (!input.isReadable()) return invalid(input, "truncated entity reply");
                boolean named = input.getUnsignedByte(input.readerIndex()) == net.minecraft.nbt.Tag.TAG_COMPOUND;
                int length;
                if (named) length = input.readableBytes();
                else
                {
                    if (input.readableBytes() < Integer.BYTES) return invalid(input, "truncated entity reply");
                    length = input.readInt();
                    if (length <= 0 || length != input.readableBytes()) return invalid(input, "invalid entity data length");
                }
                if (length > MAX_ENTITY_DATA_BYTES) return invalid(input, "entity data exceeds size limit");
                byte[] bytes = new byte[length];
                input.readBytes(bytes);
                try
                {
                    var raw = new ByteArrayInputStream(bytes);
                    boolean gzip = bytes.length >= 2 && (bytes[0] & 255) == 0x1F && (bytes[1] & 255) == 0x8B;
                    try (var decoded = new DataInputStream(gzip ? new GZIPInputStream(raw) : raw))
                    {
                        CompoundTag data = NbtIo.read(decoded, NbtAccounter.create(MAX_ENTITY_DATA_BYTES));
                        if (data != null && decoded.read() == -1) return new Payload(kind, id, null, DataConverterNbt.fromVanillaCompound(data), null);
                    }
                }
                catch (IOException | RuntimeException e) { return invalid(input, "invalid entity data: " + e.getClass().getSimpleName()); }
                return invalid(input, "invalid entity compound or trailing data");
                //#else
                //$$ try
                //$$ {
                //$$     net.minecraft.nbt.Tag tag = input.readNbt(NbtAccounter.create(2 * 1024 * 1024L));
                //$$     if (tag instanceof CompoundTag data && !input.isReadable()) return new Payload(kind, id, null, data, null);
                //$$ }
                //$$ catch (RuntimeException e) { return invalid(input, "invalid entity NBT: " + e.getClass().getSimpleName()); }
                //$$ return invalid(input, "invalid entity NBT compound or trailing data");
                //#endif
            }
            // Upstream's current entity handler sends simple replies. Never reinterpret splitter bytes as Age.
            return invalid(input, "unsupported packet type " + kind);
        }

        private void write(FriendlyByteBuf output)
        {
            output.writeVarInt(this.kind);
            if (this.kind == 2) output.writeNbt(this.metadata);
            else if (this.kind == 4) output.writeVarInt(this.entityId);
        }

        @Override public Type<Payload> type() { return TYPE; }
    }
}
//#endif
