//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ConfigExpansionProvider;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigExpansionProvider;
import io.github.halfmasa.xaerobinding.gui.TweakerooConfigExpansionProvider;

/** A visit owns its collapsed state; child rows reference the same original configs. */
public final class MasaFavoritesExpansionProvider implements ConfigExpansionProvider
{
    private final List<MasaConfigCatalog.Entry> roots;
    private final MasaFavoritesSession session;
    private final Map<String, ConfigExpansionProvider> sources = new LinkedHashMap<>();
    private List<MasaConfigCatalog.Entry> searchRoots;
    private final Map<IConfigBase, Integer> displayOrder = new IdentityHashMap<>();
    private String query;
    private Set<String> queryFavorites = Set.of();
    private boolean globalSearch;

    public void setSearchQuery(String query)
    {
        boolean enabled = Configs.MASA_FAVORITES_GLOBAL_SEARCH.getBooleanValue();
        if (!java.util.Objects.equals(this.query, query) || this.globalSearch != enabled)
        {
            this.query = query;
            this.globalSearch = enabled;
            this.queryFavorites = Set.copyOf(MasaFavoritesStore.get().snapshot());
        }
    }

    public java.util.Comparator<ConfigOptionWrapper> comparator()
    {
        return java.util.Comparator.comparingInt(entry -> this.displayOrder.getOrDefault(entry.getConfig(), Integer.MAX_VALUE));
    }
    private final Map<IConfigBase, List<IConfigBase>> children = new IdentityHashMap<>();
    private final Map<IConfigBase, IConfigBase> parents = new IdentityHashMap<>();
    private final Map<IConfigBase, ConfigBoolean> expansion = new IdentityHashMap<>();

    public MasaFavoritesExpansionProvider(MasaFavoritesSession session)
    {
        this.session = session;
        this.indexBranches(session.snapshot());
        // A separately favorited child is represented inside its favorited parent's subtree.
        Set<IConfigBase> favorites = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        session.snapshot().forEach(entry -> favorites.add(entry.editable()));
        this.roots = session.snapshot().stream().filter(entry -> !this.hasFavoriteAncestor(entry.editable(), favorites)).toList();
    }

    private void indexBranches(List<MasaConfigCatalog.Entry> entries)
    {
        if (entries.stream().anyMatch(entry -> entry.modId().equals("halfmasa")))
            this.sources.computeIfAbsent("halfmasa", ignored -> halfMasaSource());
        if (entries.stream().anyMatch(entry -> entry.modId().equals("tweakeroo")))
            this.sources.computeIfAbsent("tweakeroo", ignored -> tweakerooSource());
        for (var entry : entries)
        {
            var source = this.sources.get(entry.modId());
            if (source != null) this.addBranch(entry, source, new HashSet<>());
        }
    }

    private static ConfigExpansionProvider halfMasaSource()
    {
        HalfMasaConfigExpansionProvider half = new HalfMasaConfigExpansionProvider("halfmasa");
        half.prepareEntries(ConfigOptionWrapper.createFor(Configs.ALL), true);
        return new ConfigExpansionProvider()
        {
            @Override public List<IConfigBase> getExpansionChildren(IConfigBase config)
            {
                return java.util.stream.Stream.concat(Configs.getExpansionChildren(config).stream(),
                        half.getExpansionChildren(config).stream()).distinct().toList();
            }
        };
    }

    private static ConfigExpansionProvider tweakerooSource()
    {
        TweakerooConfigExpansionProvider tweakeroo = new TweakerooConfigExpansionProvider();
        tweakeroo.prepareEntries(MasaConfigCatalog.indexedEntries("tweakeroo").stream()
                .map(entry -> new ConfigOptionWrapper(entry.editable())).toList(), true);
        return tweakeroo;
    }

    private void addBranch(MasaConfigCatalog.Entry entry, ConfigExpansionProvider source, Set<String> path)
    {
        if (!path.add(entry.key())) return;
        IConfigBase original = entry.modId().equals("halfmasa") ? entry.identity() : entry.editable();
        List<IConfigBase> branch = new ArrayList<>();
        for (IConfigBase child : source.getExpansionChildren(original))
        {
            var resolved = MasaConfigCatalog.observe(entry.modId(), child);
            if (resolved == null || path.contains(resolved.key())) continue;
            branch.add(resolved.editable());
            this.parents.putIfAbsent(resolved.editable(), entry.editable());
            this.addBranch(resolved, source, path);
        }
        if (!branch.isEmpty())
        {
            this.children.put(entry.editable(), List.copyOf(branch));
            this.expansion.putIfAbsent(entry.editable(), new ConfigBoolean("favoriteExpanded." + entry.key(), false));
        }
        path.remove(entry.key());
    }

    private boolean hasFavoriteAncestor(IConfigBase config, Set<IConfigBase> favorites)
    {
        Set<IConfigBase> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        while ((config = this.parents.get(config)) != null && seen.add(config))
            if (favorites.contains(config)) return true;
        return false;
    }

    @Override
    public Collection<ConfigOptionWrapper> prepareEntries(Collection<ConfigOptionWrapper> entries, boolean searchActive)
    {
        if (!Configs.MASA_FAVORITES.getBooleanValue()) return List.of();
        List<ConfigOptionWrapper> visible = new ArrayList<>();
        Set<IConfigBase> emitted = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<MasaConfigCatalog.Entry> active = this.roots;
        if (searchActive && this.globalSearch)
        {
            if (this.searchRoots == null)
            {
                this.indexBranches(MasaConfigCatalog.entries());
                this.searchRoots = MasaConfigCatalog.entries().stream().filter(entry -> !this.parents.containsKey(entry.editable())).toList();
            }
            List<MasaConfigCatalog.Entry> pinned = new ArrayList<>();
            List<MasaConfigCatalog.Entry> remaining = new ArrayList<>();
            for (var entry : this.searchRoots)
                (this.branchFavorite(entry.editable(), new HashSet<>()) ? pinned : remaining).add(entry);
            pinned.addAll(remaining);
            active = pinned;
        }
        for (var root : active) this.append(root.editable(), visible, emitted);
        this.displayOrder.clear();
        for (int index = 0; index < visible.size(); index++) this.displayOrder.put(visible.get(index).getConfig(), index);
        return visible;
    }

    private boolean branchFavorite(IConfigBase config, Set<IConfigBase> visited)
    {
        if (!visited.add(config)) return false;
        var entry = MasaConfigCatalog.lookup(config);
        return entry != null && this.queryFavorites.contains(entry.key()) ||
                this.getExpansionChildren(config).stream().anyMatch(child -> this.branchFavorite(child, visited));
    }

    private void append(IConfigBase config, List<ConfigOptionWrapper> visible, Set<IConfigBase> emitted)
    {
        if (!emitted.add(config)) return;
        var entry = MasaConfigCatalog.lookup(config);
        if (entry != null) this.session.track(entry);
        visible.add(new ConfigOptionWrapper(config));
        ConfigBoolean expanded = this.expansion.get(config);
        if (expanded != null && expanded.getBooleanValue())
            for (IConfigBase child : this.getExpansionChildren(config)) this.append(child, visible, emitted);
    }

    @Override public IConfigBase getExpansionParent(IConfigBase config) { return this.parents.get(config); }
    @Override public List<IConfigBase> getExpansionChildren(IConfigBase config) { return this.children.getOrDefault(config, List.of()); }
    @Override public ConfigBoolean getExpansionConfig(IConfigBase config) { return this.expansion.get(config); }
}
//#endif
