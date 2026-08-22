package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class ActionConfigListWidget extends WidgetListConfigOptions
{
    @Override
    protected List<String> getEntryStringsForFilter(ConfigOptionWrapper entry)
    {
        List<String> strings = new ArrayList<>(super.getEntryStringsForFilter(entry));
        IConfigBase config = entry.getConfig();
        if (config == null)
        {
            return strings;
        }

        IConfigBase parent = Configs.getExpansionParent(config);
        if (parent != null)
        {
            addSearchStrings(strings, parent);
        }
        for (IConfigBase child : Configs.getExpansionChildren(config))
        {
            addSearchStrings(strings, child);
        }
        return strings;
    }

    private static void addSearchStrings(List<String> strings, IConfigBase config)
    {
        String name = config.getName().toLowerCase();
        String translated = config.getConfigGuiDisplayName().toLowerCase();
        if (!strings.contains(name))
        {
            strings.add(name);
        }
        if (!name.equals(translated) && !strings.contains(translated))
        {
            strings.add(translated);
        }
    }

    @Override
    public int getMaxNameLengthWrapped(java.util.List<ConfigOptionWrapper> wrappers)
    {
        int width = 0;

        for (ConfigOptionWrapper wrapper : wrappers)
        {
            fi.dy.masa.malilib.config.IConfigBase config = wrapper.getConfig();
            if (config == null)
            {
                continue;
            }

            int indent = 18 + (Configs.isExpandedChild(config) ? 28 : 0);
            width = Math.max(width,
                    this.getStringWidth(config.getConfigGuiDisplayName()) + indent + 16);
        }

        return width;
    }

    public ActionConfigListWidget(
            int x,
            int y,
            int width,
            int height,
            int configWidth,
            float zLevel,
            boolean useKeybindSearch,
            HalfMasaConfigScreen parent)
    {
        super(x, y, width, height, configWidth, zLevel, useKeybindSearch, parent);
    }

    @Override
    protected WidgetConfigOption createListEntryWidget(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            ConfigOptionWrapper entry)
    {
        return new ActionConfigOptionWidget(
                x,
                y,
                this.browserEntryWidth,
                22,
                this.maxLabelWidth,
                this.configWidth,
                entry,
                listIndex,
                this.parent,
                this);
    }
}
