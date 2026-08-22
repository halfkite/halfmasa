package io.github.halfmasa.xaerobinding.waypoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.ChatFormatting;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldContainer;

public final class WaypointBundleService
{
    public static final String PREFIX = "XWB1:";
    private static final Gson GSON = new Gson();

    private WaypointBundleService()
    {
    }

    public static ExportResult exportCurrentWorld() throws IOException
    {
        MinimapWorld world = currentWorld();
        BundleData bundle = new BundleData();
        bundle.createdAt = Instant.now().toString();
        bundle.sourceWorld = world.getFullPath().toString();

        int count = 0;
        for (WaypointSet set : world.getIterableWaypointSets())
        {
            SetData setData = new SetData();
            setData.name = set.getName();

            for (Waypoint waypoint : set.getWaypoints())
            {
                if (waypoint.isTemporary() || waypoint.isServerWaypoint() || waypoint.isThirdParty())
                {
                    continue;
                }
                setData.waypoints.add(WaypointData.from(waypoint));
                count++;
            }
            bundle.sets.add(setData);
        }

        byte[] json = GSON.toJson(bundle).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed))
        {
            gzip.write(json);
        }

        String encoded = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
        return new ExportResult(encoded, count, bundle.sets.size());
    }

    public static ImportResult importIntoCurrentWorld(String encoded) throws IOException
    {
        if (encoded == null || !encoded.strip().startsWith(PREFIX))
        {
            throw new IOException("Clipboard does not contain an XWB1 waypoint bundle");
        }

        BundleData bundle;
        try
        {
            byte[] compressed = Base64.getUrlDecoder().decode(encoded.strip().substring(PREFIX.length()));
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed)))
            {
                bundle = GSON.fromJson(new String(gzip.readAllBytes(), StandardCharsets.UTF_8), BundleData.class);
            }
        }
        catch (IllegalArgumentException | JsonParseException exception)
        {
            throw new IOException("Invalid waypoint bundle", exception);
        }

        if (bundle == null || bundle.format != 1 || bundle.sets == null)
        {
            throw new IOException("Unsupported waypoint bundle version");
        }

        MinimapWorld world = currentWorld();
        int imported = 0;
        for (SetData setData : bundle.sets)
        {
            if (setData == null || setData.name == null || setData.waypoints == null)
            {
                continue;
            }

            WaypointSet set = world.getWaypointSet(setData.name);
            if (set == null)
            {
                world.addWaypointSet(setData.name);
                set = world.getWaypointSet(setData.name);
            }

            for (WaypointData waypointData : setData.waypoints)
            {
                if (waypointData != null)
                {
                    set.add(waypointData.toWaypoint());
                    imported++;
                }
            }
        }

        int removed = removeDuplicates(world, null);
        session().getWorldManagerIO().saveWorld(world);
        return new ImportResult(imported, removed);
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

    public static List<String> createChatShareMessages(boolean allSets) throws ReflectiveOperationException
    {
        MinimapSession session = session();
        MinimapWorld world = currentWorld();
        Object sharing = session.getWaypointSession().getSharing();
        var destinationMethod = sharing.getClass().getDeclaredMethod(
                "getSharedDestinationDetails", MinimapWorldContainer.class);
        destinationMethod.setAccessible(true);
        String destination = (String) destinationMethod.invoke(sharing, world.getContainer());

        List<String> messages = new ArrayList<>();
        for (WaypointSet set : world.getIterableWaypointSets())
        {
            if (!allSets && !Objects.equals(world.getCurrentWaypointSetId(), set.getName()))
            {
                continue;
            }
            for (Waypoint waypoint : set.getWaypoints())
            {
                if (!waypoint.isTemporary() && !waypoint.isServerWaypoint() && !waypoint.isThirdParty())
                {
                    messages.add(createChatShareMessage(waypoint, destination));
                }
            }
        }
        return messages;
    }

    private static String createChatShareMessage(Waypoint waypoint, String destination)
    {
        String name = ChatFormatting.stripFormatting(waypoint.getNameSafe("^col^"));
        String initials = ChatFormatting.stripFormatting(waypoint.getInitialsSafe("^col^"));
        String y = waypoint.isYIncluded() ? Integer.toString(waypoint.getY()) : "~";
        return "xaero-waypoint:" + name + ':' + initials + ':' + waypoint.getX() + ':' + y + ':'
                + waypoint.getZ() + ':' + waypoint.getWaypointColor().ordinal() + ':' + waypoint.isRotation()
                + ':' + waypoint.getYaw() + ':' + destination;
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

    private static MinimapWorld currentWorld()
    {
        MinimapWorld world = session().getWorldManager().getCurrentWorld();
        if (world == null)
        {
            throw new IllegalStateException("Xaero has no active waypoint world");
        }
        return world;
    }

    private static MinimapSession session()
    {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null)
        {
            throw new IllegalStateException("Xaero Minimap is not ready");
        }
        return session;
    }

    public record ExportResult(String text, int waypointCount, int setCount) {}
    public record ImportResult(int importedCount, int duplicateCount) {}
    private record CoordinatesAndNote(int x, int y, int z, String note) {}

    private static final class BundleData
    {
        int format = 1;
        String createdAt;
        String sourceWorld;
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
