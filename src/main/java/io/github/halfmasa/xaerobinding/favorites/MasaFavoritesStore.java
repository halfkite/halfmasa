//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Only membership is persisted here; values belong to their original mods. */
public final class MasaFavoritesStore
{
    private static final MasaFavoritesStore INSTANCE = new MasaFavoritesStore(
            Configs.getHalfMasaDirectory().resolve("masa-favorites.json"));
    private final Path file;
    private final Set<String> favorites = new LinkedHashSet<>();
    private boolean loaded;
    private boolean writable = true;

    public MasaFavoritesStore(Path file) { this.file = file; }
    public static MasaFavoritesStore get() { return INSTANCE; }

    public synchronized List<String> snapshot()
    {
        this.load();
        return List.copyOf(this.favorites);
    }

    public synchronized boolean contains(String key)
    {
        this.load();
        return this.favorites.contains(key);
    }

    /** Commit to disk before changing UI-visible membership. */
    public synchronized boolean setFavorite(String key, boolean favorite)
    {
        this.load();
        if (!this.writable || key == null || key.isBlank()) return false;
        Set<String> next = new LinkedHashSet<>(this.favorites);
        if (favorite) next.add(key); else next.remove(key);
        if (next.equals(this.favorites)) return true;
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray entries = new JsonArray();
        next.forEach(entries::add);
        root.add("favorites", entries);
        Path temporary = null;
        try
        {
            Files.createDirectories(this.file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(this.file.toAbsolutePath().getParent(), "masa-favorites-", ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            try
            {
                Files.move(temporary, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException unsupported)
            {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            this.favorites.clear();
            this.favorites.addAll(next);
            return true;
        }
        catch (IOException error)
        {
            XaeroWorldBinding.LOGGER.error("Unable to save Masa favorites to {}", this.file, error);
            return false;
        }
        finally
        {
            if (temporary != null)
            {
                try { Files.deleteIfExists(temporary); }
                catch (IOException error) { XaeroWorldBinding.LOGGER.warn("Unable to remove {}", temporary, error); }
            }
        }
    }

    private void load()
    {
        if (this.loaded) return;
        this.loaded = true;
        if (!Files.exists(this.file)) return;
        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported favorites version");
            Set<String> parsed = new LinkedHashSet<>();
            for (var entry : root.getAsJsonArray("favorites"))
            {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString() || entry.getAsString().isBlank())
                    throw new IllegalArgumentException("Invalid favorite identifier");
                parsed.add(entry.getAsString());
            }
            this.favorites.addAll(parsed);
        }
        catch (IOException | RuntimeException error)
        {
            // Keep the original file intact; a failed read must never become a destructive empty save.
            this.writable = false;
            XaeroWorldBinding.LOGGER.error("Unable to read Masa favorites; preserving {}", this.file, error);
        }
    }
}
//#endif
