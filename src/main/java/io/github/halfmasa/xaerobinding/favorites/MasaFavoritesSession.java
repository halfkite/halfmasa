//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;

/** Membership is frozen for a visit, while each row keeps its live original config. */
public final class MasaFavoritesSession
{
    private final List<MasaConfigCatalog.Entry> snapshot;
    private final Map<String, String> savedValues = new LinkedHashMap<>();
    private final Map<String, MasaConfigCatalog.Entry> tracked = new LinkedHashMap<>();

    public MasaFavoritesSession()
    {
        MasaConfigCatalog.discover();
        this.snapshot = MasaFavoritesStore.get().snapshot().stream().map(MasaConfigCatalog::find)
                .filter(java.util.Objects::nonNull).toList();
        this.snapshot.forEach(this::track);
    }

    public List<MasaConfigCatalog.Entry> snapshot() { return this.snapshot; }

    public void track(MasaConfigCatalog.Entry entry)
    {
        if (this.tracked.putIfAbsent(entry.key(), entry) == null)
            this.savedValues.put(entry.key(), fingerprint(entry));
    }

    public Set<String> changedMods()
    {
        Set<String> changed = new LinkedHashSet<>();
        for (var entry : this.tracked.values())
            if (!fingerprint(entry).equals(this.savedValues.get(entry.key()))) changed.add(entry.modId());
        return changed;
    }

    public void saveChanges()
    {
        Set<String> changed = this.changedMods();
        for (String mod : changed) ConfigManager.getInstance().onConfigsChanged(mod);
        if (!changed.isEmpty()) InputEventHandler.getKeybindManager().updateUsedKeys();
        this.tracked.values().forEach(entry -> this.savedValues.put(entry.key(), fingerprint(entry)));
    }

    private static String fingerprint(MasaConfigCatalog.Entry entry)
    {
        JsonObject state = new JsonObject();
        state.add("value", entry.identity().getAsJsonElement());
        if (entry.editable() instanceof IHotkey hotkey && hotkey.getKeybind() != null)
            state.add("hotkey", hotkey.getKeybind().getAsJsonElement());
        return state.toString();
    }
}
//#endif
