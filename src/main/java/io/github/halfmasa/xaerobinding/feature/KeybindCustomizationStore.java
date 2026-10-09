package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;

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
        return this.get(mapping.getName());
    }

    public synchronized Entry get(String bindingId)
    {
        ensureLoaded();
        return this.entries.computeIfAbsent(bindingId, ignored -> new Entry());
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

    public synchronized boolean isActive(KeyMapping mapping, Screen screen)
    {
        return get(mapping).activationContext.isActive(screen != null);
    }

    public synchronized boolean hasCustomCombination(KeyMapping mapping)
    {
        return !get(mapping).comboKeys.isEmpty();
    }

    public synchronized List<Integer> comboKeys(KeyMapping mapping)
    {
        return new ArrayList<>(get(mapping).comboKeys);
    }

    public synchronized boolean requiresKeyOrder(KeyMapping mapping)
    {
        return get(mapping).requireKeyOrder;
    }

    public synchronized ActivationContext activationContext(KeyMapping mapping)
    {
        return get(mapping).activationContext;
    }

    //#if MC >= 26.3
    public synchronized boolean participatesInWheel(KeyMapping mapping)
    {
        return participatesInWheel(mapping.getName());
    }

    public synchronized boolean participatesInWheel(String bindingId)
    {
        Entry entry = get(bindingId);
        return entry.wheelEnabled != null ? entry.wheelEnabled : !entry.disableWheel;
    }

    public synchronized boolean participatesInWheel(String bindingId, List<Integer> keys)
    {
        return get(bindingId).participatesInWheel(keys);
    }

    public synchronized void put(String bindingId, Entry entry)
    {
        ensureLoaded();
        this.entries.put(bindingId, sanitize(entry.copy()));
    }
    //#endif

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
        if (entry.activationContext == null)
        {
            entry.activationContext = ActivationContext.AUTO;
        }
        if (entry.comboKeys == null)
        {
            entry.comboKeys = new ArrayList<>();
        }
        Set<Integer> seen = new HashSet<>();
        entry.comboKeys.removeIf(key -> key == null || !seen.add(key));
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
        public ActivationContext activationContext = ActivationContext.AUTO;
        public List<Integer> comboKeys = new ArrayList<>();
        public boolean requireKeyOrder;
        //#if MC >= 26.3
        public boolean disableWheel;
        /** Null keeps the key-dependent default; explicit choices override it. */
        public Boolean wheelEnabled;

        public boolean participatesInWheel(List<Integer> keys)
        {
            if (this.wheelEnabled != null) return this.wheelEnabled;
            if (this.disableWheel) return false;
            if (keys.size() != 1) return true;
            // SDL scancodes for left/right Ctrl, Shift and Alt, plus primary mouse buttons.
            // Hardware Fn normally produces no key event and cannot open a wheel.
            return switch (keys.getFirst())
            {
                case -1, -2, -3, 0, 224, 225, 226, 228, 229, 230 -> false;
                default -> true;
            };
        }

        public Entry copy()
        {
            Entry copy = new Entry();
            copy.displayName = this.displayName;
            copy.hideCategory = this.hideCategory;
            copy.sectorColor = this.sectorColor;
            copy.activationContext = this.activationContext;
            copy.comboKeys = new ArrayList<>(this.comboKeys);
            copy.requireKeyOrder = this.requireKeyOrder;
            copy.disableWheel = this.disableWheel;
            copy.wheelEnabled = this.wheelEnabled;
            return copy;
        }

        public boolean sameAs(Entry other)
        {
            return java.util.Objects.equals(this.displayName, other.displayName) &&
                    this.hideCategory == other.hideCategory &&
                    java.util.Objects.equals(this.sectorColor, other.sectorColor) &&
                    this.activationContext == other.activationContext && this.comboKeys.equals(other.comboKeys) &&
                    this.requireKeyOrder == other.requireKeyOrder && this.disableWheel == other.disableWheel &&
                    java.util.Objects.equals(this.wheelEnabled, other.wheelEnabled);
        }
        //#endif

        private boolean isDefault()
        {
            return (this.displayName == null || this.displayName.isBlank()) &&
                    !this.hideCategory && this.sectorColor == null &&
                    this.activationContext == ActivationContext.AUTO &&
                    this.comboKeys.isEmpty() && !this.requireKeyOrder
                    //#if MC >= 26.3
                    && !this.disableWheel && this.wheelEnabled == null
                    //#endif
                    ;
        }
    }

    public enum ActivationContext
    {
        AUTO,
        GAMEPLAY,
        SCREEN,
        ANY,
        DISABLED;

        public boolean isActive(boolean hasScreen)
        {
            return switch (this)
            {
                case AUTO, GAMEPLAY -> !hasScreen;
                case SCREEN -> hasScreen;
                case ANY -> true;
                case DISABLED -> false;
            };
        }

        public ActivationContext next()
        {
            ActivationContext[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public String translationKey()
        {
            return "halfmasa.gui.keybind_editor.context." + this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
