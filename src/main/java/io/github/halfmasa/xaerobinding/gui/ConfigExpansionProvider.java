package io.github.halfmasa.xaerobinding.gui;

import java.util.Collection;
import java.util.List;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;

import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;

/** Supplies the optional grouping rules used by a MaLiLib config list. */
public interface ConfigExpansionProvider
{
    default String getConfigSource()
    {
        return CustomConfigGroupStore.HALF_MASA_SOURCE;
    }

    default Collection<ConfigOptionWrapper> prepareEntries(
            Collection<ConfigOptionWrapper> entries,
            boolean searchActive)
    {
        return entries;
    }

    default IConfigBase getExpansionParent(IConfigBase config)
    {
        return null;
    }

    default List<IConfigBase> getExpansionChildren(IConfigBase config)
    {
        return List.of();
    }

    default ConfigBoolean getExpansionConfig(IConfigBase config)
    {
        return null;
    }

    default boolean isExpandedChild(IConfigBase config)
    {
        return this.getExpansionParent(config) != null;
    }

    default IConfigBase getInlineCompanion(IConfigBase config)
    {
        return null;
    }

    default boolean isInlineCompanion(IConfigBase config)
    {
        return false;
    }

    default void onExpansionChanged(IConfigBase config, boolean expanded)
    {
    }
}
