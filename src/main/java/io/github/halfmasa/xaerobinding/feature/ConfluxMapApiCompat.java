package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.ConfluxMapCapabilities;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Optional public API adapter. No API classes are linked or bundled into HalfMasa. */
public final class ConfluxMapApiCompat implements IClientTickHandler
{
    private static final ConfluxMapApiCompat INSTANCE = new ConfluxMapApiCompat();
    private static final String API = "cn.net.rms.confluxmap.api.";
    private final List<Binding> bindings = new ArrayList<>();
    private Method instance;
    private Object observedApi;
    private boolean unavailable;

    private ConfluxMapApiCompat() {}
    public static ConfluxMapApiCompat getInstance() { return INSTANCE; }

    @Override public void onClientTick(Minecraft client)
    {
        if (unavailable || !FabricLoader.getInstance().isModLoaded("confluxmap")) return;
        try
        {
            if (instance == null) instance = type("ConfluxMapApi").getMethod("instance");
            Object api = ((Optional<?>)instance.invoke(null)).orElse(null);
            if (!Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue()) api = null;
            if (api == observedApi) return;
            closeBindings();
            observedApi = api;
            if (api == null) return;
            Object events = type("ConfluxMapApi").getMethod("events").invoke(api);
            if (ConfluxMapCapabilities.event("teleportLandingResolve"))
            {
                Method setY = type("ConfluxMapEvents$TeleportLandingResolve").getMethod("setLandingY", int.class);
                bind(events, "teleportLandingResolve", payload ->
                        setY.invoke(payload, Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.getIntegerValue()));
            }
            if (ConfluxMapCapabilities.event("waypointListOpening"))
            {
                Class<?> opening = type("ConfluxMapEvents$WaypointListOpening");
                Method hotkey = opening.getMethod("openedFromHotkey");
                Method shared = opening.getMethod("sharedWaypointsEnabled");
                Method dimensions = opening.getMethod("requestAllDimensions");
                Class<?> tab = type("ConfluxMapEvents$WaypointListTab");
                Object publicTab = tab.getField("PUBLIC").get(null);
                Method requestTab = opening.getMethod("requestTab", tab);
                bind(events, "waypointListOpening", payload -> {
                    if (!Boolean.TRUE.equals(hotkey.invoke(payload))) return;
                    if (Configs.CONFLUX_MAP_ALL_DIMENSIONS.getBooleanValue()) dimensions.invoke(payload);
                    if (Configs.CONFLUX_MAP_PUBLIC_WAYPOINTS.getBooleanValue() && Boolean.TRUE.equals(shared.invoke(payload)))
                        requestTab.invoke(payload, publicTab);
                });
            }
            if (ConfluxMapCapabilities.event("locationActionPerformed"))
            {
                Method action = type("ConfluxMapEvents$LocationActionEvent").getMethod("action");
                Object actions = type("ConfluxMapApi").getMethod("actions").invoke(api);
                Method close = type("ActionApi").getMethod("closeFullscreenMap");
                bind(events, "locationActionPerformed", payload -> {
                    if (Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT.getBooleanValue() && "TELEPORT".equals(String.valueOf(action.invoke(payload))))
                        close.invoke(actions);
                });
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            closeBindings();
            unavailable = true;
            XaeroWorldBinding.LOGGER.warn("Conflux Map public API contract is unavailable; API extensions disabled", exception);
        }
    }

    private void bind(Object events, String name, Listener listener) throws ReflectiveOperationException
    {
        Object event = type("ConfluxMapEvents").getMethod(name).invoke(events);
        Binding binding = new Binding(name, listener);
        binding.handle = (AutoCloseable)type("Event").getMethod("register", Consumer.class).invoke(event, binding);
        bindings.add(binding);
    }

    private static Class<?> type(String name) throws ClassNotFoundException
    { return Class.forName(API + name, true, ConfluxMapApiCompat.class.getClassLoader()); }

    private void closeBindings()
    {
        for (Binding binding : bindings) binding.close();
        bindings.clear();
    }

    @FunctionalInterface private interface Listener { void accept(Object payload) throws ReflectiveOperationException; }

    private static final class Binding implements Consumer<Object>
    {
        private final String name;
        private final Listener listener;
        private AutoCloseable handle;
        private boolean failed;
        private Binding(String name, Listener listener) { this.name = name; this.listener = listener; }
        @Override public void accept(Object payload)
        {
            if (failed || !Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue()) return;
            try { listener.accept(payload); }
            catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
            {
                failed = true;
                close();
                XaeroWorldBinding.LOGGER.warn("Conflux Map API listener {} failed and was unregistered", name, exception);
            }
        }
        private void close()
        {
            if (handle == null) return;
            try { handle.close(); }
            catch (Exception exception) { XaeroWorldBinding.LOGGER.warn("Cannot unregister Conflux Map listener {}", name, exception); }
            handle = null;
        }
    }
}
//#endif
