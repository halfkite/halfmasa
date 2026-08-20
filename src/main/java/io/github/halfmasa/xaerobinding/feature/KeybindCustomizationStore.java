package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.KeyMapping;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class KeybindCustomizationStore
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final KeybindCustomizationStore INSTANCE = new KeybindCustomizationStore();
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private boolean loaded;

    private KeybindCustomizationStore() {}

    public static KeybindCustomizationStore getInstance()
    {
        return INSTANCE;
    }

    public synchronized boolean reload()
    {
        this.entries.clear();
        this.loaded = true;
        Path file = file();
        if (!Files.isReadable(file))
        {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null && data.bindings != null)
            {
                data.bindings.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> this.entries.put(entry.getKey(), sanitize(entry.getValue())));
            }
            return true;
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.error("Failed to read keybind pie customizations", exception);
            return false;
        }
    }

    public synchronized void save()
    {
        ensureLoaded();
        try
        {
            Path file = file();
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling("bindings.json.tmp");
            Data data = new Data();
            this.entries.entrySet().stream()
                    .filter(entry -> !entry.getValue().isDefault())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> data.bindings.put(entry.getKey(), entry.getValue()));
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                GSON.toJson(data, writer);
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.error("Failed to save keybind pie customizations", exception);
        }
    }

    public synchronized Entry get(KeyMapping mapping)
    {
        ensureLoaded();
        return this.entries.computeIfAbsent(mapping.getName(), ignored -> new Entry());
    }

    public synchronized String displayName(KeyMapping mapping)
    {
        Entry entry = get(mapping);
        if (entry.displayName != null && !entry.displayName.isBlank())
        {
            return entry.displayName;
        }

        String action = net.minecraft.network.chat.Component.translatable(mapping.getName()).getString();
        if (entry.hideCategory)
        {
            return action;
        }
        //#if MC >= 1.21.10
        String category = mapping.getCategory().label().getString();
        //#else
        //$$ String category = net.minecraft.network.chat.Component.translatable(mapping.getCategory()).getString();
        //#endif
        return category + ": " + action;
    }

    public synchronized void reset(KeyMapping mapping)
    {
        ensureLoaded();
        this.entries.remove(mapping.getName());
        save();
    }

    private void ensureLoaded()
    {
        if (!this.loaded)
        {
            reload();
        }
    }

    private static Entry sanitize(Entry entry)
    {
        if (entry == null)
        {
            return new Entry();
        }
        if (entry.displayName != null && entry.displayName.isBlank())
        {
            entry.displayName = null;
        }
        if (entry.sectorColor != null)
        {
            entry.sectorColor &= 0xFFFFFF;
        }
        return entry;
    }

    private static Path file()
    {
        return Configs.getHalfMasaDirectory().resolve("keybind-pie").resolve("bindings.json");
    }

    private static final class Data
    {
        private Map<String, Entry> bindings = new LinkedHashMap<>();
    }

    public static final class Entry
    {
        public String displayName;
        public boolean hideCategory;
        public Integer sectorColor;

        private boolean isDefault()
        {
            return (this.displayName == null || this.displayName.isBlank()) && !this.hideCategory && this.sectorColor == null;
        }
    }
}
