//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ConfigExpansionProvider;

/** Stable partition by root config: a parent and its children always move together. */
public final class MasaFavoritesOrder
{
    private MasaFavoritesOrder() { }

    public static List<ConfigOptionWrapper> sort(Collection<ConfigOptionWrapper> entries, String mod,
            ConfigExpansionProvider provider)
    {
        if (!Configs.MASA_FAVORITES.getBooleanValue() || !Configs.MASA_FAVORITES_PIN.getBooleanValue()) return List.copyOf(entries);
        List<ConfigOptionWrapper> result = new ArrayList<>();
        Map<IConfigBase, List<ConfigOptionWrapper>> blocks = new LinkedHashMap<>();
        for (var entry : entries)
        {
            if (entry.getConfig() == null)
            {
                flush(result, blocks, mod, provider);
                result.add(entry);
                continue;
            }
            IConfigBase root = entry.getConfig();
            Set<IConfigBase> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            while (provider != null && seen.add(root))
            {
                IConfigBase parent = provider.getExpansionParent(root);
                if (parent == null) break;
                root = parent;
            }
            blocks.computeIfAbsent(root, ignored -> new ArrayList<>()).add(entry);
        }
        flush(result, blocks, mod, provider);
        return result;
    }

    private static void flush(List<ConfigOptionWrapper> result, Map<IConfigBase, List<ConfigOptionWrapper>> blocks,
            String mod, ConfigExpansionProvider provider)
    {
        for (boolean pinned : new boolean[] {true, false})
            for (var block : blocks.entrySet())
            {
                boolean favorite = isFavorite(mod, block.getKey());
                if (provider != null)
                    favorite |= provider.getExpansionChildren(block.getKey()).stream().anyMatch(config -> isFavorite(mod, config));
                favorite |= block.getValue().stream().anyMatch(entry -> isFavorite(mod, entry.getConfig()));
                if (favorite == pinned) result.addAll(block.getValue());
            }
        blocks.clear();
    }

    private static boolean isFavorite(String mod, IConfigBase config)
    {
        var entry = MasaConfigCatalog.observe(mod, config);
        return entry != null && MasaFavoritesStore.get().contains(entry.key());
    }
}
//#endif
