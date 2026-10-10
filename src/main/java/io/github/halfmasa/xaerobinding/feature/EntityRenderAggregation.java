package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
//#if MC >= 1.21.1
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.fabricmc.loader.api.FabricLoader;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//#endif
//#if MC >= 1.21.10
//#if MC >= 26.0
import net.minecraft.client.renderer.state.level.LevelRenderState;
//#else
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//#endif
//#endif
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
//#endif

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
//#if MC >= 1.21.1
    // Only numeric, immutable entity snapshots cross this thread boundary.
    private static final ExecutorService COUNT_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "halfmasa-entity-aggregation");
        thread.setDaemon(true);
        return thread;
    });
    private Future<List<GroupResult>> pending;
    private Map<Integer, GroupMeta> pendingMetadata;
    private long generation;
    private long pendingGeneration;
    //#if MC >= 1.21.10
    private final Map<EntityRenderState, Integer> frameLabels = new IdentityHashMap<>();
    //#endif
    private final Map<Integer, Vec3> legacyLabelOffsets = new HashMap<>();
    // Vanilla SubmitNodeCollection uses this scale and adds 0.5 to the attachment's Y.
    private static final float NAME_TAG_SCALE = 0.025F;
    private static final double NAME_TAG_Y_OFFSET = 0.5D;
    private static final double LABEL_GAP_PIXELS = 3.0D;
//#endif

    private EntityRenderAggregation() {}

    public static EntityRenderAggregation getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (
//#if MC >= 1.21.1
                (!Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue() &&
                 !Configs.ITEM_RENDER_AGGREGATION.getBooleanValue())
//#else
                //$$ !Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue()
//#endif
                || client.level == null
//#if MC >= 1.21.1
                || TweakerooRenderingDisabled.isDisabled()
//#endif
                )
        {
            clear();
            return;
        }

        if (this.trackedLevel != client.level)
        {
            this.groupsByEntityId.clear();
            this.trackedLevel = client.level;
            this.lastRebuildTick = Long.MIN_VALUE;
//#if MC >= 1.21.1
            cancelPending();
//#endif
        }

        long gameTime = client.level.getGameTime();
        int interval = Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.getIntegerValue();
//#if MC >= 1.21.1
        if (this.pending != null && this.pending.isDone())
        {
            publishCompleted();
        }
        // Never queue stale worlds or continuously rescan while a worker is busy.
        if (this.pending == null && (this.lastRebuildTick == Long.MIN_VALUE ||
                gameTime < this.lastRebuildTick || gameTime - this.lastRebuildTick >= interval))
        {
            Snapshot snapshot = capture(client.level, client);
            this.pendingMetadata = snapshot.metadata();
            this.pendingGeneration = this.generation;
            this.pending = COUNT_EXECUTOR.submit(() -> calculate(snapshot));
            this.lastRebuildTick = gameTime;
        }
//#else
        //$$ if (!this.groupsByEntityId.isEmpty())
        //$$ {
            //$$ absorbNewEntities(client.level);
        //$$ }
        //$$ if (this.lastRebuildTick == Long.MIN_VALUE || gameTime < this.lastRebuildTick ||
                //$$ gameTime - this.lastRebuildTick >= interval)
        //$$ {
            //$$ rebuild(client.level);
            //$$ this.lastRebuildTick = gameTime;
        //$$ }
//#endif
    }

    public void beginRenderFrame()
    {
//#if MC >= 1.21.1
        //#if MC >= 1.21.10
        this.frameLabels.clear();
        //#endif
        //#if MC < 1.21.10
        //$$ layoutLegacyLabels();
        //#endif
//#endif
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
        if (
//#if MC >= 1.21.1
                (!Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue() &&
                 !Configs.ITEM_RENDER_AGGREGATION.getBooleanValue())
//#else
                //$$ !Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue()
//#endif
                || this.groupsByEntityId.isEmpty())
        {
            return entities;
        }

        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : entities)
        {
            Group group = this.groupsByEntityId.get(entity.getId());
            if (group == null || group.preferredRepresentativeId == entity.getId())
            {
                if (group != null) group.representativeId = entity.getId();
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

    //#if MC >= 1.21.1 && MC < 1.21.10
    //$$ public Vec3 getLabelOffset(Entity entity) { return legacyLabelOffsets.getOrDefault(entity.getId(), Vec3.ZERO); }
    //$$ private void layoutLegacyLabels()
    //$$ {
    //$$     legacyLabelOffsets.clear();
    //$$     Minecraft client = Minecraft.getInstance();
    //$$     if (client.level == null || groupsByEntityId.isEmpty()) return;
    //$$     var camera = client.gameRenderer.getMainCamera();
    //$$     int width = client.getWindow().getWidth(), height = client.getWindow().getHeight();
    //$$     if (width <= 0 || height <= 0) return;
    //#if MC >= 1.21.3
    //$$     float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    //#else
    //$$     float partial = client.getTimer().getGameTimeDeltaPartialTick(false);
    //#endif
    //$$     var fov = ((io.github.halfmasa.xaerobinding.mixin.AggregationGameRendererAccessor) client.gameRenderer).halfmasa$getFov(camera, partial, true);
    //$$     Matrix4f projection = client.gameRenderer.getProjectionMatrix(fov);
    //$$     float focalX = projection.m00(), focalY = projection.m11();
    //$$     if (focalX == 0 || focalY == 0) return;
    //$$     Matrix4f view = new Matrix4f().rotation(new org.joml.Quaternionf(camera.rotation()).conjugate());
    //$$     projection.mul(view);
    //$$     Matrix4f inverse = new Matrix4f(view).invert();
    //$$     var labels = new ArrayList<AggregationLabelLayout.Label>();
    //$$     var clipDepths = new HashMap<Integer, Float>();
    //$$     boolean side = Configs.ENTITY_AGGREGATION_LABEL_POSITION.getOptionListValue() == io.github.halfmasa.xaerobinding.config.EntityLabelPosition.SIDE;
    //$$     for (Group group : new HashSet<>(groupsByEntityId.values())) {
    //$$         Entity entity = client.level.getEntity(group.preferredRepresentativeId);
    //$$         if (entity == null || group.label == null) continue;
    //$$         Vec3 relative = entity.getPosition(partial).subtract(camera.getPosition());
    //$$         Vector4f clip = projection.transform(new Vector4f((float) (relative.x + (side ? entity.getBbWidth() * 0.5D + 0.35D : 0)), (float) (relative.y + (side ? entity.getBbHeight() * 0.5D : entity.getBbHeight() + 0.5D)), (float) relative.z, 1));
    //$$         if (!Float.isFinite(clip.w) || clip.w <= 0 || Math.abs(clip.x) > clip.w || Math.abs(clip.y) > clip.w || Math.abs(clip.z) > clip.w) continue;
    //$$         double sx = NAME_TAG_SCALE * Math.abs(focalX) * width / (2 * clip.w), sy = NAME_TAG_SCALE * Math.abs(focalY) * height / (2 * clip.w);
    //$$         labels.add(new AggregationLabelLayout.Label(entity.getId(), (clip.x / clip.w + 1) * width / 2, (1 - clip.y / clip.w) * height / 2, (client.font.width(group.label) + 2) * sx, (client.font.lineHeight + 2) * sy, relative.lengthSqr()));
    //$$         clipDepths.put(entity.getId(), clip.w);
    //$$     }
    //$$     for (var placement : AggregationLabelLayout.arrange(labels, LABEL_GAP_PIXELS)) {
    //$$         if (placement.offsetY() == 0) continue;
    //$$         Vector3f offset = inverse.transformDirection(new Vector3f(0, (float) (-2 * placement.offsetY() * clipDepths.get(placement.id()) / (height * focalY)), 0));
    //$$         legacyLabelOffsets.put(placement.id(), new Vec3(offset.x, offset.y, offset.z));
    //$$     }
    //$$ }
    //#endif

    public void clear()
    {
//#if MC >= 1.21.1
        cancelPending();
        //#if MC >= 1.21.10
        this.frameLabels.clear();
        //#endif
//#endif
        this.groupsByEntityId.clear();
        this.trackedLevel = null;
        this.lastRebuildTick = Long.MIN_VALUE;
    }

//#if MC >= 1.21.1
    //#if MC >= 1.21.10
    public void trackLabel(Entity entity, EntityRenderState state)
    {
        this.frameLabels.put(state, entity.getId());
    }

    //#if MC >= 1.21.10
    /** All visible representatives must be extracted before resolving screen-space collisions. */
    public void layoutLabels(LevelRenderState levelState)
    {
        if (this.frameLabels.size() < 2) return;
        var camera = levelState.cameraRenderState;
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getWidth(), height = client.getWindow().getHeight();
        if (camera == null || camera.pos == null || camera.orientation == null || width <= 0 || height <= 0) return;
        //#if MC >= 26.0
        if (camera.projectionMatrix == null || camera.viewRotationMatrix == null) return;
        Matrix4f projectionMatrix = camera.projectionMatrix;
        Matrix4f viewRotation = camera.viewRotationMatrix;
        //#else
        //$$ float fov = ((io.github.halfmasa.xaerobinding.mixin.AggregationGameRendererAccessor) client.gameRenderer).halfmasa$getFov(client.gameRenderer.getMainCamera(), client.getDeltaTracker().getGameTimeDeltaPartialTick(false), true);
        //$$ Matrix4f projectionMatrix = client.gameRenderer.getProjectionMatrix(fov);
        //$$ Matrix4f viewRotation = new Matrix4f().rotation(new org.joml.Quaternionf(camera.orientation).conjugate());
        //#endif
        float focalX = projectionMatrix.m00(), focalY = projectionMatrix.m11();
        if (focalX == 0 || focalY == 0) return;
        Matrix4f projection = new Matrix4f(projectionMatrix).mul(viewRotation);
        Matrix4f inverseView = new Matrix4f(viewRotation).invert();
        var labels = new ArrayList<AggregationLabelLayout.Label>();
        var projected = new HashMap<Integer, ProjectedLabel>();
        for (EntityRenderState state : levelState.entityRenderStates)
        {
            Integer id = this.frameLabels.get(state);
            if (id == null || state.nameTag == null || state.nameTagAttachment == null) continue;
            Vec3 attachment = state.nameTagAttachment;
            Vector4f clip = projection.transform(new Vector4f(
                    (float) (state.x + attachment.x - camera.pos.x),
                    (float) (state.y + attachment.y + NAME_TAG_Y_OFFSET - camera.pos.y),
                    (float) (state.z + attachment.z - camera.pos.z), 1));
            if (!Float.isFinite(clip.w) || clip.w <= 0 || Math.abs(clip.x) > clip.w ||
                    Math.abs(clip.y) > clip.w || Math.abs(clip.z) > clip.w) continue;
            double scaleX = NAME_TAG_SCALE * Math.abs(focalX) * width / (2.0D * clip.w);
            double scaleY = NAME_TAG_SCALE * Math.abs(focalY) * height / (2.0D * clip.w);
            // Include vanilla's nameplate background and shadow around the glyphs.
            double textWidth = (client.font.width(state.nameTag) + 2) * scaleX;
            double textHeight = (client.font.lineHeight + 2) * scaleY;
            labels.add(new AggregationLabelLayout.Label(id,
                    (clip.x / clip.w + 1) * width / 2.0D,
                    (1 - clip.y / clip.w) * height / 2.0D - scaleY,
                    textWidth, textHeight, state.distanceToCameraSq));
            projected.put(id, new ProjectedLabel(state, clip.w));
        }
        for (var placement : AggregationLabelLayout.arrange(labels, LABEL_GAP_PIXELS))
        {
            if (placement.offsetY() == 0) continue;
            ProjectedLabel label = projected.get(placement.id());
            float viewY = (float) (-2 * placement.offsetY() * label.clipW() / (height * focalY));
            Vector3f offset = inverseView.transformDirection(new Vector3f(0, viewY, 0));
            EntityRenderState state = label.state();
            state.nameTagAttachment = state.nameTagAttachment.add(offset.x, offset.y, offset.z);
        }
    }

    //#endif
    private record ProjectedLabel(EntityRenderState state, float clipW) {}
    //#endif

    private void cancelPending()
    {
        this.generation++;
        if (this.pending != null)
        {
            this.pending.cancel(true);
            this.pending = null;
        }
        this.pendingMetadata = null;
    }

    private void publishCompleted()
    {
        Future<List<GroupResult>> completed = this.pending;
        Map<Integer, GroupMeta> metadata = this.pendingMetadata;
        long completedGeneration = this.pendingGeneration;
        this.pending = null;
        this.pendingMetadata = null;
        try
        {
            List<GroupResult> results = completed.get();
            if (completedGeneration != this.generation || metadata == null) return;

            publishResults(results, metadata);
        }
        catch (CancellationException ignored)
        {
            // Disabling aggregation or changing worlds deliberately invalidates the job.
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
        }
        catch (ExecutionException exception)
        {
            XaeroWorldBinding.LOGGER.warn("Entity aggregation worker failed", exception);
        }
    }

    private void publishResults(List<GroupResult> results, Map<Integer, GroupMeta> metadata)
    {
        this.groupsByEntityId.clear();
        for (GroupResult result : results)
        {
            GroupMeta meta = metadata.get(result.keyId());
            if (meta == null) continue;
            Group group = new Group(meta.key(), meta.displayName(), meta.item(),
                    result.memberIds().size(), result.displayCount(), result.representativeId());
            group.memberIds.addAll(result.memberIds());
            for (int entityId : result.memberIds())
            {
                this.groupsByEntityId.put(entityId, group);
            }
        }
    }

    private static Snapshot capture(ClientLevel level, Minecraft client)
    {
        Map<Object, Integer> keyIds = new HashMap<>();
        Map<Integer, GroupMeta> metadata = new HashMap<>();
        List<FlatCandidate> candidates = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering())
        {
            Candidate candidate = candidateFor(entity);
            if (candidate == null) continue;
            int keyId = keyIds.computeIfAbsent(candidate.key(), ignored -> keyIds.size());
            metadata.putIfAbsent(keyId,
                    new GroupMeta(candidate.key(), candidate.displayName(), candidate.item()));
            Vec3 position = candidate.position();
            candidates.add(new FlatCandidate(entity.getId(), keyId,
                    position.x, position.y, position.z, candidate.itemCount(), candidate.item()));
        }
        Vec3 camera = client.getCameraEntity() != null
                ? client.getCameraEntity().position() : Vec3.ZERO;
        double radius = Configs.ENTITY_AGGREGATION_RADIUS.getDoubleValue();
        int threshold = Configs.ENTITY_AGGREGATION_THRESHOLD.getIntegerValue();
        return new Snapshot(List.copyOf(candidates), metadata,
                camera.x, camera.y, camera.z, radius, threshold);
    }

    private static List<GroupResult> calculate(Snapshot snapshot)
    {
        Map<Integer, List<FlatCandidate>> byKey = new HashMap<>();
        for (FlatCandidate candidate : snapshot.candidates())
        {
            byKey.computeIfAbsent(candidate.keyId(), ignored -> new ArrayList<>()).add(candidate);
        }

        List<GroupResult> results = new ArrayList<>();
        double radius = snapshot.radius();
        double radiusSquared = radius * radius;
        for (List<FlatCandidate> candidates : byKey.values())
        {
            if (Thread.currentThread().isInterrupted()) return List.of();
            // Two matching item entities already save one model draw. The mob
            // threshold remains configurable and independent of this switch.
            int threshold = candidates.getFirst().item() ? 1 : snapshot.threshold();
            if (candidates.size() <= threshold) continue;

            UnionFind unionFind = new UnionFind(candidates.size());
            Map<Cell, List<Integer>> buckets = new HashMap<>();
            for (int index = 0; index < candidates.size(); index++)
            {
                if (Thread.currentThread().isInterrupted()) return List.of();
                FlatCandidate candidate = candidates.get(index);
                Cell cell = Cell.of(candidate.x(), candidate.y(), candidate.z(), radius);
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
                                if (candidate.distanceSquared(candidates.get(neighbor)) <= radiusSquared)
                                {
                                    unionFind.union(index, neighbor);
                                }
                            }
                        }
                    }
                }
                buckets.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(index);
            }

            Map<Integer, List<FlatCandidate>> components = new HashMap<>();
            for (int index = 0; index < candidates.size(); index++)
            {
                components.computeIfAbsent(unionFind.find(index), ignored -> new ArrayList<>())
                        .add(candidates.get(index));
            }
            for (List<FlatCandidate> component : components.values())
            {
                if (component.size() <= threshold) continue;
                FlatCandidate representative = component.stream()
                        .min(Comparator.comparingDouble(candidate -> candidate.distanceSquared(
                                snapshot.cameraX(), snapshot.cameraY(), snapshot.cameraZ())))
                        .orElseThrow();
                List<Integer> ids = component.stream().map(FlatCandidate::entityId).toList();
                int displayCount = component.stream().mapToInt(FlatCandidate::itemCount).sum();
                results.add(new GroupResult(component.getFirst().keyId(), ids,
                        representative.entityId(), displayCount));
            }
        }
        return results;
    }

    private record Snapshot(List<FlatCandidate> candidates, Map<Integer, GroupMeta> metadata,
                            double cameraX, double cameraY, double cameraZ, double radius, int threshold) {}
    private record GroupMeta(Object key, Component displayName, boolean item) {}
    private record GroupResult(int keyId, List<Integer> memberIds, int representativeId, int displayCount) {}
    private record FlatCandidate(int entityId, int keyId, double x, double y, double z,
                                 int itemCount, boolean item)
    {
        private double distanceSquared(FlatCandidate other)
        {
            return distanceSquared(other.x, other.y, other.z);
        }

        private double distanceSquared(double otherX, double otherY, double otherZ)
        {
            double dx = this.x - otherX;
            double dy = this.y - otherY;
            double dz = this.z - otherZ;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private static final class TweakerooRenderingDisabled
    {
        private static final ConfigBooleanHotkeyed TOGGLE = findToggle();

        private static ConfigBooleanHotkeyed findToggle()
        {
            if (!FabricLoader.getInstance().isModLoaded("tweakeroo")) return null;
            try
            {
                Class<?> owner = Class.forName("fi.dy.masa.tweakeroo.config.Configs$Disable");
                Field field = owner.getField("DISABLE_ENTITY_RENDERING");
                return (ConfigBooleanHotkeyed) field.get(null);
            }
            catch (ReflectiveOperationException | LinkageError exception)
            {
                XaeroWorldBinding.LOGGER.warn("Unable to read Tweakeroo disableEntityRendering", exception);
                return null;
            }
        }

        private static boolean isDisabled()
        {
            return TOGGLE != null && TOGGLE.getBooleanValue();
        }
    }
//#else
    //$$ private void rebuild(ClientLevel level)
    //$$ {
        //$$ this.groupsByEntityId.clear();

        //$$ Map<Object, List<Candidate>> candidatesByKey = new HashMap<>();
        //$$ for (Entity entity : level.entitiesForRendering())
        //$$ {
            //$$ Candidate candidate = candidateFor(entity);
            //$$ if (candidate != null)
            //$$ {
                //$$ candidatesByKey.computeIfAbsent(candidate.key, ignored -> new ArrayList<>()).add(candidate);
            //$$ }
        //$$ }

        //$$ double radius = Configs.ENTITY_AGGREGATION_RADIUS.getDoubleValue();
        //$$ double radiusSquared = radius * radius;
        //$$ int threshold = Configs.ENTITY_AGGREGATION_THRESHOLD.getIntegerValue();

        //$$ for (List<Candidate> candidates : candidatesByKey.values())
        //$$ {
            //$$ aggregate(candidates, radius, radiusSquared, threshold);
        //$$ }
    //$$ }

    //$$ private void absorbNewEntities(ClientLevel level)
    //$$ {
        //$$ double radius = Configs.ENTITY_AGGREGATION_RADIUS.getDoubleValue();
        //$$ double radiusSquared = radius * radius;
        //$$ List<Group> groups = new ArrayList<>(new HashSet<>(this.groupsByEntityId.values()));

        //$$ for (Entity entity : level.entitiesForRendering())
        //$$ {
            //$$ if (this.groupsByEntityId.containsKey(entity.getId())) continue;

            //$$ Candidate candidate = candidateFor(entity);
            //$$ if (candidate == null) continue;

            //$$ for (Group group : groups)
            //$$ {
                //$$ if (!group.key.equals(candidate.key)) continue;

                //$$ boolean close = false;
                //$$ for (int memberId : group.memberIds)
                //$$ {
                    //$$ Entity member = level.getEntity(memberId);
                    //$$ if (member != null && member.position().distanceToSqr(candidate.position()) <= radiusSquared)
                    //$$ {
                        //$$ close = true;
                        //$$ break;
                    //$$ }
                //$$ }

                //$$ if (close)
                //$$ {
                    //$$ group.add(candidate);
                    //$$ this.groupsByEntityId.put(candidate.entity.getId(), group);
                    //$$ break;
                //$$ }
            //$$ }
        //$$ }
    //$$ }

    //$$ private void aggregate(List<Candidate> candidates, double radius, double radiusSquared, int threshold)
    //$$ {
        //$$ if (candidates.size() <= threshold) return;

        //$$ int size = candidates.size();
        //$$ UnionFind unionFind = new UnionFind(size);
        //$$ Map<Cell, List<Integer>> buckets = new HashMap<>();

        //$$ for (int index = 0; index < size; index++)
        //$$ {
            //$$ Candidate candidate = candidates.get(index);
            //$$ Cell cell = Cell.of(candidate.position(), radius);
            //$$ for (int dx = -1; dx <= 1; dx++)
            //$$ {
                //$$ for (int dy = -1; dy <= 1; dy++)
                //$$ {
                    //$$ for (int dz = -1; dz <= 1; dz++)
                    //$$ {
                        //$$ List<Integer> neighbors = buckets.get(cell.offset(dx, dy, dz));
                        //$$ if (neighbors == null) continue;
                        //$$ for (int neighbor : neighbors)
                        //$$ {
                            //$$ if (candidate.position().distanceToSqr(candidates.get(neighbor).position()) <= radiusSquared)
                            //$$ {
                                //$$ unionFind.union(index, neighbor);
                            //$$ }
                        //$$ }
                    //$$ }
                //$$ }
            //$$ }
            //$$ buckets.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(index);
        //$$ }

        //$$ Map<Integer, List<Candidate>> components = new HashMap<>();
        //$$ for (int index = 0; index < size; index++)
        //$$ {
            //$$ components.computeIfAbsent(unionFind.find(index), ignored -> new ArrayList<>()).add(candidates.get(index));
        //$$ }

        //$$ for (List<Candidate> component : components.values())
        //$$ {
            //$$ if (component.size() <= threshold) continue;

            //$$ int displayCount = component.getFirst().item ? component.stream().mapToInt(candidate -> candidate.itemCount).sum() : component.size();
            //$$ Vec3 cameraPosition = Minecraft.getInstance().getCameraEntity() != null
                    //$$ ? Minecraft.getInstance().getCameraEntity().position()
                    //$$ : Vec3.ZERO;
            //$$ Candidate representative = component.stream()
                    //$$ .min(Comparator.comparingDouble(candidate -> candidate.position().distanceToSqr(cameraPosition)))
                    //$$ .orElseThrow();
            //$$ Group group = new Group(
                    //$$ component.getFirst().key,
                    //$$ component.getFirst().displayName,
                    //$$ component.getFirst().item,
                    //$$ component.size(),
                    //$$ displayCount,
                    //$$ representative.entity.getId());
            //$$ for (Candidate candidate : component)
            //$$ {
                //$$ group.memberIds.add(candidate.entity.getId());
                //$$ this.groupsByEntityId.put(candidate.entity.getId(), group);
            //$$ }
        //$$ }
    //$$ }
//#endif

    private static Candidate candidateFor(Entity entity)
    {
        if (entity.isRemoved() || entity instanceof Player || entity instanceof ArmorStand || entity instanceof Display ||
                entity instanceof EnderDragon || entity instanceof WitherBoss ||
//#if MC >= 1.21.1
                (entity.hasCustomName() && !(entity instanceof ItemEntity)))
//#else
                //$$ entity.hasCustomName())
//#endif
        {
            return null;
        }

        if (!matchesList(entity)) return null;

        if (entity instanceof ItemEntity itemEntity)
        {
//#if MC >= 1.21.1
            if (!Configs.ITEM_RENDER_AGGREGATION.getBooleanValue()) return null;
//#endif
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return null;
            return new Candidate(entity, new ItemKey(stack.copy()), stack.getHoverName(), true, stack.getCount());
        }

        if (entity instanceof LivingEntity living)
        {
//#if MC >= 1.21.1
            if (!Configs.ENTITY_RENDER_AGGREGATION.getBooleanValue()) return null;
//#endif
            EntityType<?> type = entity.getType();
//#if MC >= 1.21.1
            if (Configs.ENTITY_AGGREGATION_SEPARATE_SIZES.getBooleanValue())
            {
                // Standing dimensions distinguish baby/adult mobs, slime sizes and scale
                // attributes without splitting a group merely because its pose changes.
                var dimensions = living.getDimensions(Pose.STANDING);
                Object key = new MobSizeKey(type, living.isBaby(), dimensions.width(), dimensions.height());
                return new Candidate(entity, key, type.getDescription(), false, 1);
            }
//#endif
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
                    .append(Component.literal(" * " + count));
        }
    }

    private record Candidate(Entity entity, Object key, Component displayName, boolean item, int itemCount)
    {
        private Vec3 position()
        {
            return this.entity.position();
        }
    }

//#if MC >= 1.21.1
    private record MobSizeKey(EntityType<?> type, boolean baby, float width, float height) {}
//#endif

    private record Cell(int x, int y, int z)
    {
//#if MC >= 1.21.1
        private static Cell of(double x, double y, double z, double size)
        {
            return new Cell((int) Math.floor(x / size),
                    (int) Math.floor(y / size), (int) Math.floor(z / size));
        }
//#else
        //$$ private static Cell of(Vec3 position, double size)
        //$$ {
            //$$ return new Cell(
                    //$$ (int) Math.floor(position.x / size),
                    //$$ (int) Math.floor(position.y / size),
                    //$$ (int) Math.floor(position.z / size));
        //$$ }
//#endif

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
