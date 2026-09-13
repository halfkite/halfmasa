package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Stores user-created configuration groups outside the normal option values. */
public final class CustomConfigGroupStore
{
    public static final String HALF_MASA_SOURCE = "halfmasa";
    public static final String TWEAKEROO_SOURCE = "tweakeroo";
    private static final String REF_SEPARATOR = "::";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final CustomConfigGroupStore INSTANCE = new CustomConfigGroupStore();
    private final List<Group> groups = new ArrayList<>();
    private final Set<String> initializedSources = new HashSet<>();
    private boolean loaded;

    private CustomConfigGroupStore()
    {
    }

    public static CustomConfigGroupStore getInstance()
    {
        return INSTANCE;
    }

    public synchronized List<Group> getGroups()
    {
        ensureLoaded();
        return this.groups.stream().map(Group::copy).toList();
    }

    public synchronized Group create(String name)
    {
        return this.create(HALF_MASA_SOURCE, name);
    }

    public synchronized Group create(String source, String name)
    {
        ensureLoaded();
        String trimmed = clean(name);
        if (trimmed.isEmpty())
        {
            return null;
        }

        Group group = new Group("custom_" + UUID.randomUUID(), normalizeSource(source), trimmed);
        this.groups.add(group);
        save();
        return group.copy();
    }

    /** Imports built-in groups once, preserving any configs already assigned by the user. */
    public synchronized boolean ensureDefaults(String source, List<GroupTemplate> templates)
    {
        ensureLoaded();
        String normalizedSource = normalizeSource(source);
        if (templates == null || templates.isEmpty() || this.initializedSources.contains(normalizedSource))
        {
            return false;
        }

        Set<String> ids = new HashSet<>();
        Set<String> assigned = new HashSet<>();
        for (Group group : this.groups)
        {
            ids.add(group.id);
            if (!normalizedSource.equals(normalizeSource(group.source)))
            {
                continue;
            }
            String main = mainReference(group);
            if (!main.isEmpty())
            {
                assigned.add(main);
            }
            assigned.addAll(group.children);
        }

        boolean changed = false;
        for (GroupTemplate template : templates)
        {
            if (template == null || ids.contains(clean(template.id)))
            {
                continue;
            }
            List<String> available = new ArrayList<>();
            String preferredMain = reference(normalizedSource, template.mainConfig);
            if (!preferredMain.isEmpty() && !assigned.contains(preferredMain))
            {
                available.add(preferredMain);
            }
            if (template.children != null)
            {
                for (String child : template.children)
                {
                    String candidate = reference(normalizedSource, nameOf(child));
                    if (!candidate.isEmpty() && !assigned.contains(candidate) && !available.contains(candidate))
                    {
                        available.add(candidate);
                    }
                }
            }
            if (available.size() < 2)
            {
                continue;
            }

            String groupName = clean(template.name);
            if (groupName.isEmpty())
            {
                groupName = nameOf(available.get(0));
            }
            Group group = new Group(clean(template.id), normalizedSource, groupName);
            if (group.id.isEmpty())
            {
                continue;
            }
            group.mainSource = normalizedSource;
            group.mainConfig = nameOf(available.get(0));
            group.children.addAll(available.subList(1, available.size()));
            this.groups.add(group);
            ids.add(group.id);
            assigned.addAll(available);
            changed = true;
        }
        this.initializedSources.add(normalizedSource);
        save();
        return changed;
    }

    public synchronized boolean rename(String groupId, String name)
    {
        Group group = find(groupId);
        String trimmed = clean(name);
        if (group == null || trimmed.isEmpty())
        {
            return false;
        }
        group.name = trimmed;
        save();
        return true;
    }

    public synchronized boolean delete(String groupId)
    {
        ensureLoaded();
        boolean removed = this.groups.removeIf(group -> group.id.equals(groupId));
        if (removed)
        {
            save();
        }
        return removed;
    }

    public synchronized boolean setMain(String groupId, String configName)
    {
        return this.setMain(groupId, HALF_MASA_SOURCE, configName);
    }

    public synchronized boolean setMain(String groupId, String source, String configName)
    {
        Group group = find(groupId);
        String config = clean(configName);
        String normalizedSource = normalizeSource(source);
        String reference = reference(normalizedSource, config);
        if (group == null || config.isEmpty())
        {
            return false;
        }

        String previousSource = normalizeSource(group.mainSource);
        String previousConfig = clean(group.mainConfig);
        String previousReference = reference(previousSource, previousConfig);
        if (sameMain(group, normalizedSource, config))
        {
            return true;
        }

        for (Group other : this.groups)
        {
            if (other != group && sameMain(other, normalizedSource, config))
            {
                other.mainConfig = "";
                promoteFirstChildToMain(other);
            }
            other.children.removeIf(reference::equals);
        }
        if (!normalizeSource(group.source).equals(normalizedSource))
        {
            group.children.clear();
        }
        else if (!previousReference.isEmpty())
        {
            group.children.remove(previousReference);
            group.children.add(0, previousReference);
        }
        group.source = normalizedSource;
        group.mainSource = normalizedSource;
        group.mainConfig = config;
        group.children.removeIf(reference::equals);
        save();
        return true;
    }

    /** Removes the main role while keeping the previous main config as the first child. */
    public synchronized boolean demoteMain(String groupId)
    {
        Group group = find(groupId);
        if (group == null || clean(group.mainConfig).isEmpty())
        {
            return false;
        }
        String main = reference(group.mainSource, group.mainConfig);
        group.mainConfig = "";
        group.mainSource = normalizeSource(group.source);
        group.children.remove(main);
        group.children.add(0, main);
        save();
        return true;
    }

    public synchronized boolean addChild(String groupId, String configName)
    {
        return this.addChild(groupId, HALF_MASA_SOURCE, configName);
    }

    public synchronized boolean addChild(String groupId, String source, String configName)
    {
        Group group = find(groupId);
        String config = clean(configName);
        String normalizedSource = normalizeSource(source);
        String reference = reference(normalizedSource, config);
        if (group == null || config.isEmpty() || !normalizedSource.equals(normalizeSource(group.source)) ||
                sameMain(group, normalizedSource, config))
        {
            return false;
        }

        for (Group other : this.groups)
        {
            if (other != group && sameMain(other, normalizedSource, config))
            {
                return false;
            }
        }
        for (Group other : this.groups)
        {
            other.children.removeIf(reference::equals);
        }
        if (!group.children.contains(reference))
        {
            group.children.add(reference);
        }
        save();
        return true;
    }

    public synchronized boolean removeChild(String groupId, String configName)
    {
        Group group = find(groupId);
        if (group == null)
        {
            return false;
        }
        boolean removed = group.children.remove(configName);
        if (removed)
        {
            save();
        }
        return removed;
    }

    public synchronized boolean removeConfig(String source, String configName)
    {
        ensureLoaded();
        String normalizedSource = normalizeSource(source);
        String config = clean(configName);
        String reference = reference(normalizedSource, config);
        boolean changed = false;
        for (Group group : this.groups)
        {
            if (!normalizedSource.equals(normalizeSource(group.source)))
            {
                continue;
            }
            if (sameMain(group, normalizedSource, config))
            {
                group.mainConfig = "";
                group.mainSource = normalizeSource(group.source);
                promoteFirstChildToMain(group);
                changed = true;
            }
            if (group.children.removeIf(reference::equals))
            {
                changed = true;
            }
        }
        if (changed)
        {
            save();
        }
        return changed;
    }

    public synchronized boolean moveConfig(String source, String configName, int delta)
    {
        ensureLoaded();
        String normalizedSource = normalizeSource(source);
        String config = clean(configName);
        String reference = reference(normalizedSource, config);
        for (int index = 0; index < this.groups.size(); index++)
        {
            Group group = this.groups.get(index);
            if (!normalizedSource.equals(normalizeSource(group.source)))
            {
                continue;
            }
            if (sameMain(group, normalizedSource, config))
            {
                return this.moveGroup(group.id, index + delta);
            }
            int childIndex = group.children.indexOf(reference);
            if (childIndex >= 0)
            {
                return this.moveChild(group.id, reference, childIndex + delta);
            }
        }
        return false;
    }

    public synchronized boolean moveChild(String groupId, String configName, int targetIndex)
    {
        Group group = find(groupId);
        if (group == null)
        {
            return false;
        }
        int sourceIndex = group.children.indexOf(configName);
        if (sourceIndex < 0)
        {
            return false;
        }
        group.children.remove(sourceIndex);
        int index = Math.max(0, Math.min(targetIndex, group.children.size()));
        group.children.add(index, configName);
        if (sourceIndex != index)
        {
            save();
        }
        return sourceIndex != index;
    }

    public synchronized boolean moveChildToGroup(
            String sourceGroupId, String configName, String targetGroupId, int targetIndex)
    {
        return this.moveChildToGroup(sourceGroupId, configName, HALF_MASA_SOURCE, targetGroupId, targetIndex);
    }

    public synchronized boolean moveChildToGroup(
            String sourceGroupId, String configName, String configSource,
            String targetGroupId, int targetIndex)
    {
        Group sourceGroup = find(sourceGroupId);
        Group target = find(targetGroupId);
        String normalizedSource = normalizeSource(configSource);
        String reference = reference(normalizedSource, configName);
        if (sourceGroup == null || target == null || sourceGroup == target ||
                !normalizedSource.equals(normalizeSource(sourceGroup.source)) ||
                !normalizedSource.equals(normalizeSource(target.source)) ||
                sameMain(target, normalizedSource, configName))
        {
            return false;
        }
        if (!sourceGroup.children.remove(reference))
        {
            return false;
        }
        target.children.remove(reference);
        int index = Math.max(0, Math.min(targetIndex, target.children.size()));
        target.children.add(index, reference);
        save();
        return true;
    }

    public synchronized boolean moveGroup(String groupId, int targetIndex)
    {
        ensureLoaded();
        int sourceIndex = indexOf(groupId);
        if (sourceIndex < 0)
        {
            return false;
        }
        Group group = this.groups.remove(sourceIndex);
        int index = Math.max(0, Math.min(targetIndex, this.groups.size()));
        this.groups.add(index, group);
        if (sourceIndex != index)
        {
            save();
        }
        return sourceIndex != index;
    }

    public synchronized boolean setExpanded(String groupId, boolean expanded)
    {
        Group group = find(groupId);
        if (group == null || group.expanded == expanded)
        {
            return false;
        }
        group.expanded = expanded;
        save();
        return true;
    }

    private Group find(String groupId)
    {
        ensureLoaded();
        return this.groups.stream().filter(group -> group.id.equals(groupId)).findFirst().orElse(null);
    }

    private int indexOf(String groupId)
    {
        for (int index = 0; index < this.groups.size(); index++)
        {
            if (this.groups.get(index).id.equals(groupId))
            {
                return index;
            }
        }
        return -1;
    }

    private void ensureLoaded()
    {
        if (this.loaded)
        {
            return;
        }
        this.loaded = true;
        Path file = file();
        if (!Files.isReadable(file))
        {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null && data.groups != null)
            {
                if (data.initializedSources != null)
                {
                    for (String source : data.initializedSources)
                    {
                        this.initializedSources.add(normalizeSource(source));
                    }
                }
                Set<String> ids = new HashSet<>();
                for (Group group : data.groups)
                {
                    if (group == null)
                    {
                        continue;
                    }
                    group.id = clean(group.id);
                    group.source = normalizeSource(group.source);
                    group.name = clean(group.name);
                    group.mainSource = normalizeSource(group.mainSource);
                    group.mainConfig = clean(group.mainConfig);
                    if (group.id.isEmpty() || group.name.isEmpty() || !ids.add(group.id))
                    {
                        continue;
                    }
                    if (group.children == null)
                    {
                        group.children = new ArrayList<>();
                    }
                    List<String> normalizedChildren = new ArrayList<>();
                    Set<String> children = new HashSet<>();
                    for (String child : group.children)
                    {
                        String normalized = normalizeReference(child);
                        if (!normalized.isEmpty() && children.add(normalized) &&
                                !normalized.equals(mainReference(group)))
                        {
                            normalizedChildren.add(normalized);
                        }
                    }
                    group.children = normalizedChildren;
                    this.groups.add(group);
                }
            }
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to read custom configuration groups", exception);
        }
    }

    private static boolean promoteFirstChildToMain(Group group)
    {
        if (!clean(group.mainConfig).isEmpty() || group.children == null || group.children.isEmpty())
        {
            return false;
        }
        String first = group.children.remove(0);
        group.mainSource = sourceOf(first);
        group.mainConfig = nameOf(first);
        group.source = normalizeSource(group.mainSource);
        return !group.mainConfig.isEmpty();
    }

    private void save()
    {
        Path file = file();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try
        {
            Files.createDirectories(file.getParent());
            Data data = new Data();
            data.groups.addAll(this.groups);
            data.initializedSources.addAll(this.initializedSources);
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                GSON.toJson(data, writer);
            }
            try
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException exception)
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to save custom configuration groups", exception);
        }
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.trim();
    }

    public static String reference(String source, String configName)
    {
        String name = clean(configName);
        return name.isEmpty() ? "" : normalizeSource(source) + REF_SEPARATOR + name;
    }

    public static String sourceOf(String reference)
    {
        String value = clean(reference);
        int separator = value.indexOf(REF_SEPARATOR);
        return separator > 0 ? normalizeSource(value.substring(0, separator)) : HALF_MASA_SOURCE;
    }

    public static String nameOf(String reference)
    {
        String value = clean(reference);
        int separator = value.indexOf(REF_SEPARATOR);
        return separator > 0 ? clean(value.substring(separator + REF_SEPARATOR.length())) : value;
    }

    private static String normalizeReference(String value)
    {
        String cleaned = clean(value);
        if (cleaned.isEmpty())
        {
            return "";
        }
        return reference(sourceOf(cleaned), nameOf(cleaned));
    }

    private static String normalizeSource(String source)
    {
        return TWEAKEROO_SOURCE.equalsIgnoreCase(clean(source))
                ? TWEAKEROO_SOURCE : HALF_MASA_SOURCE;
    }

    private static boolean sameMain(Group group, String source, String configName)
    {
        return normalizeSource(source).equals(normalizeSource(group.mainSource)) &&
                clean(configName).equals(clean(group.mainConfig));
    }

    private static String mainReference(Group group)
    {
        return reference(group.mainSource, group.mainConfig);
    }

    private static Path file()
    {
        return Configs.getHalfMasaDirectory().resolve("custom-config-groups.json");
    }

    private static final class Data
    {
        private List<Group> groups = new ArrayList<>();
        private List<String> initializedSources = new ArrayList<>();
    }

    public record GroupTemplate(String id, String name, String mainConfig, List<String> children)
    {
    }

    public static final class Group
    {
        public String id;
        public String source = HALF_MASA_SOURCE;
        public String name;
        public String mainSource = HALF_MASA_SOURCE;
        public String mainConfig = "";
        public List<String> children = new ArrayList<>();
        public boolean expanded;

        private Group(String id, String source, String name)
        {
            this.id = id;
            this.source = normalizeSource(source);
            this.name = name;
            this.mainSource = this.source;
        }

        private Group copy()
        {
            Group copy = new Group(this.id, this.source, this.name);
            copy.mainSource = this.mainSource;
            copy.mainConfig = this.mainConfig;
            copy.children = new ArrayList<>(this.children);
            copy.expanded = this.expanded;
            return copy;
        }
    }
}
