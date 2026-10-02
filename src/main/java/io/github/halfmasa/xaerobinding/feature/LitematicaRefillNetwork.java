package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Constructor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

/** Wire-compatible FGA v1 client; no local FGA dependency or client inventory writes. */
public final class LitematicaRefillNetwork
{
    private static boolean ready;
    private static Constructor<?> queryConstructor;
    private static Constructor<?> takeConstructor;
    private static Constructor<?> directTakeConstructor;
    private static Constructor<?> silentTakeConstructor;
    private static Constructor<?> amountQueryConstructor;
    private static Constructor<?> amountTakeConstructor;

    private LitematicaRefillNetwork() {}

    public static void initialize()
    {
        // FGA registers its DTOs during main initialization. Reuse them after all entrypoints run.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> register());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register()
    {
        try
        {
            queryConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), Query.TYPE,
                    Query.CODEC, Query.class, "Query",
                    int.class, long.class, int.class, String.class, int.class);
            takeConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), Take.TYPE,
                    Take.CODEC, Take.class, "Take", int.class, long.class, String.class, String.class);
            directTakeConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), DirectTake.TYPE,
                    DirectTake.CODEC, DirectTake.class, "DirectTake", int.class, long.class, String.class);
            silentTakeConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), SilentTake.TYPE,
                    SilentTake.CODEC, SilentTake.class, "SilentTake", int.class, long.class, String.class, String.class);
            amountQueryConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), AmountQuery.TYPE,
                    AmountQuery.CODEC, AmountQuery.class, "AmountQuery", int.class, long.class, String.class, int.class, int.class);
            amountTakeConstructor = registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), AmountTake.TYPE,
                    AmountTake.CODEC, AmountTake.class, "AmountTake", int.class, long.class, String.class, String.class, int.class, boolean.class);
            registerType(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.clientboundPlay(), Reply.TYPE,
                    Reply.CODEC, Reply.class, "Reply", long.class, String.class);
            ready = ClientPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type) Reply.TYPE,
                    (payload, context) -> {
                        try
                        {
                            long id = (long) payload.getClass().getMethod("requestId").invoke(payload);
                            String json = (String) payload.getClass().getMethod("json").invoke(payload);
                            // Capture the connection, so a queued reply cannot cross reconnects.
                            var connection = context.player().connection;
                            context.client().execute(() -> LitematicaMaterialRefill.getInstance()
                                    .onReply(connection, id, json));
                        }
                        catch (ReflectiveOperationException e)
                        {
                            XaeroWorldBinding.LOGGER.warn("Cannot decode FGA inventory reply", e);
                        }
                    });
            if (!ready) XaeroWorldBinding.LOGGER.warn("FGA inventory reply channel already has a client receiver");
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            ready = false;
            XaeroWorldBinding.LOGGER.warn("Cannot initialize Litematica FGA refill protocol", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Constructor<?> registerType(PayloadTypeRegistry registry, CustomPacketPayload.Type<?> type,
            StreamCodec codec, Class<?> ownClass, String name, Class<?>... parameters)
            throws ReflectiveOperationException
    {
        // Fabric currently exposes registry lookup through its implementation, not the public interface.
        Object existing = registry.getClass().getMethod("get", CustomPacketPayload.Type.class).invoke(registry, type);
        if (existing != null)
        {
            return Class.forName("carpet.fga.PlayerSortInventoryPayloads$" + name).getConstructor(parameters);
        }
        registry.register(type, codec);
        return ownClass.getConstructor(parameters);
    }

    public static boolean isSupported()
    {
        return ready && ClientPlayNetworking.canSend(Query.TYPE) && ClientPlayNetworking.canSend(Take.TYPE);
    }

    public static void query(long id, String itemId, int cursor)
    {
        send(queryConstructor, 1, id, 2, itemId, cursor);
    }

    public static boolean supportsDirectTake()
    {
        return ready && ClientPlayNetworking.canSend(DirectTake.TYPE);
    }

    public static void directTake(long id, String itemId)
    {
        send(directTakeConstructor, 1, id, itemId);
    }

    public static boolean supportsSilentTake()
    {
        return ready && ClientPlayNetworking.canSend(SilentTake.TYPE);
    }

    public static void silentTake(long id, String token, String itemId)
    {
        send(silentTakeConstructor, 1, id, token, itemId);
    }

    public static void take(long id, String token, String itemId)
    {
        send(takeConstructor, 1, id, token, itemId);
    }

    public static boolean supportsAmount()
    {
        return ready && ClientPlayNetworking.canSend(AmountQuery.TYPE) && ClientPlayNetworking.canSend(AmountTake.TYPE);
    }
    public static void queryAmount(long id, String itemId, int cursor, int amount)
    {
        send(amountQueryConstructor, 1, id, itemId, cursor, amount);
    }
    public static void takeAmount(long id, String token, String itemId, int amount, boolean silent)
    {
        send(amountTakeConstructor, 1, id, token, itemId, amount, silent);
    }

    private static void send(Constructor<?> constructor, Object... args)
    {
        try
        {
            ClientPlayNetworking.send((CustomPacketPayload) constructor.newInstance(args));
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Cannot construct FGA inventory request", e);
        }
    }

    public record Query(int version, long requestId, int kind, String target, int cursor) implements CustomPacketPayload
    {
        public static final Type<Query> TYPE = LitematicaRefillNetwork.type("query");
        public static final StreamCodec<FriendlyByteBuf, Query> CODEC = CustomPacketPayload.codec(Query::write, Query::new);
        public Query(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readVarInt(), b.readUtf(256), b.readVarInt()); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeVarInt(kind); b.writeUtf(target, 256); b.writeVarInt(cursor); }
        @Override public Type<Query> type() { return TYPE; }
    }

    public record Take(int version, long requestId, String token, String itemId) implements CustomPacketPayload
    {
        public static final Type<Take> TYPE = LitematicaRefillNetwork.type("take");
        public static final StreamCodec<FriendlyByteBuf, Take> CODEC = CustomPacketPayload.codec(Take::write, Take::new);
        public Take(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readUtf(64), b.readUtf(256)); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeUtf(token, 64); b.writeUtf(itemId, 256); }
        @Override public Type<Take> type() { return TYPE; }
    }

    public record DirectTake(int version, long requestId, String itemId) implements CustomPacketPayload
    {
        public static final Type<DirectTake> TYPE = LitematicaRefillNetwork.type("direct_take");
        public static final StreamCodec<FriendlyByteBuf, DirectTake> CODEC = CustomPacketPayload.codec(DirectTake::write, DirectTake::new);
        public DirectTake(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readUtf(256)); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeUtf(itemId, 256); }
        @Override public Type<DirectTake> type() { return TYPE; }
    }

    public record SilentTake(int version, long requestId, String token, String itemId) implements CustomPacketPayload
    {
        public static final Type<SilentTake> TYPE = LitematicaRefillNetwork.type("silent_take");
        public static final StreamCodec<FriendlyByteBuf, SilentTake> CODEC = CustomPacketPayload.codec(SilentTake::write, SilentTake::new);
        public SilentTake(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readUtf(64), b.readUtf(256)); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeUtf(token, 64); b.writeUtf(itemId, 256); }
        @Override public Type<SilentTake> type() { return TYPE; }
    }

    public record AmountQuery(int version, long requestId, String itemId, int cursor, int amount) implements CustomPacketPayload
    {
        public static final Type<AmountQuery> TYPE = LitematicaRefillNetwork.type("query_amount");
        public static final StreamCodec<FriendlyByteBuf, AmountQuery> CODEC = CustomPacketPayload.codec(AmountQuery::write, AmountQuery::new);
        public AmountQuery(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readUtf(256), b.readVarInt(), b.readVarInt()); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeUtf(itemId, 256); b.writeVarInt(cursor); b.writeVarInt(amount); }
        @Override public Type<AmountQuery> type() { return TYPE; }
    }
    public record AmountTake(int version, long requestId, String token, String itemId, int amount, boolean silent) implements CustomPacketPayload
    {
        public static final Type<AmountTake> TYPE = LitematicaRefillNetwork.type("take_amount");
        public static final StreamCodec<FriendlyByteBuf, AmountTake> CODEC = CustomPacketPayload.codec(AmountTake::write, AmountTake::new);
        public AmountTake(FriendlyByteBuf b) { this(b.readVarInt(), b.readLong(), b.readUtf(64), b.readUtf(256), b.readVarInt(), b.readBoolean()); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(version); b.writeLong(requestId); b.writeUtf(token, 64); b.writeUtf(itemId, 256); b.writeVarInt(amount); b.writeBoolean(silent); }
        @Override public Type<AmountTake> type() { return TYPE; }
    }

    public record Reply(long requestId, String json) implements CustomPacketPayload
    {
        public static final Type<Reply> TYPE = LitematicaRefillNetwork.type("reply");
        public static final StreamCodec<FriendlyByteBuf, Reply> CODEC = CustomPacketPayload.codec(Reply::write, Reply::new);
        public Reply(FriendlyByteBuf b) { this(b.readLong(), b.readUtf(8192)); }
        private void write(FriendlyByteBuf b) { b.writeLong(requestId); b.writeUtf(json, 8192); }
        @Override public Type<Reply> type() { return TYPE; }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path)
    {
        //#if MC >= 1.21.11
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("carpet-fga-addition", "playersort/" + path));
        //#else
        //$$ return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("carpet-fga-addition", "playersort/" + path));
        //#endif
    }
}
