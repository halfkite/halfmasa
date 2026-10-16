package io.github.halfmasa.xaerobinding.feature;
//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
public final class HalfMasaSupportClient {
    private record HealthCache(BossBarHealth.Health value, long time) {}
    private static final Map<UUID,HealthCache> HEALTH = new HashMap<>();
    private static final Map<UUID,Long> QUERIES = new HashMap<>();
    private static Object connection, world, actor;
    private static long sequence;
    private static final Map<Long,String> RESULTS = new HashMap<>();
    private static final java.util.Set<Long> AWAITING = new java.util.HashSet<>();
    private HalfMasaSupportClient() {}
    public static void clear() { HEALTH.clear(); QUERIES.clear(); RESULTS.clear(); AWAITING.clear(); connection = null; world = null; actor = null; }
    private static Minecraft bind() {
        var client = Minecraft.getInstance();
        if (connection != client.getConnection() || world != client.level || actor != client.player) {
            clear(); connection = client.getConnection(); world = client.level; actor = client.player;
        }
        return client;
    }
    private static boolean api() { return FabricLoader.getInstance().isModLoaded("fabric-api"); }
    public static boolean canEat() { var c = bind(); return c.getSingleplayerServer() != null || api() && HalfMasaSupportNetwork.canEat(); }
    public static long eat(int slot, ItemStack expected, int target, boolean pauseCombat) {
        var client = bind(); long id = ++sequence;
        expectResult(id);
        var request = new HalfMasaSupportPayloads.Eat(1,id,slot,expected.copy(),target,pauseCombat);
        Object originConnection = connection, originWorld = world;
        var server = client.getSingleplayerServer();
        if (server != null) {
            UUID playerId = client.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                String status = player == null ? "DISCONNECTED" : SilentFoodTransaction.consume(player,slot,request.expected(),target,pauseCombat);
                client.execute(() -> { if (client.getConnection() == originConnection && client.level == originWorld) result(id,status); });
            });
        } else HalfMasaSupportNetwork.eat(request);
        return id;
    }
    public static void expectResult(long id) {
        bind();
        if (!AWAITING.contains(id) && AWAITING.size() >= 64) throw new IllegalStateException("Too many outstanding support requests");
        AWAITING.add(id); RESULTS.remove(id);
    }
    public static void result(long id, String status) { bind(); if (AWAITING.remove(id)) RESULTS.put(id,status); }
    public static String takeResult(long id) { bind(); return RESULTS.remove(id); }
    public static void healthResult(UUID id, float current, float maximum) {
        var client = bind(); if (client.level == null) return;
        if (HEALTH.size() >= 128) HEALTH.entrySet().removeIf(entry -> client.level.getGameTime() - entry.getValue().time > 30);
        if (HEALTH.size() < 128) HEALTH.put(id,new HealthCache(new BossBarHealth.Health(current,maximum),client.level.getGameTime()));
    }
    public static BossBarHealth.Health health(UUID id) {
        var client = bind(); var cached = HEALTH.get(id);
        return cached != null && client.level != null && client.level.getGameTime() - cached.time <= 30 ? cached.value : null;
    }
    public static void queryHealth(UUID id) {
        var client = bind(); if (client.level == null || client.player == null) return;
        long tick = client.level.getGameTime();
        if (QUERIES.getOrDefault(id,Long.MIN_VALUE / 2) + 20 > tick) return;
        if (QUERIES.size() >= 128) QUERIES.entrySet().removeIf(entry -> tick - entry.getValue() > 40);
        if (QUERIES.size() >= 128) return;
        var server = client.getSingleplayerServer();
        Object originConnection = connection, originWorld = world;
        if (server != null) {
            QUERIES.put(id,tick); UUID playerId = client.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId); if (player == null) return;
                var reply = BossServerHealth.find(player,id);
                client.execute(() -> { if (client.getConnection() == originConnection && client.level == originWorld)
                    healthResult(reply.bossId(),reply.current(),reply.maximum()); });
            });
        } else if (api() && HalfMasaSupportNetwork.canHealth()) {
            QUERIES.put(id,tick); HalfMasaSupportNetwork.queryHealth(new HalfMasaSupportPayloads.HealthQuery(id));
        }
    }
}
//#endif
