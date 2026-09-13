package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;
import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;

/** Adapter for the existing halfmasa-specific expansion rules. */
public final class HalfMasaConfigExpansionProvider implements ConfigExpansionProvider
{
    public static final HalfMasaConfigExpansionProvider INSTANCE =
            new HalfMasaConfigExpansionProvider(CustomConfigGroupStore.HALF_MASA_SOURCE);
    private final String source;
    private final CustomConfigGroupStore customGroups = CustomConfigGroupStore.getInstance();
    private final Map<IConfigBase, IConfigBase> customParents = new IdentityHashMap<>();
    private final Map<IConfigBase, List<IConfigBase>> customChildren = new IdentityHashMap<>();
    private final Map<IConfigBase, String> customIds = new IdentityHashMap<>();
    private final Map<IConfigBase, ConfigBoolean> customExpansionConfigs = new IdentityHashMap<>();
    private final Map<IConfigBase, ConfigOptionWrapper> customPrimaryWrappers = new IdentityHashMap<>();
    private final Map<IConfigBase, List<ConfigOptionWrapper>> customGroupedChildren = new IdentityHashMap<>();
    private final Map<IConfigBase, IConfigBase> customPrimaryByConfig = new IdentityHashMap<>();

    public HalfMasaConfigExpansionProvider(String source)
    {
        this.source = source;
    }

    public static List<CustomConfigGroupStore.GroupTemplate> getBuiltInGroupTemplates()
    {
        List<CustomConfigGroupStore.GroupTemplate> templates = new ArrayList<>();
        for (IConfigBase parent : Configs.getBuiltInExpansionParents())
        {
            List<IConfigBase> children = Configs.getExpansionChildren(parent);
            if (children.isEmpty())
            {
                continue;
            }
            IConfigBase main = parent instanceof ConfigGroupHeader ? children.get(0) : parent;
            List<String> childNames = new ArrayList<>();
            if (!(parent instanceof ConfigGroupHeader) && parent != main)
            {
                childNames.add(parent.getName());
            }
            for (IConfigBase child : children)
            {
                if (child != main && !childNames.contains(child.getName()))
                {
                    childNames.add(child.getName());
                }
            }
            if (!childNames.isEmpty())
            {
                templates.add(new CustomConfigGroupStore.GroupTemplate(
                        "builtin_halfmasa_" + parent.getName(),
                        parent.getConfigGuiDisplayName(),
                        main.getName(),
                        List.copyOf(childNames)));
            }
        }
        return List.copyOf(templates);
    }

    @Override
    public String getConfigSource()
    {
        return this.source;
    }

    @Override
    public Collection<ConfigOptionWrapper> prepareEntries(
            Collection<ConfigOptionWrapper> entries,
            boolean searchActive)
    {
        this.customParents.clear();
        this.customChildren.clear();
        this.customIds.clear();
        this.customExpansionConfigs.clear();
        this.customPrimaryWrappers.clear();
        this.customGroupedChildren.clear();
        this.customPrimaryByConfig.clear();

        List<ConfigOptionWrapper> source = List.copyOf(entries);
        if (!Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return source;
        }
        if (CustomConfigGroupStore.HALF_MASA_SOURCE.equals(this.source))
        {
            this.customGroups.ensureDefaults(this.source, getBuiltInGroupTemplates());
        }
        Map<String, ConfigOptionWrapper> wrappersByName = new LinkedHashMap<>();
        for (ConfigOptionWrapper wrapper : source)
        {
            IConfigBase config = wrapper.getConfig();
            if (config != null)
            {
                wrappersByName.putIfAbsent(config.getName(), wrapper);
            }
        }

        for (CustomConfigGroupStore.Group group : this.customGroups.getGroups())
        {
            if (!this.source.equals(group.source))
            {
                continue;
            }
            ConfigOptionWrapper main = wrappersByName.get(group.mainConfig);
            if (!isGroupable(main))
            {
                continue;
            }

            List<ConfigOptionWrapper> children = new ArrayList<>();
            children.add(main);
            for (String childName : group.children)
            {
                if (!this.source.equals(CustomConfigGroupStore.sourceOf(childName)))
                {
                    continue;
                }
                ConfigOptionWrapper child = wrappersByName.get(CustomConfigGroupStore.nameOf(childName));
                if (isGroupable(child) && !children.contains(child))
                {
                    children.add(child);
                }
            }
            if (children.size() < 2)
            {
                continue;
            }

            ConfigBoolean expanded = new ConfigBoolean(
                    "customConfigGroup." + group.id,
                    group.expanded)
                    .apply("halfmasa.config.custom_groups");
            IConfigBase primaryConfig = main.getConfig();
            List<ConfigOptionWrapper> rest = children.stream()
                    .filter(wrapper -> wrapper != main)
                    .toList();
            this.customIds.put(primaryConfig, group.id);
            this.customExpansionConfigs.put(primaryConfig, expanded);
            this.customPrimaryWrappers.put(primaryConfig, main);
            this.customGroupedChildren.put(primaryConfig, rest);
            this.customChildren.put(primaryConfig, rest.stream()
                    .map(ConfigOptionWrapper::getConfig)
                    .toList());
            for (ConfigOptionWrapper wrapper : children)
            {
                IConfigBase config = wrapper.getConfig();
                if (config != null)
                {
                    this.customPrimaryByConfig.put(config, primaryConfig);
                    if (config != primaryConfig)
                    {
                        this.customParents.put(config, primaryConfig);
                    }
                }
            }
        }

        List<ConfigOptionWrapper> result = new ArrayList<>(source.size());
        Set<IConfigBase> emitted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ConfigOptionWrapper wrapper : source)
        {
            IConfigBase primaryConfig = wrapper.getConfig() == null
                    ? null : this.customPrimaryByConfig.get(wrapper.getConfig());
            if (primaryConfig == null)
            {
                result.add(wrapper);
                continue;
            }
            if (emitted.add(primaryConfig))
            {
                ConfigOptionWrapper primary = this.customPrimaryWrappers.get(primaryConfig);
                if (primary != null)
                {
                    result.add(primary);
                }
                ConfigBoolean expansion = this.customExpansionConfigs.get(primaryConfig);
                if (expansion != null && (expansion.getBooleanValue() || searchActive))
                {
                    result.addAll(this.customGroupedChildren.getOrDefault(primaryConfig, List.of()));
                }
            }
        }
        return result;
    }

    @Override
    public IConfigBase getExpansionParent(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customParents.get(config);
        }
        return CustomConfigGroupStore.HALF_MASA_SOURCE.equals(this.source)
                ? Configs.getExpansionParent(config) : null;
    }

    @Override
    public List<IConfigBase> getExpansionChildren(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customChildren.getOrDefault(config, List.of());
        }
        return Configs.getExpansionChildren(config);
    }

    @Override
    public ConfigBoolean getExpansionConfig(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customExpansionConfigs.get(config);
        }
        return Configs.getExpansionConfig(config);
    }

    @Override
    public void onExpansionChanged(IConfigBase config, boolean expanded)
    {
        String groupId = this.customIds.get(config);
        if (groupId != null)
        {
            this.customGroups.setExpanded(groupId, expanded);
        }
    }

    private static boolean isGroupable(ConfigOptionWrapper wrapper)
    {
        if (wrapper == null || wrapper.getConfig() instanceof ConfigGroupHeader)
        {
            return false;
        }
        IConfigBase config = wrapper.getConfig();
        return config != null && !config.getName().endsWith("Expanded");
    }
}
