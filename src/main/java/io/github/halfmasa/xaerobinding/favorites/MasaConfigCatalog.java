//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.ConfigTypeWrapper;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.MasaFavoritesConfigTypeAccessor;

/** Bounded adapters for known public config containers, plus observed MaLiLib rows. */
public final class MasaConfigCatalog
{
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<IConfigBase, Entry> OBJECTS = new IdentityHashMap<>();
    private static final Set<String> INDEXED_MODS = new HashSet<>();
    private static final Set<String> INDEXED_MAGIC_MODS = new HashSet<>();
    private static final Set<String> INDEXED_PACKAGES = new HashSet<>();
    private static final Set<String> PROBED_PAGES = new HashSet<>();
    private static final List<String> CONTAINERS = List.of("Configs", "Hotkeys", "FeatureToggle", "InfoToggle", "RendererToggle", "StructureToggle");
    private static final List<String> MASA_MODS = List.of("minihud", "litematica", "tweakeroo", "itemscroller", "malilib");
    private static boolean discovered;

    private MasaConfigCatalog() { }

    public static void discover()
    {
        if (discovered) return;
        discovered = true;
        indexMod("halfmasa");
        MASA_MODS.forEach(MasaConfigCatalog::indexMod);
        indexMod("masa_gadget_mod");
        indexMod("tweakermore");
        scanMagicLib(null);
        indexMod("lpctools");
        MasaGadgetCompat.pages().forEach(MasaConfigCatalog::probePage);
    }

    public static void indexPage(GuiConfigsBase screen)
    {
        String mod = screen.getModId();
        if (mod == null || mod.isBlank() || mod.equals(MasaFavoritesScreen.SCREEN_ID)) return;
        indexMod(mod);
        String packageName = screen.getClass().getPackageName();
        int gui = packageName.indexOf(".gui");
        if (gui >= 0) indexPackage(mod, packageName.substring(0, gui));
    }

    private static void indexPackage(String mod, String packageName)
    {
        if (INDEXED_PACKAGES.add(mod + ":" + packageName))
            for (String name : CONTAINERS) scanOptional(mod, packageName + ".config." + name);
    }

    private static void indexMod(String mod)
    {
        if (mod == null || mod.isBlank() || !INDEXED_MODS.add(mod) || !FabricLoader.getInstance().isModLoaded(mod)) return;
        if (mod.equals("halfmasa"))
        {
            scan(mod, Configs.class);
            for (IConfigBase config : Configs.ALL) observe(mod, config);
        }
        if (mod.equals("malilib")) scanOptional(mod, "fi.dy.masa.malilib.MaLiLibConfigs");
        if (MASA_MODS.contains(mod)) indexPackage(mod, "fi.dy.masa." + mod);
        if (mod.equals("masa_gadget_mod")) scanOptional(mod, "com.plusls.MasaGadget.game.Configs");
        if (mod.equals("tweakermore")) scanOptional(mod, "me.fallenbreath.tweakermore.config.TweakerMoreConfigs");
        if (mod.equals("lpctools"))
        {
            try
            {
                Object page = Class.forName("lpctools.LPCTools").getField("page").get(null);
                scanLpcTree(page, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
            }
            catch (ReflectiveOperationException | LinkageError error)
            { XaeroWorldBinding.LOGGER.warn("Unable to index LPCTools config tree", error); }
        }
    }

    private static void probePage(fi.dy.masa.malilib.util.data.ModInfo page)
    {
        String mod = MasaGadgetCompat.modId(page);
        var supplier = MasaGadgetCompat.supplier(page);
        if (supplier == null || mod.equals(MasaFavoritesScreen.SCREEN_ID) || !PROBED_PAGES.add(mod)) return;
        try
        {
            if (!(supplier.get() instanceof GuiConfigsBase screen)) return;
            indexPage(screen);
            for (var wrapper : screen.getConfigs()) observe(mod, wrapper.getConfig());
        }
        catch (RuntimeException | LinkageError error)
        { XaeroWorldBinding.LOGGER.warn("Unable to index Masa config page {}", mod, error); }
    }

    private static void scanMagicLib(String mod)
    {
        if (mod != null && (discovered || !INDEXED_MAGIC_MODS.add(mod))) return;
        // The public global registry includes every category of all MagicLib config providers.
        try
        {
            Class<?> global = Class.forName("top.hendrixshen.magiclib.impl.malilib.config.GlobalConfigManager");
            Class<?> container = Class.forName("top.hendrixshen.magiclib.impl.malilib.config.ConfigContainer");
            Class<?> manager = Class.forName("top.hendrixshen.magiclib.api.malilib.config.MagicConfigManager");
            Object registry = global.getMethod("getInstance").invoke(null);
            var getManager = container.getMethod("getConfigManager");
            var getConfig = container.getMethod("getConfig");
            var getIdentifier = manager.getMethod("getIdentifier");
            if (global.getMethod("getAllContainers").invoke(registry) instanceof Iterable<?> configs)
                for (Object value : configs)
                {
                    Object owner = getManager.invoke(value);
                    if (owner == null) continue;
                    String id = (String) getIdentifier.invoke(owner);
                    if ((mod == null || mod.equals(id)) && getConfig.invoke(value) instanceof IConfigBase config)
                        observe(id, config);
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
        if (config == null) return null;
        IConfigBase identity = canonical(config);
        Entry known = OBJECTS.get(identity);
        if (known != null) return known;
        indexMod(mod);
        known = OBJECTS.get(identity);
        return known != null ? known : register(mod, "observed/" + identity.getClass().getName() + "/" + config.getName(), config);
    }

    public static Entry lookup(IConfigBase config) { return config == null ? null : OBJECTS.get(canonical(config)); }

    public static Entry find(String key)
    {
        if (key == null) return null;
        Entry known = ENTRIES.get(key);
        int separator = key.indexOf(':');
        if (known != null || separator <= 0 || key.startsWith("screen:")) return known;
        String mod = key.substring(0, separator);
        indexMod(mod);
        if (!ENTRIES.containsKey(key)) scanMagicLib(mod);
        if (!ENTRIES.containsKey(key) && !PROBED_PAGES.contains(mod))
        {
            var page = MasaGadgetCompat.pageForFavorite(mod);
            if (page != null) probePage(page);
        }
        return ENTRIES.get(key);
    }

    static List<Entry> indexedEntries(String mod)
    {
        return ENTRIES.values().stream().filter(entry -> entry.modId().equals(mod)).toList();
    }
    public static List<Entry> entries() { discover(); return new ArrayList<>(ENTRIES.values()); }

    public record Entry(String key, String modId, IConfigBase identity, IConfigBase editable) { }
}
//#endif
