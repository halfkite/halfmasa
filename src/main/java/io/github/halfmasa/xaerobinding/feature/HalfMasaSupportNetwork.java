package io.github.halfmasa.xaerobinding.feature;
//#if MC >= 1.21.1
import java.lang.reflect.Constructor;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
public final class HalfMasaSupportNetwork {
    private static boolean ready;
    private static Constructor<?> eat, query;
    private HalfMasaSupportNetwork() {}
    public static void initialize() { ClientLifecycleEvents.CLIENT_STARTED.register(client -> register()); }
    @SuppressWarnings({"rawtypes","unchecked"}) private static Constructor<?> type(PayloadTypeRegistry registry,
            CustomPacketPayload.Type id, StreamCodec codec, Class own, String name, Class<?>... parameters) throws ReflectiveOperationException {
        Object existing = registry.getClass().getMethod("get",CustomPacketPayload.Type.class).invoke(registry,id);
        if (existing != null) return Class.forName("carpet.fga.HalfMasaSupportPayloads$" + name).getConstructor(parameters);
        registry.register(id,codec); return own.getConstructor(parameters);
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void register() {
        try {
            eat = type(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), HalfMasaSupportPayloads.Eat.TYPE, HalfMasaSupportPayloads.Eat.CODEC,
                    HalfMasaSupportPayloads.Eat.class,"Eat",int.class,long.class,int.class,ItemStack.class,int.class,boolean.class);
            query = type(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.serverboundPlay(), HalfMasaSupportPayloads.HealthQuery.TYPE, HalfMasaSupportPayloads.HealthQuery.CODEC,
                    HalfMasaSupportPayloads.HealthQuery.class,"HealthQuery",java.util.UUID.class);
            type(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.clientboundPlay(), HalfMasaSupportPayloads.EatResult.TYPE, HalfMasaSupportPayloads.EatResult.CODEC,
                    HalfMasaSupportPayloads.EatResult.class,"EatResult",long.class,String.class);
            type(io.github.halfmasa.xaerobinding.compat.FabricPlayCompat.clientboundPlay(), HalfMasaSupportPayloads.HealthResult.TYPE, HalfMasaSupportPayloads.HealthResult.CODEC,
                    HalfMasaSupportPayloads.HealthResult.class,"HealthResult",java.util.UUID.class,float.class,float.class);
            boolean resultReady = ClientPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type)HalfMasaSupportPayloads.EatResult.TYPE,(payload,context) -> {
                var connection = context.player().connection; var world = context.client().level;
                try {
                    long id = (long)payload.getClass().getMethod("requestId").invoke(payload);
                    String status = (String)payload.getClass().getMethod("status").invoke(payload);
                    context.client().execute(() -> { if (context.client().getConnection() == connection && context.client().level == world) HalfMasaSupportClient.result(id,status); });
                } catch (ReflectiveOperationException exception) { XaeroWorldBinding.LOGGER.warn("Invalid FGA silent food result",exception); }
            });
            boolean healthReady = ClientPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type)HalfMasaSupportPayloads.HealthResult.TYPE,(payload,context) -> {
                var connection = context.player().connection; var world = context.client().level;
                try {
                    var id = (java.util.UUID)payload.getClass().getMethod("bossId").invoke(payload);
                    float current = (float)payload.getClass().getMethod("current").invoke(payload), maximum = (float)payload.getClass().getMethod("maximum").invoke(payload);
                    context.client().execute(() -> { if (context.client().getConnection() == connection && context.client().level == world) HalfMasaSupportClient.healthResult(id,current,maximum); });
                } catch (ReflectiveOperationException exception) { XaeroWorldBinding.LOGGER.warn("Invalid FGA boss health result",exception); }
            });
            ready = resultReady && healthReady;
            if (!ready) XaeroWorldBinding.LOGGER.warn("Halfmasa support channels already have client receivers");
        } catch (ReflectiveOperationException | RuntimeException exception) { XaeroWorldBinding.LOGGER.warn("Cannot register halfmasa support protocol",exception); }
    }
    public static boolean canEat() { return ready && ClientPlayNetworking.canSend(HalfMasaSupportPayloads.Eat.TYPE); }
    public static boolean canHealth() { return ready && ClientPlayNetworking.canSend(HalfMasaSupportPayloads.HealthQuery.TYPE); }
    private static void send(Constructor<?> constructor, Object... args) {
        try { ClientPlayNetworking.send((CustomPacketPayload)constructor.newInstance(args)); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot construct halfmasa support request",exception); }
    }
    public static void eat(HalfMasaSupportPayloads.Eat request) { HalfMasaSupportClient.expectResult(request.requestId()); send(eat,request.version(),request.requestId(),request.slot(),request.expected(),request.target(),request.pauseCombat()); }
    public static void queryHealth(HalfMasaSupportPayloads.HealthQuery request) { send(query,request.bossId()); }
}
//#endif
