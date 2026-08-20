package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.EntityAggregationListMode;

public final class EntityRenderAggregation implements IClientTickHandler
{
    private static final EntityRenderAggregation INSTANCE = new EntityRenderAggregation();
    private final Map<Integer, Group> groupsByEntityId = new HashMap<>();
    private ClientLevel trackedLevel;
    private long lastRebuildTick = Long.MIN_VALUE;

    private EntityRenderAggregation() {}

    public static EntityRenderAggregation getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue() || client.level == null)
        {
            clear();
            return;
        }

        if (this.trackedLevel != client.level)
        {
            this.groupsByEntityId.clear();
            this.trackedLevel = client.level;
            this.lastRebuildTick = Long.MIN_VALUE;
        }

        long gameTime = client.level.getGameTime();
        int interval = Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.getIntegerValue();
        if (!this.groupsByEntityId.isEmpty())
        {
            absorbNewEntities(client.level);
        }
        if (this.lastRebuildTick == Long.MIN_VALUE || gameTime < this.lastRebuildTick ||
                gameTime - this.lastRebuildTick >= interval)
        {
            rebuild(client.level);
            this.lastRebuildTick = gameTime;
        }
    }

    public void beginRenderFrame()
    {
        for (Group group : new HashSet<>(this.groupsByEntityId.values()))
        {
            group.representativeId = -1;
        }
    }

    public boolean shouldHide(Entity entity)
    {
        Group group = this.groupsByEntityId.get(entity.getId());
        if (group == null) return false;

        if (group.preferredRepresentativeId == entity.getId())
        {
            group.representativeId = entity.getId();
            return false;
        }
        return true;
    }

    public boolean shouldHideModel(Entity entity)
    {
        return Configs.ENTITY_AGGREGATION_COUNT_ONLY.getBooleanValue() &&
                this.groupsByEntityId.containsKey(entity.getId());
    }

    public Iterable<Entity> filterForRendering(Iterable<Entity> entities)
    {
        if (!Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue() || this.groupsByEntityId.isEmpty())
        {
            return entities;
        }

        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : entities)
        {
            Group group = this.groupsByEntityId.get(entity.getId());
            if (group == null || group.preferredRepresentativeId == entity.getId())
            {
                filtered.add(entity);
            }
        }
        return filtered;
    }

    public Component getLabel(Entity entity)
    {
        Group group = this.groupsByEntityId.get(entity.getId());
        return group != null && group.representativeId == entity.getId() ? group.label : null;
    }

    public void clear()
    {
        this.groupsByEntityId.clear();
        this.trackedLevel = null;
        this.lastRebuildTick = Long.MIN_VALUE;
    }

    private void rebuild(ClientLevel level)
    {
        this.groupsByEntityId.clear();

        Map<Object, List<Candidate>> candidatesByKey = new HashMap<>();
        for (Entity entity : level.entitiesForRendering())
        {
            Candidate candidate = candidateFor(entity);
            if (candidate != null)
            {
                candidatesByKey.computeIfAbsent(candidate.key, ignored -> new ArrayList<>()).add(candidate);
            }
        }

        double radius = Configs.ENTITY_AGGREGATION_RADIUS.getDoubleValue();
        double radiusSquared = radius * radius;
        int threshold = Configs.ENTITY_AGGREGATION_THRESHOLD.getIntegerValue();

        for (List<Candidate> candidates : candidatesByKey.values())
        {
            aggregate(candidates, radius, radiusSquared, threshold);
        }
    }

    private void absorbNewEntities(ClientLevel level)
    {
        double radius = Configs.ENTITY_AGGREGATION_RADIUS.getDoubleValue();
        double radiusSquared = radius * radius;
        List<Group> groups = new ArrayList<>(new HashSet<>(this.groupsByEntityId.values()));

        for (Entity entity : level.entitiesForRendering())
        {
            if (this.groupsByEntityId.containsKey(entity.getId())) continue;

            Candidate candidate = candidateFor(entity);
            if (candidate == null) continue;

            for (Group group : groups)
            {
                if (!group.key.equals(candidate.key)) continue;

                boolean close = false;
                for (int memberId : group.memberIds)
                {
                    Entity member = level.getEntity(memberId);
                    if (member != null && member.position().distanceToSqr(candidate.position()) <= radiusSquared)
                    {
                        close = true;
                        break;
                    }
                }

                if (close)
                {
                    group.add(candidate);
                    this.groupsByEntityId.put(candidate.entity.getId(), group);
                    break;
                }
            }
        }
    }

    private void aggregate(List<Candidate> candidates, double radius, double radiusSquared, int threshold)
    {
        if (candidates.size() <= threshold) return;

        int size = candidates.size();
        UnionFind unionFind = new UnionFind(size);
        Map<Cell, List<Integer>> buckets = new HashMap<>();

        for (int index = 0; index < size; index++)
        {
            Candidate candidate = candidates.get(index);
            Cell cell = Cell.of(candidate.position(), radius);
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    for (int dz = -1; dz <= 1; dz++)
                    {
                        List<Integer> neighbors = buckets.get(cell.offset(dx, dy, dz));
                        if (neighbors == null) continue;
                        for (int neighbor : neighbors)
                        {
                            if (candidate.position().distanceToSqr(candidates.get(neighbor).position()) <= radiusSquared)
                            {
                                unionFind.union(index, neighbor);
                            }
                        }
                    }
                }
            }
            buckets.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(index);
        }

        Map<Integer, List<Candidate>> components = new HashMap<>();
        for (int index = 0; index < size; index++)
        {
            components.computeIfAbsent(unionFind.find(index), ignored -> new ArrayList<>()).add(candidates.get(index));
        }

        for (List<Candidate> component : components.values())
        {
            if (component.size() <= threshold) continue;

            int displayCount = component.getFirst().item ? component.stream().mapToInt(candidate -> candidate.itemCount).sum() : component.size();
            Vec3 cameraPosition = Minecraft.getInstance().getCameraEntity() != null
                    ? Minecraft.getInstance().getCameraEntity().position()
                    : Vec3.ZERO;
            Candidate representative = component.stream()
                    .min(Comparator.comparingDouble(candidate -> candidate.position().distanceToSqr(cameraPosition)))
                    .orElseThrow();
            Group group = new Group(
                    component.getFirst().key,
                    component.getFirst().displayName,
                    component.getFirst().item,
                    component.size(),
                    displayCount,
                    representative.entity.getId());
            for (Candidate candidate : component)
            {
                group.memberIds.add(candidate.entity.getId());
                this.groupsByEntityId.put(candidate.entity.getId(), group);
            }
        }
    }

    private static Candidate candidateFor(Entity entity)
    {
        if (entity.isRemoved() || entity instanceof Player || entity instanceof ArmorStand || entity instanceof Display ||
                entity instanceof EnderDragon || entity instanceof WitherBoss || entity.hasCustomName())
        {
            return null;
        }

        if (!matchesList(entity)) return null;

        if (entity instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return null;
            return new Candidate(entity, new ItemKey(stack.copy()), stack.getHoverName(), true, stack.getCount());
        }

        if (entity instanceof LivingEntity)
        {
            EntityType<?> type = entity.getType();
            return new Candidate(entity, type, type.getDescription(), false, 1);
        }

        return null;
    }

    private static boolean matchesList(Entity entity)
    {
        EntityAggregationListMode mode = (EntityAggregationListMode) Configs.ENTITY_AGGREGATION_LIST_MODE.getOptionListValue();
        if (mode == EntityAggregationListMode.NONE) return true;

        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        java.util.List<String> configuredList = mode == EntityAggregationListMode.WHITELIST
                ? Configs.ENTITY_AGGREGATION_WHITELIST.getStrings()
                : Configs.ENTITY_AGGREGATION_BLACKLIST.getStrings();
        boolean listed = configuredList.stream()
                .map(String::trim)
                .anyMatch(typeId::equals);
        return mode == EntityAggregationListMode.WHITELIST ? listed : !listed;
    }

    private static final class Group
    {
        private final Object key;
        private final Component displayName;
        private final boolean item;
        private final List<Integer> memberIds = new ArrayList<>();
        private final int preferredRepresentativeId;
        private int entityCount;
        private int itemCount;
        private Component label;
        private int representativeId = -1;

        private Group(Object key, Component displayName, boolean item, int entityCount, int itemCount, int preferredRepresentativeId)
        {
            this.key = key;
            this.displayName = displayName;
            this.item = item;
            this.entityCount = entityCount;
            this.itemCount = itemCount;
            this.preferredRepresentativeId = preferredRepresentativeId;
            this.updateLabel();
        }

        private void add(Candidate candidate)
        {
            this.memberIds.add(candidate.entity.getId());
            this.entityCount++;
            this.itemCount += candidate.itemCount;
            this.updateLabel();
        }

        private void updateLabel()
        {
            int count = this.item ? this.itemCount : this.entityCount;
            this.label = Component.empty()
                    .append(this.displayName)
                    .append(Component.literal(" x " + count));
        }
    }

    private record Candidate(Entity entity, Object key, Component displayName, boolean item, int itemCount)
    {
        private Vec3 position()
        {
            return this.entity.position();
        }
    }

    private record Cell(int x, int y, int z)
    {
        private static Cell of(Vec3 position, double size)
        {
            return new Cell(
                    (int) Math.floor(position.x / size),
                    (int) Math.floor(position.y / size),
                    (int) Math.floor(position.z / size));
        }

        private Cell offset(int x, int y, int z)
        {
            return new Cell(this.x + x, this.y + y, this.z + z);
        }
    }

    private static final class ItemKey
    {
        private final ItemStack stack;

        private ItemKey(ItemStack stack)
        {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object object)
        {
            return object instanceof ItemKey other && ItemStack.isSameItemSameComponents(this.stack, other.stack);
        }

        @Override
        public int hashCode()
        {
            // Components are compared exactly in equals; bucket by item first
            // so component implementations with unstable hash codes still merge
            return this.stack.getItem().hashCode();
        }
    }

    private static final class UnionFind
    {
        private final int[] parents;

        private UnionFind(int size)
        {
            this.parents = new int[size];
            for (int index = 0; index < size; index++) this.parents[index] = index;
        }

        private int find(int value)
        {
            if (this.parents[value] != value) this.parents[value] = find(this.parents[value]);
            return this.parents[value];
        }

        private void union(int first, int second)
        {
            int firstRoot = find(first);
            int secondRoot = find(second);
            if (firstRoot != secondRoot) this.parents[secondRoot] = firstRoot;
        }
    }
}
