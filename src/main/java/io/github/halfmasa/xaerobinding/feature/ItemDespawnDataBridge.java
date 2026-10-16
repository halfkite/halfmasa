package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.entity.item.ItemEntity;
import fi.dy.masa.malilib.interfaces.IDataSyncer;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.util.data.tag.CompoundData;
//#endif
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.ItemDespawnDataSource;

/** Optional MiniHUD linkage stays outside the timer and never claims its network channel. */
public final class ItemDespawnDataBridge
{
    public record CachedAge(int age, long elapsedTicks, Object token) {}
    private static final boolean HAS_MINIHUD = FabricLoader.getInstance().isModLoaded("minihud");
    private static boolean checkedMiniHud;
    private static IDataSyncer miniHud;
    private static Method hasServux;
    private static boolean networkAvailable;
    //#if MC < 1.21.11
    //$$ private static ConfigBoolean legacySync;
    //$$ private static Collection<Integer> legacyRequests;
    //$$ private record LegacyAge(int age, long time, ItemEntity entity, Object token) {}
    //$$ private static final Map<UUID, LegacyAge> legacySamples = new HashMap<>();
    //#endif

    private ItemDespawnDataBridge() {}

    public static void initialize()
    {
        var loader = FabricLoader.getInstance();
        networkAvailable = !HAS_MINIHUD && loader.isModLoaded("fabric-networking-api-v1")
                && loader.isModLoaded("fabric-lifecycle-events-v1");
        if (networkAvailable) ItemDespawnServuxClient.initialize();
    }

    @SuppressWarnings("unchecked")
    private static boolean miniHudReady()
    {
        if (!HAS_MINIHUD) return false;
        if (!checkedMiniHud)
        {
            checkedMiniHud = true;
            try
            {
                //#if MC >= 1.21.11
                var type = Class.forName("fi.dy.masa.minihud.data.EntityDataManager");
                //#else
                //$$ var type = Class.forName("fi.dy.masa.minihud.data.EntitiesDataManager");
                //#endif
                miniHud = (IDataSyncer) type.getMethod("getInstance").invoke(null);
                hasServux = type.getMethod("hasServuxServer");
                //#if MC < 1.21.11
                //$$ legacySync = (ConfigBoolean) Class.forName("fi.dy.masa.minihud.config.Configs$Generic").getField("ENTITY_DATA_SYNC").get(null);
                //$$ var field = type.getDeclaredField("pendingEntitiesQueue");
                //$$ field.setAccessible(true);
                //$$ legacyRequests = (Collection<Integer>) field.get(miniHud);
                //#endif
            }
            catch (ReflectiveOperationException | LinkageError e)
            {
                miniHud = null;
                XaeroWorldBinding.LOGGER.warn("Item despawn timer: MiniHUD data interface is unavailable; using fallback", e);
            }
        }
        //#if MC >= 1.21.11
        if (miniHud == null || !miniHud.isEnabled()) return false;
        //#else
        //$$ if (miniHud == null || !legacySync.getBooleanValue()) return false;
        //#endif
        try { return Boolean.TRUE.equals(hasServux.invoke(miniHud)); }
        catch (ReflectiveOperationException e)
        {
            XaeroWorldBinding.LOGGER.warn("Item despawn timer: failed to read MiniHUD Servux state; disabling this adapter", e);
            miniHud = null;
            return false;
        }
    }

    public static void tick(Minecraft client, long tick)
    {
        if (networkAvailable) ItemDespawnServuxClient.tick(client, tick);
        //#if MC < 1.21.11
        //$$ long now = System.currentTimeMillis();
        //$$ legacySamples.values().removeIf(sample -> sample.entity().level() != client.level || now < sample.time() || now - sample.time() > 30000L);
        //#endif
    }

    public static void clear()
    {
        //#if MC < 1.21.11
        //$$ legacySamples.clear();
        //#endif
    }

    public static CachedAge pollMiniHud(ItemEntity item)
    {
        if (!miniHudReady()) return null;
        //#if MC >= 1.21.11
        var pair = miniHud.getCache().getEntityPairFromCache(item.getId());
        if (pair == null || pair.ent() != item || !matchesUuid(pair.data(), item.getUUID())) return null;
        Integer age = readAge(pair.data());
        if (age == null) return null;
        long elapsed = System.currentTimeMillis() - pair.time();
        if (elapsed < 0) return null; // Wall-clock rollback must not make an old sample look fresh.
        return new CachedAge(age, elapsed / 50L, pair.data());
        //#else
        //$$ var sample = legacySamples.get(item.getUUID());
        //$$ if (sample == null || sample.entity() != item) return null;
        //$$ long elapsed = System.currentTimeMillis() - sample.time();
        //$$ return elapsed < 0 ? null : new CachedAge(sample.age(), elapsed / 50L, sample.token());
        //#endif
    }

    public static boolean request(Minecraft client, ItemEntity item, ItemDespawnDataSource source, long generation)
    {
        var server = client.getSingleplayerServer();
        if (source == ItemDespawnDataSource.AUTO && server != null && client.level != null)
        {
            var dimension = client.level.dimension();
            UUID uuid = item.getUUID();
            int id = item.getId();
            server.execute(() -> {
                var level = server.getLevel(dimension);
                var entity = level == null ? null : level.getEntity(uuid);
                if (entity instanceof ItemEntity serverItem)
                {
                    int age = serverItem.getAge();
                    client.execute(() -> ItemDespawnTimerService.getInstance().accept(generation, id, uuid, age));
                }
            });
            return true;
        }
        if (HAS_MINIHUD)
        {
            if (!miniHudReady()) return false;
            //#if MC >= 1.21.11
            var requests = miniHud.getRequestTracker();
            if (!requests.hasScheduledEntity(item.getId())) requests.schedulePendingEntity(item.getId());
            //#else
            //$$ // Calling requestEntity() here can fabricate a client-side Age cache entry in old MiniHUD.
            //$$ // Enqueue directly and consume only its actual server-reply hook instead.
            //$$ if (!legacyRequests.contains(item.getId())) legacyRequests.add(item.getId());
            //#endif
            return true;
        }
        return networkAvailable && ItemDespawnServuxClient.request(item.getId());
    }

    //#if MC >= 1.21.11
    static Integer readAge(CompoundData data)
    {
        var number = data.getData("Age").flatMap(value -> value.asNumber());
        if (number.isEmpty()) return null;
        double age = number.get().doubleValue();
        return Double.isFinite(age) && age == Math.rint(age) && age >= Short.MIN_VALUE && age <= Short.MAX_VALUE
                ? (int) age : null;
    }

    static boolean matchesUuid(CompoundData data, UUID uuid)
    {
        return data.getCodec("UUID", UUIDUtil.CODEC).filter(uuid::equals).isPresent();
    }

    public static void receive(int entityId, CompoundData data)
    {
        var client = Minecraft.getInstance();
        if (client.level == null || data == null) return;
        var entity = client.level.getEntity(entityId);
        if (!(entity instanceof ItemEntity item) || !matchesUuid(data, item.getUUID())) return;
        Integer age = readAge(data);
        if (age != null) ItemDespawnTimerService.getInstance().accept(
                ItemDespawnTimerService.getInstance().generation(), entityId, item.getUUID(), age);
    }
    //#endif

    static Integer readAge(CompoundTag data)
    {
        var tag = data.get("Age");
        if (!(tag instanceof ByteTag || tag instanceof ShortTag || tag instanceof IntTag || tag instanceof LongTag)) return null;
        long age = CageNbt.longValue(data, "Age", Long.MAX_VALUE);
        return age >= Short.MIN_VALUE && age <= Short.MAX_VALUE ? (int) age : null;
    }

    static boolean matchesUuid(CompoundTag data, UUID uuid)
    {
        var tag = data.get("UUID");
        return tag != null && UUIDUtil.CODEC.parse(NbtOps.INSTANCE, tag).result().filter(uuid::equals).isPresent();
    }

    public static void receive(int entityId, CompoundTag data)
    {
        var client = Minecraft.getInstance();
        if (client.level == null || data == null) return;
        var entity = client.level.getEntity(entityId);
        if (!(entity instanceof ItemEntity item) || !matchesUuid(data, item.getUUID())) return;
        Integer age = readAge(data);
        if (age != null) ItemDespawnTimerService.getInstance().accept(
                ItemDespawnTimerService.getInstance().generation(), entityId, item.getUUID(), age);
    }

    //#if MC < 1.21.11
    //$$ public static void receiveLegacyMiniHud(int entityId, CompoundTag data)
    //$$ {
    //$$     var client = Minecraft.getInstance();
    //$$     var sender = client.getConnection();
    //$$     long generation = ItemDespawnTimerService.getInstance().generation();
    //$$     CompoundTag copy = data.copy();
    //$$     client.execute(() -> {
    //$$         if (!io.github.halfmasa.xaerobinding.config.Configs.ITEM_DESPAWN_TIMER.getBooleanValue()
    //$$                 || client.level == null || client.getConnection() != sender
    //$$                 || generation != ItemDespawnTimerService.getInstance().generation() || !miniHudReady()) return;
    //$$         var entity = client.level.getEntity(entityId);
    //$$         if (!(entity instanceof ItemEntity item) || !matchesUuid(copy, item.getUUID())) return;
    //$$         Integer age = readAge(copy);
    //$$         if (age != null) legacySamples.put(item.getUUID(), new LegacyAge(age, System.currentTimeMillis(), item, copy));
    //$$     });
    //$$ }
    //#endif
}
//#endif
