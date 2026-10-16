package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ItemDespawnDataSource;
import io.github.halfmasa.xaerobinding.config.ItemDespawnTimeFormat;

/** Owns client-thread tracking independently of aggregation and entity highlighting. */
public final class ItemDespawnTimerService implements IClientTickHandler
{
    public static final int REQUESTS_PER_TICK = 4;
    private static final ItemDespawnTimerService INSTANCE = new ItemDespawnTimerService();
    private final Map<UUID, Tracked> tracked = new HashMap<>();
    private Map<UUID, Component> labels = Map.of();
    private ClientLevel world;
    private ItemDespawnDataSource source;
    private long tick;
    private long generation;

    private static final class Tracked
    {
        final ItemEntity entity;
        final ItemDespawnAgeModel age;
        long lastRequest = Long.MIN_VALUE;
        Long pendingAt;
        Object cacheToken;
        Tracked(ItemEntity entity, long tick)
        {
            this.entity = entity;
            this.age = new ItemDespawnAgeModel(tick);
        }
    }

    private ItemDespawnTimerService() {}
    public static ItemDespawnTimerService getInstance() { return INSTANCE; }
    public long generation() { return this.generation; }

    public void clear()
    {
        this.generation++;
        this.tracked.clear();
        this.labels = Map.of();
        this.world = null;
        this.source = null;
        ItemDespawnDataBridge.clear();
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!Configs.ITEM_DESPAWN_TIMER.getBooleanValue() || client.level == null)
        {
            if (this.world != null) clear();
            return;
        }
        ItemDespawnDataSource selected = (ItemDespawnDataSource) Configs.ITEM_DESPAWN_SOURCE.getOptionListValue();
        if (this.world != client.level || this.source != selected)
        {
            clear();
            this.world = client.level;
            this.source = selected;
        }
        if (!client.isPaused()) this.tick++;
        Entity camera = client.getCameraEntity();
        if (camera == null) { this.labels = Map.of(); return; }

        Map<Integer, ItemEntity> loaded = new HashMap<>();
        List<ItemEntity> candidates = new ArrayList<>();
        double range = Configs.ITEM_DESPAWN_DISTANCE.getIntegerValue();
        var aggregation = EntityRenderAggregation.getInstance();
        for (Entity entity : this.world.entitiesForRendering())
        {
            if (!(entity instanceof ItemEntity item) || item.isRemoved() || item.getItem().isEmpty()) continue;
            loaded.put(item.getId(), item);
            Tracked old = this.tracked.get(item.getUUID());
            if (old == null || old.entity != item) this.tracked.put(item.getUUID(), new Tracked(item, this.tick));
            if (item.distanceToSqr(camera) <= range * range && !aggregation.shouldHide(item)) candidates.add(item);
        }
        this.tracked.values().removeIf(entry -> loaded.get(entry.entity.getId()) != entry.entity);
        candidates.sort(Comparator.comparingDouble((ItemEntity item) -> item.distanceToSqr(camera))
                .thenComparingInt(Entity::getId));
        List<ItemEntity> anchors = candidates.subList(0, Math.min(candidates.size(), Configs.ITEM_DESPAWN_MAX_LABELS.getIntegerValue()));
        Set<Integer> needed = new LinkedHashSet<>();
        for (ItemEntity anchor : anchors) needed.addAll(aggregation.getItemMemberIds(anchor));

        int interval = Configs.ITEM_DESPAWN_SYNC_INTERVAL.getIntegerValue();
        long timeout = Math.max(60L, Math.max(interval * 2L, ((needed.size() + REQUESTS_PER_TICK - 1L) / REQUESTS_PER_TICK) * 2L));
        long freshness = Math.max(interval * 3L, timeout);
        if (this.source != ItemDespawnDataSource.ESTIMATE && !client.isPaused())
        {
            ItemDespawnDataBridge.tick(client, this.tick);
            List<Tracked> due = new ArrayList<>();
            for (int id : needed)
            {
                ItemEntity item = loaded.get(id);
                if (item == null) continue;
                Tracked entry = this.tracked.get(item.getUUID());
                var cached = ItemDespawnDataBridge.pollMiniHud(item);
                if (cached != null && cached.token() != entry.cacheToken)
                {
                    entry.cacheToken = cached.token();
                    entry.age.accept(cached.age(), this.tick - cached.elapsedTicks());
                    entry.pendingAt = null;
                }
                if (entry.pendingAt != null && this.tick - entry.pendingAt > timeout) entry.pendingAt = null;
                if (entry.pendingAt == null && (entry.lastRequest == Long.MIN_VALUE || this.tick - entry.lastRequest >= interval)) due.add(entry);
            }
            // Distance decides the displayed anchors, not which members ever receive a refresh.
            due.sort(Comparator.comparingLong((Tracked entry) -> entry.lastRequest).thenComparingInt(entry -> entry.entity.getId()));
            int sent = 0;
            for (Tracked entry : due)
            {
                if (sent == REQUESTS_PER_TICK) break;
                entry.pendingAt = this.tick; // Register before a local-server callback can return.
                if (ItemDespawnDataBridge.request(client, entry.entity, this.source, this.generation))
                {
                    entry.lastRequest = this.tick;
                    sent++;
                }
                else entry.pendingAt = null;
            }
        }

        boolean estimateOnly = this.source == ItemDespawnDataSource.ESTIMATE;
        boolean requireSample = this.source == ItemDespawnDataSource.SERVUX;
        String format = ((ItemDespawnTimeFormat) Configs.ITEM_DESPAWN_FORMAT.getOptionListValue()).getStringValue();
        Map<UUID, Component> next = new HashMap<>();
        for (ItemEntity anchor : anchors)
        {
            List<ItemDespawnAgeModel.Reading> readings = new ArrayList<>();
            for (int id : aggregation.getItemMemberIds(anchor))
            {
                ItemEntity item = loaded.get(id);
                Tracked entry = item == null ? null : this.tracked.get(item.getUUID());
                readings.add(entry == null ? new ItemDespawnAgeModel.Reading(0, ItemDespawnAgeModel.Quality.UNKNOWN)
                        : entry.age.read(this.tick, freshness, estimateOnly, requireSample));
            }
            next.put(anchor.getUUID(), Component.literal(ItemDespawnAgeModel.format(
                    ItemDespawnAgeModel.combine(readings, requireSample), format)));
        }
        this.labels = Map.copyOf(next);
    }

    public void accept(long generation, int entityId, UUID uuid, int age)
    {
        if (generation != this.generation || this.world == null || !Configs.ITEM_DESPAWN_TIMER.getBooleanValue()) return;
        Tracked entry = this.tracked.get(uuid);
        if (entry == null || entry.pendingAt == null || this.world.getEntity(entityId) != entry.entity || entry.entity.getId() != entityId) return;
        entry.age.accept(age, this.tick);
        entry.pendingAt = null;
    }

    public Component labelFor(ItemEntity item)
    {
        return Configs.ITEM_DESPAWN_TIMER.getBooleanValue() && Minecraft.getInstance().level == this.world
                ? this.labels.get(item.getUUID()) : null;
    }
}
//#endif
