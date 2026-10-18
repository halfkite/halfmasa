package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import io.github.halfmasa.xaerobinding.config.Configs;

/** 0.1.10/0.1.11 map adapter; never replaces live unknown-column ground discovery. */
public final class ConfluxMapLegacyTeleport
{
    private final Method live, captured, ground, known, surface, blockX, blockZ, viewSession, dimension, world, exact;
    private final Field teleport;

    public ConfluxMapLegacyTeleport(Class<?> screen) throws ReflectiveOperationException
    {
        ClassLoader loader = screen.getClassLoader();
        Class<?> target = Class.forName("cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapLocationMenu$Target", false, loader);
        Class<?> lookup = Class.forName("cn.net.rms.confluxmap.core.store.ColumnStore$SurfaceLookup", false, loader);
        live = method(screen, "viewingLiveSession");
        captured = method(screen, "capturedGroundAt", int.class, int.class);
        ground = method(target, "ground"); blockX = method(target, "blockX"); blockZ = method(target, "blockZ");
        known = method(lookup, "known"); surface = method(lookup, "surfaceY");
        viewSession = method(screen, "viewSession");
        dimension = method(viewSession.getReturnType(), "dimension"); world = method(viewSession.getReturnType(), "world");
        teleport = screen.getDeclaredField("groundTeleport"); teleport.setAccessible(true);
        exact = method(teleport.getType(), "teleportExact", double.class, double.class, double.class,
                dimension.getReturnType(), world.getReturnType());
    }

    public boolean needsFallback(Object screen, Object target) throws ReflectiveOperationException
    {
        if (!Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue() || target == null) return false;
        Object saved = captured.invoke(screen, blockX.invoke(target), blockZ.invoke(target));
        Object estimate = ground.invoke(target);
        // A captured height is authoritative; a prediction must not mask a captured void.
        if (Boolean.TRUE.equals(known.invoke(saved))) return ((OptionalInt)surface.invoke(saved)).isEmpty();
        OptionalInt predictedY = (OptionalInt)surface.invoke(estimate);
        if (Boolean.TRUE.equals(known.invoke(estimate)) && predictedY.isEmpty()) return true;
        return !Boolean.TRUE.equals(live.invoke(screen)) && predictedY.isEmpty();
    }

    public boolean teleport(Object screen, Object target) throws ReflectiveOperationException
    {
        if (!needsFallback(screen, target)) return false;
        Object session = viewSession.invoke(screen);
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return false;
        // Match the native firstStage policy: an already loaded same-dimension chunk
        // supplies real ground, even if the cached column is void or unknown.
        //#if MC >= 1.21.11
        if (client.level.dimension().identifier().toString().equals(String.valueOf(dimension.invoke(session))) &&
        //#else
        //$$ if (client.level.dimension().location().toString().equals(String.valueOf(dimension.invoke(session))) &&
        //#endif
                client.level.getChunkSource().getChunk(((Number)blockX.invoke(target)).intValue() >> 4,
                        ((Number)blockZ.invoke(target)).intValue() >> 4, ChunkStatus.FULL, false) != null) return false;
        exact.invoke(teleport.get(screen), ((Number)blockX.invoke(target)).doubleValue() + 0.5,
                (double)Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.getIntegerValue(),
                ((Number)blockZ.invoke(target)).doubleValue() + 0.5,
                dimension.invoke(session), world.invoke(session));
        return true;
    }

    private static Method method(Class<?> type, String name, Class<?>... args) throws NoSuchMethodException
    {
        Method method = type.getDeclaredMethod(name, args);
        method.setAccessible(true);
        return method;
    }
}
//#endif
