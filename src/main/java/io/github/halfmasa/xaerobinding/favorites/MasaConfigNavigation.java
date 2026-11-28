//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBarConfigs;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.gui.ActionConfigListWidget;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.mixin.ConfigSearchBarAccessor;
import io.github.halfmasa.xaerobinding.mixin.GuiListBaseAccessor;
import net.minecraft.client.Minecraft;

public final class MasaConfigNavigation
{
    private static final long HIGHLIGHT_NANOS = java.time.Duration.ofSeconds(5).toNanos();
    private static Highlight highlight;

    private record Highlight(java.lang.ref.WeakReference<GuiConfigsBase> screen,
            fi.dy.masa.malilib.config.IConfigBase config, long expiresAt) {}

    public static boolean isHighlighted(Object screen, fi.dy.masa.malilib.config.IConfigBase config)
    {
        Highlight current = highlight;
        if (current == null) return false;
        if (System.nanoTime() - current.expiresAt() >= 0 || current.screen().get() == null)
        {
            highlight = null;
            return false;
        }
        return current.screen().get() == screen && MasaConfigCatalog.canonical(config) == current.config();
    }

    private MasaConfigNavigation() {}

    static void open(MasaFavoritesScreen favorites, String mod, MasaConfigCatalog.Entry entry)
    {
        highlight = null;
        var page = MasaGadgetCompat.pageForFavorite(mod);
        var factory = page == null ? null : MasaGadgetCompat.supplier(page);
        try
        {
            if (factory != null && factory.get() instanceof GuiConfigsBase screen && screen != favorites)
            {
                if (entry != null) selectTab(screen, entry);
                screen.setParent(favorites);
                MinecraftClientCompat.setScreen(Minecraft.getInstance(), screen);
                if (entry != null && !reveal(screen, entry))
                    screen.addMessage(MessageType.WARNING, "halfmasa.gui.favorites.option_unavailable",
                            entry.identity().getConfigGuiDisplayName());
                return;
            }
        }
        catch (RuntimeException | LinkageError error)
        {
            XaeroWorldBinding.LOGGER.warn("Unable to open Masa config page {}", mod, error);
        }
        favorites.addMessage(MessageType.WARNING, "halfmasa.gui.favorites.source_unavailable", MasaConfigCatalog.sourceName(mod));
    }

    private static void selectTab(GuiConfigsBase screen, MasaConfigCatalog.Entry entry)
    {
        if (screen instanceof HalfMasaConfigScreen halfmasa)
        {
            halfmasa.showAllForNavigation();
            return;
        }
        String className = screen.getClass().getName();
        try
        {
            if (className.equals("lpctools.lpcfymasaapi.LPCConfigPage$ConfigPageInstance"))
            {
                Object page = Class.forName("lpctools.LPCTools").getField("page").get(null);
                var selected = page.getClass().getDeclaredField("selectedIndex");
                selected.setAccessible(true);
                var tabs = (java.util.Collection<?>) page.getClass().getMethod("getConfigs").invoke(page);
                var indices = java.util.stream.IntStream.range(0, tabs.size()).boxed().toList();
                chooseTab(screen, entry, selected.get(page), indices, index -> selected.set(page, index));
                return;
            }
            for (Class<?> type = screen.getClass(); type != GuiConfigsBase.class; type = type.getSuperclass())
            {
                if (type.getName().equals("top.hendrixshen.magiclib.impl.malilib.config.gui.MagicConfigGui"))
                {
                    selectMagicCategory(screen, entry, type);
                    return;
                }
            }
            if (!className.equals("fi.dy.masa." + entry.modId() + ".gui.GuiConfigs")) return;
            if (entry.modId().equals("litematica"))
            {
                Class<?> manager = Class.forName("fi.dy.masa.litematica.data.DataManager");
                var get = manager.getMethod("getConfigGuiTab");
                var set = manager.getMethod("setConfigGuiTab", get.getReturnType());
                var tabs = get.getReturnType().getEnumConstants();
                chooseTab(screen, entry, get.invoke(null), Arrays.asList(tabs), tab -> set.invoke(null, tab));
            }
            else
            {
                var field = screen.getClass().getDeclaredField("tab");
                if (!field.getType().isEnum()) return;
                field.setAccessible(true);
                chooseTab(screen, entry, field.get(screen), Arrays.asList(field.getType().getEnumConstants()),
                        tab -> field.set(screen, tab));
            }
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError error)
        {
            XaeroWorldBinding.LOGGER.debug("Unable to select a config tab in {}", className, error);
        }
    }

    private static void selectMagicCategory(GuiConfigsBase screen, MasaConfigCatalog.Entry entry, Class<?> type)
            throws ReflectiveOperationException
    {
        var field = type.getDeclaredField("configManager");
        field.setAccessible(true);
        Object manager = field.get(screen);
        Object settings = field.getType().getMethod("getGuiSetting").invoke(manager);
        var categories = (Iterable<?>) field.getType().getMethod("getCategories").invoke(manager);
        var get = settings.getClass().getMethod("getCategory");
        var set = settings.getClass().getMethod("setCategory", String.class);
        chooseTab(screen, entry, get.invoke(settings), categories, category -> set.invoke(settings, category));
    }

    private static void chooseTab(GuiConfigsBase screen, MasaConfigCatalog.Entry entry,
            Object original, Iterable<?> tabs, TabSetter select) throws ReflectiveOperationException
    {
        Object destination = original;
        try
        {
            for (Object tab : tabs)
            {
                if (tab instanceof Enum<?> value && value.name().equals("ALL")) continue;
                select.set(tab);
                if (screen.getConfigs().stream().anyMatch(row -> MasaConfigCatalog.matches(row.getConfig(), entry)))
                {
                    destination = tab;
                    break;
                }
            }
        }
        finally
        {
            select.set(destination);
        }
    }

    private interface TabSetter
    {
        void set(Object value) throws ReflectiveOperationException;
    }

    private static boolean reveal(GuiConfigsBase screen, MasaConfigCatalog.Entry entry)
    {
        if (!(((GuiListBaseAccessor) (Object) screen).halfmasa$getListWidget() instanceof WidgetListConfigOptions list))
            return false;
        int index = findRow(list, entry);
        if (index < 0 && list.getSearchBarWidget() != null)
        {
            var search = list.getSearchBarWidget();
            var box = ((ConfigSearchBarAccessor) search).halfmasa$getSearchBox();
            //#if MC > 1.21.10
            String previous = box.getValueWrapper();
            Consumer<String> setText = box::setValueWrapper;
            //#else
            //$$ String previous = box.getTextWrapper();
            //$$ Consumer<String> setText = box::setTextWrapper;
            //#endif
            boolean wasOpen = search.isSearchOpen();
            List<Integer> keys = search instanceof WidgetSearchBarConfigs configSearch
                    ? new ArrayList<>(configSearch.getKeybind().getKeys()) : List.of();
            if (search instanceof WidgetSearchBarConfigs configSearch) configSearch.getKeybind().clearKeys();
            search.setSearchOpen(true);
            setText.accept(entry.identity().getName());
            box.setFocusedWrapper(false);
            list.refreshEntries();
            index = findRow(list, entry);
            if (index < 0)
            {
                setText.accept(previous);
                search.setSearchOpen(wasOpen);
                if (search instanceof WidgetSearchBarConfigs configSearch) keys.forEach(configSearch.getKeybind()::addKey);
                list.refreshEntries();
            }
        }
        if (index < 0) return false;
        highlight = new Highlight(new java.lang.ref.WeakReference<>(screen),
                MasaConfigCatalog.canonical(list.getCurrentEntries().get(index).getConfig()),
                System.nanoTime() + HIGHLIGHT_NANOS);
        list.getScrollbar().setValue(Math.min(index, list.getScrollbar().getMaxValue()));
        return true;
    }

    private static int findRow(WidgetListConfigOptions list, MasaConfigCatalog.Entry entry)
    {
        var rows = list.getCurrentEntries();
        for (int index = 0; index < rows.size(); index++)
        {
            var config = rows.get(index).getConfig();
            if (MasaConfigCatalog.matches(config, entry) || config != null && list instanceof ActionConfigListWidget actionList &&
                    MasaConfigCatalog.matches(actionList.getInlineConfig(config), entry)) return index;
        }
        return -1;
    }
}
//#endif
