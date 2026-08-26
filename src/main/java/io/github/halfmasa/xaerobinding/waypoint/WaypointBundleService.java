package io.github.halfmasa.xaerobinding.waypoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

public final class WaypointBundleService
{
    public static final String LEGACY_PREFIX = "XWB1:";
    public static final String PREFIX = "XWB2:";
    private static final Gson GSON = new Gson();

    private WaypointBundleService()
    {
    }

    public static ExportResult export(ExportScope scope) throws IOException
    {
        MinimapWorld currentWorld = currentWorld();
        BundleV2 bundle = new BundleV2();
        bundle.createdAt = Instant.now().toString();
        bundle.scope = scope.name();
        bundle.sourceContainer = currentWorld.getContainer().getRoot().getPath().toString();

        int waypointCount = 0;
        int setCount = 0;
        Set<String> exportedPaths = new HashSet<>();
        Iterable<MinimapWorld> worlds = scope == ExportScope.ALL_DIMENSIONS
                ? currentWorld.getContainer().getRoot().getAllWorldsIterable()
                : List.of(currentWorld);

        for (MinimapWorld world : worlds)
        {
            if (!exportedPaths.add(world.getFullPath().toString()))
            {
                continue;
            }

            DimensionData dimension = new DimensionData();
            dimension.dimensionId = dimensionId(world);
            dimension.equivalentDimensionId = equivalentDimensionId(world);
            dimension.node = world.getNode();
            dimension.sourcePath = world.getFullPath().toString();
            dimension.containerNodes = containerNodes(world);
            dimension.containerPath = world.getContainer().getPath().toString();
            dimension.worldPath = world.getFullPath().toString();
            dimension.localWorldKey = world.getLocalWorldKey().toString();
            dimension.dimensionName = world.getContainer().getSubName();
            dimension.containerName = containerName(world);
            String onlySet = scope == ExportScope.CURRENT_SET ? world.getCurrentWaypointSetId() : null;
            if (scope == ExportScope.CURRENT_SET && (onlySet == null || onlySet.isBlank()))
            {
                throw new IOException("halfmasa.error.xaero_set_missing");
            }

            for (WaypointSet set : world.getIterableWaypointSets())
            {
                if (onlySet != null && !onlySet.equals(set.getName()))
                {
                    continue;
                }

                SetData setData = new SetData();
                setData.name = set.getName();
                for (Waypoint waypoint : set.getWaypoints())
                {
                    if (isExportable(waypoint))
                    {
                        setData.waypoints.add(WaypointData.from(waypoint));
                        waypointCount++;
                    }
                }
                dimension.sets.add(setData);
                setCount++;
            }
            bundle.dimensions.add(dimension);
        }

        return new ExportResult(encode(PREFIX, bundle), waypointCount, setCount, bundle.dimensions.size());
    }

    public static ImportResult importIntoCurrentWorld(String encoded) throws IOException
    {
        String text = encoded == null ? "" : encoded.strip();
        if (text.startsWith(PREFIX))
        {
            return importV2(decode(text, PREFIX, BundleV2.class));
        }
        if (text.startsWith(LEGACY_PREFIX))
        {
            return importV1(decode(text, LEGACY_PREFIX, BundleV1.class));
        }
        throw new IOException("halfmasa.error.waypoint_bundle_missing");
    }

    private static ImportResult importV2(BundleV2 bundle) throws IOException
    {
        if (bundle == null || bundle.format != 2 || bundle.dimensions == null || bundle.dimensions.isEmpty())
        {
            throw new IOException("halfmasa.error.waypoint_bundle_version");
        }

        List<PreparedDimension> prepared = new ArrayList<>();
        for (DimensionData dimension : bundle.dimensions)
        {
            prepared.add(prepareDimension(dimension));
        }

        MinimapSession session = session();
        MinimapWorld currentWorld = currentWorld();
        MinimapWorldRootContainer root = currentWorld.getContainer().getRoot();
        Set<MinimapWorld> changedWorlds = new LinkedHashSet<>();
        int imported = 0;
        for (PreparedDimension dimension : prepared)
        {
            MinimapWorld world = findOrCreateWorld(root, currentWorld.getContainer(), dimension);
            applyDimensionNames(root, dimension);
            changedWorlds.add(world);
            for (PreparedSet preparedSet : dimension.sets())
            {
                WaypointSet set = world.getWaypointSet(preparedSet.name());
                if (set == null)
                {
                    world.addWaypointSet(preparedSet.name());
                    set = world.getWaypointSet(preparedSet.name());
                }
                for (Waypoint waypoint : preparedSet.waypoints())
                {
                    set.add(waypoint);
                    imported++;
                }
            }
        }

        int removed = 0;
        for (MinimapWorld world : changedWorlds)
        {
            removed += removeDuplicates(world, null);
            session.getWorldManagerIO().saveWorld(world);
        }
        return new ImportResult(imported, removed, changedWorlds.size());
    }

    private static ImportResult importV1(BundleV1 bundle) throws IOException
    {
        if (bundle == null || bundle.format != 1 || bundle.sets == null)
        {
            throw new IOException("halfmasa.error.waypoint_bundle_version");
        }

        MinimapWorld world = currentWorld();
        DimensionData legacyDimension = new DimensionData();
        legacyDimension.dimensionId = dimensionId(world);
        legacyDimension.equivalentDimensionId = equivalentDimensionId(world);
        legacyDimension.node = world.getNode();
        legacyDimension.sets = bundle.sets;
        PreparedDimension prepared = prepareDimension(legacyDimension);

        int imported = 0;
        for (PreparedSet preparedSet : prepared.sets())
        {
            WaypointSet set = world.getWaypointSet(preparedSet.name());
            if (set == null)
            {
                world.addWaypointSet(preparedSet.name());
                set = world.getWaypointSet(preparedSet.name());
            }
            for (Waypoint waypoint : preparedSet.waypoints())
            {
                set.add(waypoint);
                imported++;
            }
        }

        int removed = removeDuplicates(world, null);
        session().getWorldManagerIO().saveWorld(world);
        return new ImportResult(imported, removed, 1);
    }

    private static PreparedDimension prepareDimension(DimensionData dimension) throws IOException
    {
        if (dimension == null || dimension.node == null || dimension.node.isBlank() || dimension.sets == null)
        {
            throw new IOException("halfmasa.error.waypoint_bundle_invalid");
        }

        ResourceKey<Level> dimensionKey = null;
        if (dimension.dimensionId != null && !dimension.dimensionId.isBlank())
        {
            //#if MC >= 1.21.11
            Identifier location = Identifier.tryParse(dimension.dimensionId);
            //#else
            //$$ ResourceLocation location = ResourceLocation.tryParse(dimension.dimensionId);
            //#endif
            if (location == null)
            {
                throw new IOException("halfmasa.error.waypoint_bundle_invalid");
            }
            dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
        }
        if (dimensionKey == null && dimension.equivalentDimensionId != null &&
                !dimension.equivalentDimensionId.isBlank())
        {
            //#if MC >= 1.21.11
            Identifier location = Identifier.tryParse(dimension.equivalentDimensionId);
            //#else
            //$$ ResourceLocation location = ResourceLocation.tryParse(dimension.equivalentDimensionId);
            //#endif
            if (location != null)
            {
                dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
            }
        }

        List<PreparedSet> sets = new ArrayList<>();
        for (SetData setData : dimension.sets)
        {
            if (setData == null || setData.name == null || setData.name.isBlank() || setData.waypoints == null)
            {
                throw new IOException("halfmasa.error.waypoint_bundle_invalid");
            }
            List<Waypoint> waypoints = new ArrayList<>();
            for (WaypointData waypoint : setData.waypoints)
            {
                if (waypoint == null || waypoint.color < 0 || waypoint.color >= WaypointColor.values().length)
                {
                    throw new IOException("halfmasa.error.waypoint_bundle_invalid");
                }
                waypoints.add(waypoint.toWaypoint());
            }
            sets.add(new PreparedSet(setData.name, waypoints));
        }
        List<String> containerNodes = dimension.containerNodes == null
                ? List.of()
                : List.copyOf(dimension.containerNodes);
        for (String node : containerNodes)
        {
            if (node == null || node.isBlank() || node.contains(":") || node.contains("/"))
            {
                throw new IOException("halfmasa.error.waypoint_bundle_invalid");
            }
        }
        String containerPath = dimension.containerPath == null ? "" : dimension.containerPath;
        String worldPath = dimension.worldPath == null ? "" : dimension.worldPath;
        String localWorldKey = dimension.localWorldKey == null ? "" : dimension.localWorldKey;
        return new PreparedDimension(
                dimension.node,
                dimensionKey,
                containerNodes,
                containerPath,
                worldPath,
                localWorldKey,
                dimension.dimensionName,
                dimension.containerName,
                sets);
    }

    private static MinimapWorld findOrCreateWorld(
            MinimapWorldRootContainer root,
            MinimapWorldContainer currentContainer,
            PreparedDimension dimension)
    {
        MinimapWorld nodeMatch = null;
        for (MinimapWorld candidate : root.getAllWorldsIterable())
        {
            if (dimensionMatches(candidate, dimension))
            {
                return candidate;
            }
            if (dimension.node().equals(candidate.getNode()) &&
                    sameContainerNodes(candidate, dimension.containerNodes()))
            {
                nodeMatch = candidate;
            }
        }
        if (nodeMatch != null)
        {
            if (dimension.dimensionKey() == null)
            {
                return nodeMatch;
            }
        }

        MinimapWorldContainer targetContainer = containerFor(root, currentContainer, dimension);
        if (targetContainer == null && dimension.dimensionKey() != null)
        {
            String dimensionNode = session().getDimensionHelper()
                    .getDimensionDirectoryName(dimension.dimensionKey());
            targetContainer = root.addSubContainer(root.getPath().resolve(dimensionNode));
        }
        if (targetContainer == null)
        {
            targetContainer = currentContainer;
        }

        MinimapWorld world = targetContainer.addWorld(dimension.node());
        if (dimension.dimensionKey() != null)
        {
            world.setDimId(dimension.dimensionKey());
        }
        return world;
    }

    private static MinimapWorldContainer containerFor(
            MinimapWorldRootContainer root,
            MinimapWorldContainer currentContainer,
            PreparedDimension dimension)
    {
        if (dimension.containerNodes().isEmpty())
        {
            return dimension.dimensionKey() == null ? currentContainer : null;
        }

        MinimapWorldContainer container = root;
        xaero.hud.path.XaeroPath path = root.getPath();
        for (String node : dimension.containerNodes())
        {
            path = path.resolve(node);
            container = container.addSubContainer(path);
        }
        return container;
    }

    private static boolean dimensionMatches(MinimapWorld candidate, PreparedDimension dimension)
    {
        if (dimension.dimensionKey() != null)
        {
            ResourceKey<Level> candidateKey = candidate.getDimId();
            if (dimension.dimensionKey().equals(candidateKey))
            {
                return true;
            }
            if (dimensionKeyId(dimension.dimensionKey()).equals(equivalentDimensionId(candidate)))
            {
                return true;
            }
        }
        if (!dimension.containerPath().isBlank() && !dimension.containerNodes().isEmpty() &&
                dimension.containerNodes().equals(containerNodes(candidate)))
        {
            return true;
        }
        return !dimension.localWorldKey().isBlank() &&
                dimension.localWorldKey().equals(candidate.getLocalWorldKey().toString());
    }

    private static void applyDimensionNames(
            MinimapWorldRootContainer root,
            PreparedDimension dimension)
    {
        String name = dimension.containerName();
        if ((name == null || name.isBlank()) && dimension.dimensionName() != null)
        {
            name = dimension.dimensionName();
        }
        if (!dimension.containerNodes().isEmpty() && name != null && !name.isBlank())
        {
            MinimapWorldContainer parent = root;
            xaero.hud.path.XaeroPath path = root.getPath();
            List<String> nodes = dimension.containerNodes();
            for (int index = 0; index < nodes.size() - 1; index++)
            {
                path = path.resolve(nodes.get(index));
                parent = parent.addSubContainer(path);
            }
            parent.setName(nodes.get(nodes.size() - 1), name);
        }
    }

    private static boolean sameContainerNodes(MinimapWorld world, List<String> nodes)
    {
        return !nodes.isEmpty() && nodes.equals(containerNodes(world));
    }

    private static List<String> containerNodes(MinimapWorld world)
    {
        xaero.hud.path.XaeroPath rootPath = world.getContainer().getRoot().getPath();
        xaero.hud.path.XaeroPath containerPath = world.getContainer().getPath();
        if (!containerPath.isSubOf(rootPath) && !rootPath.equals(containerPath))
        {
            return List.of(containerPath.getLastNode());
        }

        List<String> nodes = new ArrayList<>();
        for (int index = rootPath.getNodeCount(); index < containerPath.getNodeCount(); index++)
        {
            nodes.add(containerPath.getAtIndex(index).getLastNode());
        }
        return List.copyOf(nodes);
    }

    private static String containerName(MinimapWorld world)
    {
        List<String> nodes = containerNodes(world);
        if (nodes.isEmpty())
        {
            return null;
        }

        MinimapWorldContainer parent = world.getContainer().getRoot();
        xaero.hud.path.XaeroPath path = parent.getPath();
        for (int index = 0; index < nodes.size() - 1; index++)
        {
            path = path.resolve(nodes.get(index));
            parent = parent.addSubContainer(path);
        }
        return parent.getName(nodes.get(nodes.size() - 1));
    }

    public static int removeDuplicatesFromCurrentSet() throws IOException
    {
        MinimapWorld world = currentWorld();
        int removed = removeDuplicates(world, world.getCurrentWaypointSetId());
        if (removed > 0)
        {
            session().getWorldManagerIO().saveWorld(world);
        }
        return removed;
    }

    public static int removeDuplicatesFromAllSets() throws IOException
    {
        MinimapWorld world = currentWorld();
        int removed = removeDuplicates(world, null);
        if (removed > 0)
        {
            session().getWorldManagerIO().saveWorld(world);
        }
        return removed;
    }

    static Snapshot captureSnapshot()
    {
        MinimapWorldRootContainer root = currentWorld().getContainer().getRoot();
        List<WorldState> worlds = new ArrayList<>();
        for (MinimapWorld world : root.getAllWorldsIterable())
        {
            List<SetState> sets = new ArrayList<>();
            for (WaypointSet set : world.getIterableWaypointSets())
            {
                List<Waypoint> waypoints = new ArrayList<>();
                set.addTo(waypoints);
                sets.add(new SetState(set, set.getName(), List.copyOf(waypoints)));
            }
            worlds.add(new WorldState(
                    world,
                    world.getContainer(),
                    world.getNode(),
                    world.getDimId(),
                    world.getCurrentWaypointSetId(),
                    List.copyOf(sets)));
        }
        return new Snapshot(root, List.copyOf(worlds));
    }

    static boolean sameState(Snapshot first, Snapshot second)
    {
        if (first.root() != second.root() || first.worlds().size() != second.worlds().size())
        {
            return false;
        }

        for (WorldState firstWorld : first.worlds())
        {
            WorldState secondWorld = findWorldState(second.worlds(), firstWorld.world());
            if (secondWorld == null || firstWorld.container() != secondWorld.container() ||
                    !Objects.equals(firstWorld.node(), secondWorld.node()) ||
                    !Objects.equals(firstWorld.dimension(), secondWorld.dimension()) ||
                    !Objects.equals(firstWorld.currentSet(), secondWorld.currentSet()) ||
                    firstWorld.sets().size() != secondWorld.sets().size())
            {
                return false;
            }

            for (SetState firstSet : firstWorld.sets())
            {
                SetState secondSet = findSetState(secondWorld.sets(), firstSet.set());
                if (secondSet == null || !Objects.equals(firstSet.name(), secondSet.name()) ||
                        !sameWaypoints(firstSet.waypoints(), secondSet.waypoints()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    static void restoreSnapshot(Snapshot snapshot) throws IOException
    {
        MinimapWorldRootContainer currentRoot = currentWorld().getContainer().getRoot();
        if (currentRoot != snapshot.root())
        {
            throw new IOException("halfmasa.error.waypoint_history_world_changed");
        }

        MinimapSession session = session();
        Set<MinimapWorld> desiredWorlds = Collections.newSetFromMap(new IdentityHashMap<>());
        for (WorldState state : snapshot.worlds())
        {
            desiredWorlds.add(state.world());
        }

        List<MinimapWorld> currentWorlds = worldsIn(currentRoot);
        for (MinimapWorld world : currentWorlds)
        {
            if (!desiredWorlds.contains(world))
            {
                Path worldFile = session.getWorldManagerIO().getWorldFile(world);
                Files.deleteIfExists(worldFile);
                world.getContainer().removeWorld(world.getNode());
            }
        }

        for (WorldState state : snapshot.worlds())
        {
            MinimapWorld world = state.world();
            if (!containsIdentity(worldsIn(currentRoot), world))
            {
                MinimapWorld conflicting = findWorldByNode(state.container(), state.node());
                if (conflicting != null)
                {
                    state.container().removeWorld(state.node());
                }
                world.setContainer(state.container());
                world.setNode(state.node());
                state.container().addWorld(world);
            }

            world.setDimId(state.dimension());
            restoreSets(world, state.sets());
            world.setCurrentWaypointSetId(state.currentSet());
            session.getWorldManagerIO().saveWorld(world);
        }
    }

    private static void restoreSets(MinimapWorld world, List<SetState> desiredSets)
    {
        List<WaypointSet> existingSets = new ArrayList<>();
        for (WaypointSet set : world.getIterableWaypointSets())
        {
            existingSets.add(set);
        }

        for (WaypointSet existing : existingSets)
        {
            SetState desired = findSetState(desiredSets, existing);
            if (desired == null)
            {
                world.removeWaypointSet(existing.getName());
            }
        }

        for (SetState desired : desiredSets)
        {
            WaypointSet current = world.getWaypointSet(desired.name());
            if (current != desired.set())
            {
                if (current != null)
                {
                    world.removeWaypointSet(desired.name());
                }
                world.addWaypointSet(desired.set());
            }
            desired.set().clear();
            desired.set().addAll(desired.waypoints());
        }
    }

    private static List<MinimapWorld> worldsIn(MinimapWorldRootContainer root)
    {
        List<MinimapWorld> worlds = new ArrayList<>();
        for (MinimapWorld world : root.getAllWorldsIterable())
        {
            worlds.add(world);
        }
        return worlds;
    }

    private static MinimapWorld findWorldByNode(MinimapWorldContainer container, String node)
    {
        for (MinimapWorld world : container.getWorlds())
        {
            if (Objects.equals(node, world.getNode()))
            {
                return world;
            }
        }
        return null;
    }

    private static WorldState findWorldState(List<WorldState> states, MinimapWorld world)
    {
        for (WorldState state : states)
        {
            if (state.world() == world)
            {
                return state;
            }
        }
        return null;
    }

    private static SetState findSetState(List<SetState> states, WaypointSet set)
    {
        for (SetState state : states)
        {
            if (state.set() == set)
            {
                return state;
            }
        }
        return null;
    }

    private static boolean sameWaypoints(List<Waypoint> first, List<Waypoint> second)
    {
        if (first.size() != second.size())
        {
            return false;
        }
        for (int index = 0; index < first.size(); index++)
        {
            if (first.get(index) != second.get(index))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean containsIdentity(List<MinimapWorld> worlds, MinimapWorld target)
    {
        for (MinimapWorld world : worlds)
        {
            if (world == target)
            {
                return true;
            }
        }
        return false;
    }

    private static int removeDuplicates(MinimapWorld world, String onlySet)
    {
        int removed = 0;
        for (WaypointSet set : world.getIterableWaypointSets())
        {
            if (onlySet != null && !onlySet.equals(set.getName()))
            {
                continue;
            }

            Set<CoordinatesAndNote> coordinates = new HashSet<>();
            List<Waypoint> duplicates = new ArrayList<>();
            for (Waypoint waypoint : set.getWaypoints())
            {
                if (!coordinates.add(new CoordinatesAndNote(
                        waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.getName())))
                {
                    duplicates.add(waypoint);
                }
            }
            set.removeAll(duplicates);
            removed += duplicates.size();
        }
        return removed;
    }

    private static boolean isExportable(Waypoint waypoint)
    {
        return !waypoint.isTemporary() && !waypoint.isServerWaypoint() && !isThirdParty(waypoint);
    }

    private static boolean isThirdParty(Waypoint waypoint)
    {
        try
        {
            return (boolean) waypoint.getClass().getMethod("isThirdParty").invoke(waypoint);
        }
        catch (ReflectiveOperationException ignored)
        {
            return false;
        }
    }

    private static String dimensionId(MinimapWorld world)
    {
        //#if MC >= 1.21.11
        return world.getDimId() == null ? "" : world.getDimId().identifier().toString();
        //#else
        //$$ return world.getDimId() == null ? "" : world.getDimId().location().toString();
        //#endif
    }

    private static String dimensionKeyId(ResourceKey<Level> dimensionKey)
    {
        //#if MC >= 1.21.11
        return dimensionKey == null ? "" : dimensionKey.identifier().toString();
        //#else
        //$$ return dimensionKey == null ? "" : dimensionKey.location().toString();
        //#endif
    }

    private static String equivalentDimensionId(MinimapWorld world)
    {
        try
        {
            Object location = world.getContainer().getClass().getMethod("getEquivalentDimId")
                    .invoke(world.getContainer());
            if (location == null)
            {
                return "";
            }
            try
            {
                Object unknown = MinimapWorldContainer.class.getField("UNKNOWN_DIM_ID").get(null);
                if (location.equals(unknown))
                {
                    return "";
                }
            }
            catch (ReflectiveOperationException ignored)
            {
            }
            return location.toString();
        }
        catch (ReflectiveOperationException ignored)
        {
            return "";
        }
    }

    private static String encode(String prefix, Object bundle) throws IOException
    {
        byte[] json = GSON.toJson(bundle).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed))
        {
            gzip.write(json);
        }
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
    }

    private static <T> T decode(String encoded, String prefix, Class<T> type) throws IOException
    {
        try
        {
            byte[] compressed = Base64.getUrlDecoder().decode(encoded.substring(prefix.length()));
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed)))
            {
                return GSON.fromJson(new String(gzip.readAllBytes(), StandardCharsets.UTF_8), type);
            }
        }
        catch (IllegalArgumentException | JsonParseException exception)
        {
            throw new IOException("halfmasa.error.waypoint_bundle_invalid", exception);
        }
    }

    private static MinimapWorld currentWorld()
    {
        MinimapWorld world = session().getWorldManager().getCurrentWorld();
        if (world == null)
        {
            throw new IllegalStateException("halfmasa.error.xaero_world_missing");
        }
        return world;
    }

    private static MinimapSession session()
    {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null)
        {
            throw new IllegalStateException("halfmasa.error.xaero_not_ready");
        }
        return session;
    }

    public enum ExportScope
    {
        ALL_DIMENSIONS,
        CURRENT_DIMENSION,
        CURRENT_SET
    }

    public record ExportResult(String text, int waypointCount, int setCount, int dimensionCount) {}
    public record ImportResult(int importedCount, int duplicateCount, int dimensionCount) {}
    record Snapshot(MinimapWorldRootContainer root, List<WorldState> worlds) {}
    record WorldState(
            MinimapWorld world,
            MinimapWorldContainer container,
            String node,
            ResourceKey<Level> dimension,
            String currentSet,
            List<SetState> sets) {}
    record SetState(WaypointSet set, String name, List<Waypoint> waypoints) {}
    private record CoordinatesAndNote(int x, int y, int z, String note) {}
    private record PreparedDimension(
            String node,
            ResourceKey<Level> dimensionKey,
            List<String> containerNodes,
            String containerPath,
            String worldPath,
            String localWorldKey,
            String dimensionName,
            String containerName,
            List<PreparedSet> sets) {}
    private record PreparedSet(String name, List<Waypoint> waypoints) {}

    private static final class BundleV2
    {
        int format = 2;
        String createdAt;
        String scope;
        String sourceContainer;
        List<DimensionData> dimensions = new ArrayList<>();
    }

    private static final class BundleV1
    {
        int format = 1;
        String createdAt;
        String sourceWorld;
        List<SetData> sets = new ArrayList<>();
    }

    private static final class DimensionData
    {
        String dimensionId;
        String equivalentDimensionId;
        String node;
        String sourcePath;
        List<String> containerNodes = new ArrayList<>();
        String containerPath;
        String worldPath;
        String localWorldKey;
        String dimensionName;
        String containerName;
        List<SetData> sets = new ArrayList<>();
    }

    private static final class SetData
    {
        String name;
        List<WaypointData> waypoints = new ArrayList<>();
    }

    private static final class WaypointData
    {
        int x;
        int y;
        int z;
        String name;
        String symbol;
        int color;
        int purpose;
        boolean disabled;
        boolean rotation;
        int yaw;
        boolean yIncluded;
        int visibility;

        static WaypointData from(Waypoint waypoint)
        {
            WaypointData data = new WaypointData();
            data.x = waypoint.getX();
            data.y = waypoint.getY();
            data.z = waypoint.getZ();
            data.name = waypoint.getName();
            data.symbol = waypoint.getSymbol();
            data.color = waypoint.getWaypointColor().ordinal();
            data.purpose = waypoint.getPurpose().ordinal();
            data.disabled = waypoint.isDisabled();
            data.rotation = waypoint.isRotation();
            data.yaw = waypoint.getYaw();
            data.yIncluded = waypoint.isYIncluded();
            data.visibility = waypoint.getVisibilityType();
            return data;
        }

        Waypoint toWaypoint()
        {
            Waypoint waypoint = new Waypoint(x, y, z, name == null ? "" : name, symbol == null ? "" : symbol, color);
            WaypointPurpose[] purposes = WaypointPurpose.values();
            if (purpose >= 0 && purpose < purposes.length)
            {
                waypoint.setPurpose(purposes[purpose]);
            }
            waypoint.setDisabled(disabled);
            waypoint.setRotation(rotation);
            waypoint.setYaw(yaw);
            waypoint.setYIncluded(yIncluded);
            waypoint.setVisibilityType(visibility);
            return waypoint;
        }
    }
}
