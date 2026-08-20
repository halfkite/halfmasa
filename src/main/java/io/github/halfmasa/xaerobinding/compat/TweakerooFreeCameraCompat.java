package io.github.halfmasa.xaerobinding.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

public final class TweakerooFreeCameraCompat
{
    private static final boolean TWEAKEROO_LOADED = FabricLoader.getInstance().isModLoaded("tweakeroo");
    private static Access access;
    private static boolean initializationAttempted;
    private static boolean failureLogged;

    private TweakerooFreeCameraCompat() {}

    public static void initialize()
    {
        getAccess();
    }

    public static Entity getActiveCamera(Minecraft client)
    {
        Access current = getAccess();
        if (current == null)
        {
            return null;
        }

        try
        {
            if (!getBoolean(current.freeCameraToggle()) || getBoolean(current.playerInputsConfig()))
            {
                return null;
            }

            Object camera = current.getCamera().invoke(null);
            Entity activeCamera = client.getCameraEntity();
            return camera instanceof Entity && camera == activeCamera ? activeCamera : null;
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            logFailure(exception);
            return null;
        }
    }

    public static double getBlockReach(Minecraft client)
    {
        Access current = getAccess();
        if (current != null)
        {
            try
            {
                if (getBoolean(current.blockReachToggle()))
                {
                    return ((Number) current.getReachDistance().invoke(current.reachDistanceConfig())).doubleValue();
                }
            }
            catch (ReflectiveOperationException | RuntimeException exception)
            {
                logFailure(exception);
            }
        }

        return client.player != null ? client.player.blockInteractionRange() : 4.5D;
    }

    private static Access getAccess()
    {
        if (!TWEAKEROO_LOADED)
        {
            return null;
        }
        if (initializationAttempted)
        {
            return access;
        }

        initializationAttempted = true;
        try
        {
            Class<?> featureToggleClass = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
            Object freeCameraToggle = getStaticField(featureToggleClass, "TWEAK_FREE_CAMERA");
            Object blockReachToggle = getStaticField(featureToggleClass, "TWEAK_BLOCK_REACH_OVERRIDE");

            Class<?> genericConfigsClass = Class.forName("fi.dy.masa.tweakeroo.config.Configs$Generic");
            Object playerInputsConfig = getStaticField(genericConfigsClass, "FREE_CAMERA_PLAYER_INPUTS");
            Object reachDistanceConfig = getStaticField(genericConfigsClass, "BLOCK_REACH_DISTANCE");

            Class<?> cameraClass = Class.forName("fi.dy.masa.tweakeroo.util.CameraEntity");
            Method getCamera = cameraClass.getMethod("getCamera");
            Method getReachDistance = reachDistanceConfig.getClass().getMethod("getDoubleValue");

            access = new Access(
                    freeCameraToggle,
                    blockReachToggle,
                    playerInputsConfig,
                    reachDistanceConfig,
                    getCamera,
                    getReachDistance);
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            logFailure(exception);
        }

        return access;
    }

    private static Object getStaticField(Class<?> owner, String name) throws ReflectiveOperationException
    {
        Field field = owner.getField(name);
        return field.get(null);
    }

    private static boolean getBoolean(Object config) throws ReflectiveOperationException
    {
        return (Boolean) config.getClass().getMethod("getBooleanValue").invoke(config);
    }

    private static void logFailure(Exception exception)
    {
        if (!failureLogged)
        {
            failureLogged = true;
            XaeroWorldBinding.LOGGER.warn("Tweakeroo free-camera compatibility is unavailable", exception);
        }
    }

    private record Access(
            Object freeCameraToggle,
            Object blockReachToggle,
            Object playerInputsConfig,
            Object reachDistanceConfig,
            Method getCamera,
            Method getReachDistance)
    {
    }
}
