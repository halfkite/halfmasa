package io.github.halfmasa.xaerobinding.gui;

import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class ActionConfigListWidget extends WidgetListConfigOptions
{
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

            int indent = Configs.isExpandedChild(config)
                    ? 28
                    : Configs.getExpansionConfig(config) != null ? 18 : 0;
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
