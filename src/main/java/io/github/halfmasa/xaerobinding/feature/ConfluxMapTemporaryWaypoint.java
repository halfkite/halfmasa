package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Creates a local Conflux Map waypoint and removes stale copies after reconnecting. */
public final class ConfluxMapTemporaryWaypoint implements IClientTickHandler
{
    private static final ConfluxMapTemporaryWaypoint INSTANCE = new ConfluxMapTemporaryWaypoint();
    private static final String API_CLASS = "cn.net.rms.confluxmap.api.ConfluxMapApi";
    private static final String WAYPOINT_API_CLASS = "cn.net.rms.confluxmap.api.WaypointApi";
    private static final String EDIT_CLASS = "cn.net.rms.confluxmap.api.WaypointApi$WaypointEdit";
    private static final String EDIT_BUILDER_CLASS = "cn.net.rms.confluxmap.api.WaypointApi$WaypointEdit$Builder";
    private static final String WAYPOINT_TYPE_CLASS = "cn.net.rms.confluxmap.api.WaypointApi$WaypointType";
    private static final String API_WAYPOINT_CLASS = "cn.net.rms.confluxmap.api.WaypointApi$ApiWaypoint";
    /* Invisible suffix distinguishes our local temporary point from user-created points. */
    private static final String TEMPORARY_NAME_SUFFIX = "\u2063";
    private static final int RECONNECT_CLEANUP_DELAY_TICKS = 10;
    private static final int API_RETRY_DELAY_TICKS = 20;

    private ClientPacketListener observedConnection;
    private final Deque<Position> queuedPositions = new ArrayDeque<>();
    private boolean cleanupPending;
    private boolean operationPending;
    private boolean warnedUnavailable;
    private int cleanupDelayTicks;
    private int retryDelayTicks;
    private int nextWaypointNumber = 1;
    private long sessionGeneration;

    private ConfluxMapTemporaryWaypoint()
    {
    }

    public static ConfluxMapTemporaryWaypoint getInstance()
    {
        return INSTANCE;
    }

    /** Queues a new local temporary waypoint at the current position. */
    public static boolean placeAtPlayer()
    {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue() ||
                !FabricLoader.getInstance().isModLoaded("confluxmap") ||
                client.player == null || client.level == null || client.getConnection() == null)
        {
            return false;
        }

        ConfluxMapTemporaryWaypoint feature = INSTANCE;
        feature.observeConnection(client.getConnection());
        var position = client.player.position();
        feature.queuedPositions.addLast(new Position(
                //#if MC >= 1.21.11
                client.level.dimension().identifier().toString(),
                //#else
                //$$ client.level.dimension().location().toString(),
                //#endif
                position.x,
                position.y,
                position.z,
                feature.nextWaypointNumber++));
        feature.retryDelayTicks = 0;
        return true;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        ClientPacketListener connection = client.getConnection();
        if (connection == null)
        {
            if (this.observedConnection != null)
            {
                this.observedConnection = null;
                this.queuedPositions.clear();
                this.cleanupPending = false;
                this.operationPending = false;
                this.nextWaypointNumber = 1;
                this.sessionGeneration++;
            }
            return;
        }

        this.observeConnection(connection);
        if (client.player == null || client.level == null ||
                !FabricLoader.getInstance().isModLoaded("confluxmap"))
        {
            return;
        }

        if (this.cleanupPending)
        {
            if (this.cleanupDelayTicks > 0)
            {
                this.cleanupDelayTicks--;
                return;
            }

            try
            {
                if (!this.removeStaleTemporaryWaypoints())
                {
                    this.cleanupDelayTicks = API_RETRY_DELAY_TICKS;
                    return;
                }
                this.cleanupPending = false;
            }
            catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
            {
                this.warnUnavailable(exception);
                this.cleanupDelayTicks = API_RETRY_DELAY_TICKS;
                return;
            }
        }

        if (!this.queuedPositions.isEmpty() && !this.operationPending)
        {
            if (this.retryDelayTicks > 0)
            {
                this.retryDelayTicks--;
                return;
            }

            Position position = this.queuedPositions.removeFirst();
            this.submitWaypointMutation(position);
        }
    }

    private void observeConnection(ClientPacketListener connection)
    {
        if (connection != this.observedConnection)
        {
            this.observedConnection = connection;
            this.queuedPositions.clear();
            this.operationPending = false;
            this.cleanupPending = true;
            this.cleanupDelayTicks = RECONNECT_CLEANUP_DELAY_TICKS;
            this.retryDelayTicks = 0;
            this.nextWaypointNumber = 1;
            this.sessionGeneration++;
        }
    }

    private boolean submitWaypointMutation(Position position)
    {
        try
        {
            Object waypointApi = this.getWaypointApi();
            if (waypointApi == null)
            {
                this.queueForRetry(position);
                return true;
            }

            ClassLoader classLoader = ConfluxMapTemporaryWaypoint.class.getClassLoader();
            Class<?> waypointApiClass = Class.forName(WAYPOINT_API_CLASS, true, classLoader);
            Object edit = this.createEdit(position, classLoader);
            Object future = waypointApiClass.getMethod("add", Class.forName(EDIT_CLASS, true, classLoader))
                    .invoke(waypointApi, edit);

            if (!(future instanceof CompletableFuture<?> completableFuture))
            {
                this.queueForRetry(position);
                return false;
            }

            this.operationPending = true;
            long generation = this.sessionGeneration;
            completableFuture.whenComplete((mutation, error) -> Minecraft.getInstance().execute(
                    () -> this.completeMutation(generation, position, mutation, error)));
            return true;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            this.warnUnavailable(exception);
            this.queueForRetry(position);
            return false;
        }
    }

    private void completeMutation(long generation, Position position, Object mutation, Throwable error)
    {
        if (generation != this.sessionGeneration)
        {
            return;
        }

        this.operationPending = false;
        if (error != null || mutation == null)
        {
            this.warnUnavailable(error == null ? new IllegalStateException("Conflux Map returned no waypoint result") : error);
            this.queueForRetry(position);
            return;
        }

        try
        {
            ClassLoader classLoader = ConfluxMapTemporaryWaypoint.class.getClassLoader();
            Class<?> mutationClass = Class.forName(
                    "cn.net.rms.confluxmap.api.WaypointApi$WaypointMutation", true, classLoader);
            Object result = mutationClass.getMethod("result").invoke(mutation);
            String resultName = String.valueOf(result);
            if ("APPLIED".equals(resultName) || "NO_CHANGE".equals(resultName))
            {
                // The requested waypoint has been added or already exists.
            }
            else if ("NO_SESSION".equals(resultName))
            {
                this.queueForRetry(position);
            }
            else
            {
                XaeroWorldBinding.LOGGER.warn("Conflux Map could not create the temporary local waypoint: {}", resultName);
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            this.warnUnavailable(exception);
        }
    }

    private boolean removeStaleTemporaryWaypoints() throws ReflectiveOperationException
    {
        Object waypointApi = this.getWaypointApi();
        if (waypointApi == null)
        {
            return false;
        }

        ClassLoader classLoader = ConfluxMapTemporaryWaypoint.class.getClassLoader();
        Class<?> waypointApiClass = Class.forName(WAYPOINT_API_CLASS, true, classLoader);
        Class<?> apiWaypointClass = Class.forName(API_WAYPOINT_CLASS, true, classLoader);
        Object result = waypointApiClass.getMethod("list").invoke(waypointApi);
        if (!(result instanceof List<?> waypoints))
        {
            return false;
        }

        Method nameMethod = apiWaypointClass.getMethod("name");
        Method idMethod = apiWaypointClass.getMethod("id");
        Method removeMethod = waypointApiClass.getMethod("remove", UUID.class);
        for (Object waypoint : waypoints)
        {
            String name = (String) nameMethod.invoke(waypoint);
            if (name != null && name.endsWith(TEMPORARY_NAME_SUFFIX))
            {
                UUID id = (UUID) idMethod.invoke(waypoint);
                Object future = removeMethod.invoke(waypointApi, id);
                if (future instanceof CompletableFuture<?> completableFuture)
                {
                    completableFuture.whenComplete((mutation, error) -> {
                        if (error != null)
                        {
                            this.warnUnavailable(error);
                        }
                    });
                }
            }
        }
        return true;
    }

    private Object getWaypointApi() throws ReflectiveOperationException
    {
        ClassLoader classLoader = ConfluxMapTemporaryWaypoint.class.getClassLoader();
        Class<?> apiClass = Class.forName(API_CLASS, true, classLoader);
        Object optionalApi = apiClass.getMethod("instance").invoke(null);
        if (!(optionalApi instanceof Optional<?> api) || api.isEmpty())
        {
            return null;
        }
        return apiClass.getMethod("waypoints").invoke(api.get());
    }

    private Object createEdit(Position position, ClassLoader classLoader) throws ReflectiveOperationException
    {
        Class<?> editClass = Class.forName(EDIT_CLASS, true, classLoader);
        Class<?> builderClass = Class.forName(EDIT_BUILDER_CLASS, true, classLoader);
        Class<?> typeClass = Class.forName(WAYPOINT_TYPE_CLASS, true, classLoader);
        Object builder = editClass.getMethod("builder").invoke(null);
        String name = Component.translatable("halfmasa.conflux_map.temporary_waypoint").getString() +
                " " + position.number() + TEMPORARY_NAME_SUFFIX;
        builderClass.getMethod("name", String.class).invoke(builder, name);
        builderClass.getMethod("dimensionId", String.class).invoke(builder, position.dimension());
        builderClass.getMethod("position", double.class, double.class, double.class)
                .invoke(builder, position.x(), position.y(), position.z());
        builderClass.getMethod("colorArgb", int.class).invoke(builder, 0xFFFFD54F);
        builderClass.getMethod("visible", boolean.class).invoke(builder, true);
        builderClass.getMethod("crossDimensionVisible", boolean.class).invoke(builder, false);
        builderClass.getMethod("type", typeClass).invoke(builder, typeClass.getField("NORMAL").get(null));
        builderClass.getMethod("iconItemId", String.class).invoke(builder, "minecraft:lodestone");
        builderClass.getMethod("markerLabel", String.class).invoke(builder, "");
        return builderClass.getMethod("build").invoke(builder);
    }

    private void queueForRetry(Position position)
    {
        this.queuedPositions.addFirst(position);
        this.retryDelayTicks = API_RETRY_DELAY_TICKS;
    }

    private void warnUnavailable(Throwable exception)
    {
        if (!this.warnedUnavailable)
        {
            this.warnedUnavailable = true;
            XaeroWorldBinding.LOGGER.warn(
                    "Conflux Map's local waypoint API is unavailable; the temporary waypoint could not be updated",
                    exception);
        }
    }

    private record Position(String dimension, double x, double y, double z, int number)
    {
    }
}
