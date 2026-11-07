//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.ConfigTypeWrapper;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.MasaFavoritesConfigTypeAccessor;

/** Bounded adapters for known public config containers, plus observed MaLiLib rows. */
public final class MasaConfigCatalog
{
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<IConfigBase, Entry> OBJECTS = new IdentityHashMap<>();
    private static boolean discovered;

    private MasaConfigCatalog() { }

    public static void discover()
    {
        if (discovered) return;
        discovered = true;
        scan("halfmasa", Configs.class);
        for (IConfigBase config : Configs.ALL) observe("halfmasa", config);
        scanOptional("malilib", "fi.dy.masa.malilib.MaLiLibConfigs");
        for (String mod : List.of("minihud", "litematica", "tweakeroo", "itemscroller", "malilib"))
        {
            if (!FabricLoader.getInstance().isModLoaded(mod)) continue;
            for (String container : List.of("Configs", "Hotkeys", "FeatureToggle", "InfoToggle", "RendererToggle", "StructureToggle"))
            {
                String name = "fi.dy.masa." + mod + ".config." + container;
                try { scan(mod, Class.forName(name)); }
                catch (ClassNotFoundException absent) { /* Not every mod defines every container. */ }
                catch (LinkageError error) { XaeroWorldBinding.LOGGER.warn("Unable to discover {}", name, error); }
            }
        }
        if (FabricLoader.getInstance().isModLoaded("masa_gadget_mod"))
            scanOptional("masa_gadget_mod", "com.plusls.MasaGadget.game.Configs");
        if (FabricLoader.getInstance().isModLoaded("tweakermore"))
            scanOptional("tweakermore", "me.fallenbreath.tweakermore.config.TweakerMoreConfigs");
        scanMagicLib();
        if (FabricLoader.getInstance().isModLoaded("lpctools"))
        {
            try
            {
                Object page = Class.forName("lpctools.LPCTools").getField("page").get(null);
                scanLpcTree(page, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
            }
            catch (ReflectiveOperationException | LinkageError error)
            { XaeroWorldBinding.LOGGER.warn("Unable to index LPCTools config tree", error); }
        }
        for (var mod : MasaGadgetCompat.pages())
        {
            if (io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.supplier(mod) == null || io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod).equals(MasaFavoritesScreen.SCREEN_ID)) continue;
            try
            {
                var screen = io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.supplier(mod).get();
                if (!(screen instanceof fi.dy.masa.malilib.gui.GuiConfigsBase configs)) continue;
                // Public config containers cover inactive categories too, without opening a GUI.
                String packageName = screen.getClass().getPackageName();
                int gui = packageName.indexOf(".gui");
                if (gui >= 0)
                    for (String name : List.of("Configs", "Hotkeys", "FeatureToggle", "InfoToggle", "RendererToggle", "StructureToggle"))
                        scanOptional(io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod), packageName.substring(0, gui) + ".config." + name);
                for (var wrapper : configs.getConfigs()) observe(io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod), wrapper.getConfig());
            }
            catch (RuntimeException | LinkageError error)
            { XaeroWorldBinding.LOGGER.warn("Unable to index Masa config page {}", io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod), error); }
        }
    }

    private static void scanMagicLib()
    {
        // The public global registry includes every category of all MagicLib config providers.
        try
        {
            Class<?> global = Class.forName("top.hendrixshen.magiclib.impl.malilib.config.GlobalConfigManager");
            Class<?> container = Class.forName("top.hendrixshen.magiclib.impl.malilib.config.ConfigContainer");
            Class<?> manager = Class.forName("top.hendrixshen.magiclib.api.malilib.config.MagicConfigManager");
            Object registry = global.getMethod("getInstance").invoke(null);
            if (global.getMethod("getAllContainers").invoke(registry) instanceof Iterable<?> configs)
                for (Object value : configs)
                {
                    Object owner = container.getMethod("getConfigManager").invoke(value);
                    if (owner != null && container.getMethod("getConfig").invoke(value) instanceof IConfigBase config)
                        observe((String) manager.getMethod("getIdentifier").invoke(owner), config);
                }
        }
        catch (ClassNotFoundException absent) { /* MagicLib is optional. */ }
        catch (ReflectiveOperationException | LinkageError error)
        { XaeroWorldBinding.LOGGER.warn("Unable to index MagicLib config registry", error); }
    }

    private static void scanLpcTree(Object node, java.util.Set<Object> visited) throws ReflectiveOperationException
    {
        if (node == null || !visited.add(node)) return;
        if (node instanceof IConfigBase config && !node.getClass().getName().equals("lpctools.lpcfymasaapi.LPCConfigList"))
            observe("lpctools", config);
        Class<?> readable = Class.forName("lpctools.lpcfymasaapi.interfaces.ILPCConfigReadable");
        if (readable.isInstance(node) && readable.getMethod("getConfigs").invoke(node) instanceof Iterable<?> children)
            for (Object child : children) scanLpcTree(child, visited);
    }

    private static void scanOptional(String mod, String name)
    {
        try { scan(mod, Class.forName(name)); }
        catch (ClassNotFoundException absent) { /* Optional public container does not exist. */ }
        catch (LinkageError error) { XaeroWorldBinding.LOGGER.warn("Unable to discover {}", name, error); }
    }

    private static void scan(String mod, Class<?> owner)
    {
        Field[] fields = owner.getFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        // Field paths disambiguate identical names across categories and survive UI translations.
        for (Field field : fields)
        {
            if (!Modifier.isStatic(field.getModifiers()) || !IConfigBase.class.isAssignableFrom(field.getType())) continue;
            try
            {
                if (field.get(null) instanceof IConfigBase config)
                    register(mod, owner.getName() + "/" + field.getName(), config);
            }
            catch (IllegalAccessException error) { XaeroWorldBinding.LOGGER.warn("Unable to read {}", field, error); }
        }
        Class<?>[] children = owner.getDeclaredClasses();
        Arrays.sort(children, Comparator.comparing(Class::getName));
        for (Class<?> child : children)
            if (Modifier.isPublic(child.getModifiers())) scan(mod, child);
        for (Field field : fields)
        {
            if (!Modifier.isStatic(field.getModifiers()) || !Iterable.class.isAssignableFrom(field.getType())) continue;
            try
            {
                if (field.get(null) instanceof Iterable<?> values)
                    for (Object value : values)
                        if (value instanceof IConfigBase config && !OBJECTS.containsKey(canonical(config)))
                            register(mod, owner.getName() + "/" + config.getName(), config);
            }
            catch (IllegalAccessException error) { XaeroWorldBinding.LOGGER.warn("Unable to read {}", field, error); }
        }
    }

    private static IConfigBase canonical(IConfigBase config)
    {
        while (true)
        {
            if (config instanceof BooleanHotkeyGuiWrapper wrapper) config = wrapper.getBooleanConfig();
            else if (config instanceof ConfigTypeWrapper)
                config = ((MasaFavoritesConfigTypeAccessor) config).halfmasa$getWrappedConfig();
            else return config;
        }
    }

    private static Entry register(String mod, String path, IConfigBase config)
    {
        if (config == null || config instanceof ConfigGroupHeader || config.getName().endsWith("Expanded")) return null;
        IConfigBase identity = canonical(config);
        Entry existing = OBJECTS.get(identity);
        if (existing != null) return existing;
        IConfigBase editable = identity;
        if ((identity.getType() == ConfigType.BOOLEAN || identity instanceof IHotkeyTogglable) &&
                identity instanceof IConfigBoolean booleanConfig && identity instanceof IHotkey hotkey &&
                !(identity instanceof ConfigBooleanHotkeyed) && !(identity instanceof BooleanHotkeyGuiWrapper) &&
                hotkey.getKeybind() != null)
            editable = new BooleanHotkeyGuiWrapper(identity.getName(), booleanConfig, hotkey.getKeybind());
        Entry entry = new Entry(mod + ":" + path, mod, identity, editable);
        Entry collision = ENTRIES.putIfAbsent(entry.key(), entry);
        if (collision != null && collision.identity() != identity)
        {
            XaeroWorldBinding.LOGGER.warn("Ambiguous Masa config {}; refusing to alias different objects", entry.key());
            return null;
        }
        OBJECTS.put(identity, entry);
        OBJECTS.put(editable, entry);
        return entry;
    }

    public static Entry observe(String mod, IConfigBase config)
    {
        discover();
        if (config == null) return null;
        Entry known = OBJECTS.get(canonical(config));
        if (known != null) return known;
        return register(mod, "observed/" + canonical(config).getClass().getName() + "/" + config.getName(), config);
    }

    public static Entry lookup(IConfigBase config) { discover(); return config == null ? null : OBJECTS.get(canonical(config)); }

    public static Entry find(String key) { discover(); return ENTRIES.get(key); }
    public static List<Entry> entries() { discover(); return new ArrayList<>(ENTRIES.values()); }

    public record Entry(String key, String modId, IConfigBase identity, IConfigBase editable) { }
}
//#endif
