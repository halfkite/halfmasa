package io.github.halfmasa.xaerobinding.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ScrollCategoryKeyProvider;

public final class ConfigScrollMemory
{
    private static final String MOD_MENU_KEY = "modmenu:list";
    private static final Map<String, Double> POSITIONS = new HashMap<>();

    private ConfigScrollMemory() {}

    public static void clear()
    {
        POSITIONS.clear();
    }

    public static void saveModMenu(double position)
    {
        if (enabled()) POSITIONS.put(MOD_MENU_KEY, Math.max(0.0D, position));
    }

    public static double restoreModMenu()
    {
        return enabled() ? POSITIONS.getOrDefault(MOD_MENU_KEY, 0.0D) : 0.0D;
    }

    public static void save(GuiConfigsBase screen, WidgetListBase<?, ?> widget, String key)
    {
        if (!enabled() || widget == null || key == null || hasFilter(widget)) return;
        POSITIONS.put(key, (double) widget.getScrollbar().getValue());
    }

    public static String restore(GuiConfigsBase screen, WidgetListBase<?, ?> widget)
    {
        String key = key(screen);
        if (!enabled() || widget == null || hasFilter(widget)) return key;
        Double saved = POSITIONS.get(key);
        if (saved != null)
        {
            int maximum = Math.max(0, widget.getScrollbar().getMaxValue());
            widget.getScrollbar().setValue(Math.max(0, Math.min(maximum, saved.intValue())));
        }
        return key;
    }

    private static boolean enabled()
    {
        return Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue();
    }

    private static boolean hasFilter(WidgetListBase<?, ?> widget)
    {
        WidgetSearchBar search = widget.getSearchBarWidget();
        return search != null && search.hasFilter();
    }

    private static String key(GuiConfigsBase screen)
    {
        String category;
        if (screen instanceof ScrollCategoryKeyProvider provider)
        {
            category = provider.halfmasa$getScrollCategoryKey();
        }
        else
        {
            category = fingerprint(screen.getConfigs());
        }
        return screen.getClass().getName() + '|' + screen.getModId() + '|' + category;
    }

    private static String fingerprint(List<ConfigOptionWrapper> configs)
    {
        StringBuilder result = new StringBuilder();
        for (ConfigOptionWrapper wrapper : configs)
        {
            if (wrapper.getConfig() != null)
            {
                result.append("C:").append(wrapper.getConfig().getName());
            }
            else
            {
                result.append("L:").append(wrapper.getLabel());
            }
            result.append('\u0000');
        }
        return result.toString();
    }
}
