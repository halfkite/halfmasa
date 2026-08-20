package io.github.halfmasa.xaerobinding.compat;

import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

public final class LitematicaEasyPlaceCompat
{
    private static final boolean LITEMATICA_LOADED = FabricLoader.getInstance().isModLoaded("litematica");
    private static Method handleEasyPlace;
    private static boolean initializationAttempted;
    private static boolean failureLogged;

    private LitematicaEasyPlaceCompat() {}

    public static boolean tryHandleEasyPlace()
    {
        Method method = getHandleEasyPlace();
        if (method == null)
        {
            return false;
        }

        try
        {
            Object result = method.invoke(null);
            return result instanceof Boolean handled && handled;
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            logFailure(exception);
            return false;
        }
    }

    private static Method getHandleEasyPlace()
    {
        if (!LITEMATICA_LOADED)
        {
            return null;
        }
        if (initializationAttempted)
        {
            return handleEasyPlace;
        }

        initializationAttempted = true;
        try
        {
            Class<?> utilityClass = Class.forName("fi.dy.masa.litematica.util.EasyPlaceUtils");
            try
            {
                handleEasyPlace = utilityClass.getMethod("handleEasyPlaceWithMessage");
            }
            catch (NoSuchMethodException ignored)
            {
                // Older Litematica builds exposed the same action without a message
                handleEasyPlace = utilityClass.getMethod("handleEasyPlace");
            }
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            logFailure(exception);
        }
        return handleEasyPlace;
    }

    private static void logFailure(Exception exception)
    {
        if (!failureLogged)
        {
            failureLogged = true;
            XaeroWorldBinding.LOGGER.warn("Litematica easy-place compatibility is unavailable", exception);
        }
    }
}
