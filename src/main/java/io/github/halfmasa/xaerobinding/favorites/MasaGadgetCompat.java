//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.mixin.MasaFavoritesGuiWidgetsAccessor;

/** Optional adapter: never changes Gadget's saved settings or requires MagicLib at runtime. */
public final class MasaGadgetCompat
{
    public static String modId(ModInfo mod) {
        //#if MC >= 1.21.11
        return mod.modId();
        //#else
        //$$ return mod.getModId();
        //#endif
    }
    public static String modName(ModInfo mod) {
        //#if MC >= 1.21.11
        return mod.modName();
        //#else
        //$$ return mod.getModName();
        //#endif
    }
    public static java.util.function.Supplier<GuiBase> supplier(ModInfo mod) {
        //#if MC >= 1.21.11
        return mod.configScreenSupplier();
        //#else
        //$$ return mod.getConfigScreenSupplier();
        //#endif
    }

    private static List<ModInfo> gadgetPages;
    private static final Map<String, ModInfo> PROVISIONAL_PAGES = new LinkedHashMap<>();
    private MasaGadgetCompat() { }
    public static boolean loaded() { return FabricLoader.getInstance().isModLoaded("masa_gadget_mod"); }

    public static List<ModInfo> pages()
    {
        if (needsPageDiscovery()) gadgetPages = discoverModMenuPages(null);
        return knownPages();
    }

    public static boolean needsPageDiscovery()
    {
        return gadgetPages == null && FabricLoader.getInstance().isModLoaded("modmenu") &&
                (loaded() || FabricLoader.getInstance().isModLoaded("magiclib") ||
                        FabricLoader.getInstance().isModLoaded("magiclib-malilib-extra"));
    }

    public static ModInfo registeredPage(String id)
    {
        for (var page : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens())
            if (modId(page).equals(id)) return page;
        return null;
    }

    public static ModInfo registerCurrentPage(GuiConfigsBase screen)
    {
        String id = screen.getModId();
        ModInfo page = registeredPage(id);
        if (page != null) return page;
        page = findCachedModMenuPage(id);
        if (page != null) return page;
        String name = FabricLoader.getInstance().getModContainer(id)
                .map(owner -> owner.getMetadata().getName()).orElseGet(() -> fi.dy.masa.malilib.util.StringUtils.splitCamelCase(id));
        page = new ModInfo(id, name, () -> screen);
        PROVISIONAL_PAGES.put(id, page);
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(page);
        return page;
    }

    private static ModInfo findCachedModMenuPage(String id)
    {
        if (gadgetPages != null)
            for (var page : gadgetPages)
                if (modId(page).equals(id)) return page;
        return null;
    }

    static ModInfo pageForFavorite(String id)
    {
        ModInfo nativePage = registeredPage(id);
        if (nativePage != null && nativePage != PROVISIONAL_PAGES.get(id)) return nativePage;
        var optional = findCachedModMenuPage(id);
        if (optional == null && needsPageDiscovery() && FabricLoader.getInstance().isModLoaded(id))
            optional = discoverModMenuPages(id).stream().findFirst().orElse(null);
        return optional != null ? optional : nativePage;
    }

    public static List<ModInfo> knownPages()
    {
        Map<String, ModInfo> pages = new LinkedHashMap<>();
        Registry.CONFIG_SCREEN.getAllModsWithConfigScreens().forEach(mod -> pages.put(modId(mod), mod));
        if (gadgetPages != null)
        {
            for (var page : gadgetPages)
            {
                String id = modId(page);
                if (pages.get(id) == PROVISIONAL_PAGES.get(id)) pages.put(id, page);
                else pages.putIfAbsent(id, page);
            }
        }
        return new ArrayList<>(pages.values());
    }

    private static boolean belongsTo(ModContainer provider, String mod)
    {
        while (provider != null)
        {
            if (mod.equals(provider.getMetadata().getId())) return true;
            provider = provider.getContainingMod().orElse(null);
        }
        return false;
    }

    private static List<ModInfo> discoverModMenuPages(String target)
    {
        List<ModInfo> result = new ArrayList<>();
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return result;
        try
        {
            Class<?> api = Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
            var getFactory = api.getMethod("getModConfigScreenFactory");
            var create = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory").getMethod("create", Screen.class);
            for (var entry : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class))
            {
                if (target != null && !belongsTo(entry.getProvider(), target)) continue;
                String mod = entry.getProvider().getMetadata().getId();
                try
                {
                    Object instance = entry.getEntrypoint();
                    if (!api.isInstance(instance)) continue;
                    Object factory = getFactory.invoke(instance);
                    if (factory == null || !(create.invoke(factory, new Object[] {null}) instanceof GuiConfigsBase screen)) continue;
                    // Nested Mod Menu entrypoint providers may not own the actual config page.
                    String screenId = screen.getModId();
                    if (target != null && !target.equals(screenId)) continue;
                    String screenName = FabricLoader.getInstance().getModContainer(screenId)
                            .map(owner -> owner.getMetadata().getName()).orElse(entry.getProvider().getMetadata().getName());
                    result.add(new ModInfo(screenId, screenName, () -> {
                        try { return (GuiBase) create.invoke(factory, io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(Minecraft.getInstance())); }
                        catch (ReflectiveOperationException error) { throw new IllegalStateException("Unable to open Masa page " + mod, error); }
                    }));
                }
                catch (ReflectiveOperationException | RuntimeException | LinkageError error)
                { XaeroWorldBinding.LOGGER.warn("Unable to discover Mod Menu Masa page {}", mod, error); }
            }
        }
        catch (ReflectiveOperationException error)
        { XaeroWorldBinding.LOGGER.warn("Masa Gadget Mod Menu adapter unavailable", error); }
        return List.copyOf(result);
    }

    private static Field switcherField()
    {
        if (!loaded()) return null;
        for (Field field : GuiConfigsBase.class.getDeclaredFields())
            if (field.getName().endsWith("$masaModGuiList") && WidgetBase.class.isAssignableFrom(field.getType()))
            { field.setAccessible(true); return field; }
        return null; // Gadget's switcher mixin is conditional on Mod Menu being installed.
    }

    public static WidgetBase detach(GuiConfigsBase screen)
    {
        Field field = switcherField();
        if (field == null) return null;
        try
        {
            WidgetBase widget = (WidgetBase) field.get(screen);
            if (widget != null)
            {
                ((MasaFavoritesGuiWidgetsAccessor) screen).halfmasa$getWidgets().remove(widget);
                field.set(screen, null); // Also stops Gadget's late redraw/hover pass.
            }
            return widget;
        }
        catch (IllegalAccessException error) { throw new IllegalStateException("Unable to detach Gadget switcher", error); }
    }

    public static void restore(GuiConfigsBase screen)
    {
        if (switcherField() == null) return;
        detach(screen);
        try { screen.getClass().getMethod("masa_gadget_mod$addFastSwitcherWidget").invoke(screen); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Unable to restore Gadget switcher", error); }
    }
}
//#endif
