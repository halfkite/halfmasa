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
//#if MC >= 26.3
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBarConfigs;
import io.github.halfmasa.xaerobinding.mixin.ConfigSearchBarAccessor;
//#endif

public final class ConfigScrollMemory
{
    private static final String MOD_MENU_KEY = "modmenu:list";
    private static final String CUSTOM_GROUP_EDITOR_PREFIX = "custom-groups:editor:";
    private static final String CUSTOM_GROUP_TARGET_PREFIX = "custom-groups:target:";
    private static final String CUSTOM_GROUP_CHOICE_PREFIX = "custom-groups:choice:";
    private static final Map<String, Double> POSITIONS = new HashMap<>();
    private static String customGroupSource = "halfmasa";
    //#if MC >= 26.3
    private record SearchState(String text, boolean open, List<Integer> keys, int position) {}
    private static final Map<String, SearchState> SEARCHES = new HashMap<>();
    private static String selectedHalfMasaTab = "ALL";

    public static void clearConfigState()
    {
        SEARCHES.clear();
        POSITIONS.keySet().removeIf(key -> key.contains("|"));
    }

    public static void clearSelectedTab() { selectedHalfMasaTab = "ALL"; }

    public static String restoreSelectedTab()
    {
        return enabled() && Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue() ? selectedHalfMasaTab : "ALL";
    }

    public static void saveSelectedTab(String tab)
    {
        if (enabled() && Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue()) selectedHalfMasaTab = tab;
    }
    //#endif

    private ConfigScrollMemory() {}

    public static void clear()
    {
        POSITIONS.clear();
        customGroupSource = "halfmasa";
        //#if MC >= 26.3
        SEARCHES.clear();
        clearSelectedTab();
        //#endif
    }

    public static void saveCustomGroupEditor(String source, int position)
    {
        saveCustomGroupPosition(CUSTOM_GROUP_EDITOR_PREFIX + source, source, position);
    }

    public static int restoreCustomGroupEditor(String source)
    {
        return restoreCustomGroupPosition(CUSTOM_GROUP_EDITOR_PREFIX + source);
    }

    public static void saveCustomGroupTarget(String source, int position)
    {
        saveCustomGroupPosition(CUSTOM_GROUP_TARGET_PREFIX + source, source, position);
    }

    public static int restoreCustomGroupTarget(String source)
    {
        return restoreCustomGroupPosition(CUSTOM_GROUP_TARGET_PREFIX + source);
    }

    public static void saveCustomGroupChoice(String source, int position)
    {
        saveCustomGroupPosition(CUSTOM_GROUP_CHOICE_PREFIX + source, source, position);
    }

    public static int restoreCustomGroupChoice(String source)
    {
        return restoreCustomGroupPosition(CUSTOM_GROUP_CHOICE_PREFIX + source);
    }

    public static String restoreCustomGroupSource()
    {
        return enabled() ? customGroupSource : "halfmasa";
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
        if (!enabled() || widget == null || key == null) return;
        //#if MC >= 26.3
        if (!Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue()) return;
        WidgetSearchBar search = widget.getSearchBarWidget();
        if (search != null)
        {
            String text = ((ConfigSearchBarAccessor) search).halfmasa$getSearchBox().getValueWrapper();
            List<Integer> keys = search instanceof WidgetSearchBarConfigs configs
                    ? List.copyOf(configs.getKeybind().getKeys()) : List.of();
            SEARCHES.put(key, new SearchState(text, search.isSearchOpen(), keys, widget.getScrollbar().getValue()));
        }
        //#endif
        if (hasFilter(widget)) return;
        POSITIONS.put(key, (double) widget.getScrollbar().getValue());
    }

    public static String restore(GuiConfigsBase screen, WidgetListBase<?, ?> widget)
    {
        String key = key(screen);
        if (!enabled() || widget == null) return key;
        //#if MC >= 26.3
        if (!Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue()) return key;
        SearchState state = SEARCHES.get(key);
        WidgetSearchBar search = widget.getSearchBarWidget();
        if (state != null && search != null)
        {
            var textField = ((ConfigSearchBarAccessor) search).halfmasa$getSearchBox();
            // Restore filters first, then clamp against the rebuilt result list.
            textField.setValueWrapper(state.text());
            search.setSearchOpen(state.open());
            textField.setFocusedWrapper(false);
            if (search instanceof WidgetSearchBarConfigs configs)
            {
                configs.getKeybind().clearKeys();
                state.keys().forEach(configs.getKeybind()::addKey);
            }
            widget.refreshEntries();
            int maximum = Math.max(0, widget.getScrollbar().getMaxValue());
            widget.getScrollbar().setValue(Math.max(0, Math.min(maximum, state.position())));
            return key;
        }
        //#endif
        if (hasFilter(widget)) return key;
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

    private static void saveCustomGroupPosition(String key, String source, int position)
    {
        if (!enabled())
        {
            return;
        }
        customGroupSource = source;
        POSITIONS.put(key, (double) Math.max(0, position));
    }

    private static int restoreCustomGroupPosition(String key)
    {
        if (!enabled())
        {
            return 0;
        }
        return Math.max(0, POSITIONS.getOrDefault(key, 0.0D).intValue());
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
