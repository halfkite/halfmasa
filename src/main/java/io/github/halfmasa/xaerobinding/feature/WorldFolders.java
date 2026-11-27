package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class WorldFolders
{
    public record Folder(String id, String name, String note) {}
    public record RowKey(Path path, String id, boolean folder) {}
    private record Entry(String folder, boolean favorite) {}

    private static final class Root
    {
        String note = "";
        Map<String, Folder> folders = new LinkedHashMap<>();
        Map<String, Entry> worlds = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
    }

    private static final class Document
    {
        int version = 1;
        Map<String, Root> roots = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final class Holder
    {
        static final WorldFolders INSTANCE = new WorldFolders(Configs.getHalfMasaDirectory().resolve("world-folders.json"));
    }
    public static WorldFolders getInstance() { return Holder.INSTANCE; }

    public static void refresh(net.minecraft.client.gui.screens.worldselection.WorldSelectionList list)
    {
        if (list == null) return;
        var access = (io.github.halfmasa.xaerobinding.mixin.WorldSelectionListAccessor) list;
        var levels = access.halfmasa$getLevels();
        if (levels != null) access.halfmasa$fillLevels(access.halfmasa$getFilter(), levels);
    }

    private final Path file;
    private Document data = new Document();
    private boolean writable = true;
    private Path selectedRoot;
    private String selectedFolder;
    private final Map<Path, Boolean> expandedRoots = new LinkedHashMap<>();
    private final java.util.Set<String> closedFolders = new java.util.HashSet<>();

    public WorldFolders(Path file)
    {
        this.file = file;
        if (!Files.exists(file)) return;
        try
        {
            if (Files.size(file) > 8 * 1024 * 1024) throw new IOException("World folders file exceeds size limit");
            var saved = GSON.fromJson(Files.readString(file), Document.class);
            if (saved == null || saved.version != 1 || saved.roots == null
                    || saved.roots.values().stream().anyMatch(root -> root == null || root.folders == null || root.worlds == null))
                throw new IOException("Invalid world folders data");
            this.data = saved;
        }
        catch (IOException | JsonParseException e)
        {
            this.writable = false;
            XaeroWorldBinding.LOGGER.error("Unable to load world folders; original data preserved", e);
        }
    }

    private static String key(Path path) { return path.toAbsolutePath().normalize().toString(); }
    private Root root(Path path) { return this.data.roots.get(key(path)); }

    public List<Folder> folders(Path path)
    {
        Root root = root(path);
        return root == null ? List.of() : new ArrayList<>(root.folders.values());
    }

    public String note(Path path)
    {
        Root root = root(path);
        return root == null || root.note == null ? "" : root.note;
    }

    public boolean setNote(Path path, String note) { return edit(path, root -> root.note = note.strip()); }

    public boolean saveFolder(Path path, String id, String name, String note)
    {
        if (name.isBlank()) return false;
        String key = id == null ? UUID.randomUUID().toString() : id;
        return edit(path, root -> root.folders.put(key, new Folder(key, name.strip(), note.strip())));
    }

    public boolean removeFolder(Path path, String id)
    {
        return edit(path, root -> {
            root.folders.remove(id);
            root.worlds.replaceAll((world, entry) ->
                    Objects.equals(entry.folder(), id) ? new Entry(null, entry.favorite()) : entry);
        });
    }

    public boolean assign(Path path, String world, String folder)
    {
        Root root = root(path);
        if (folder != null && (root == null || !root.folders.containsKey(folder))) return false;
        return edit(path, value -> updateWorld(value, world, new Entry(folder, isFavorite(path, world))));
    }

    private static void updateWorld(Root root, String world, Entry entry)
    {
        if (entry.folder() == null && !entry.favorite()) root.worlds.remove(world);
        else root.worlds.put(world, entry);
    }

    public boolean toggleFavorite(Path path, String world)
    {
        return edit(path, root -> updateWorld(root, world, new Entry(folderOf(path, world), !isFavorite(path, world))));
    }

    public boolean isFavorite(Path path, String world)
    {
        Root root = root(path);
        Entry entry = root == null ? null : root.worlds.get(world);
        return entry != null && entry.favorite();
    }

    public String folderOf(Path path, String world)
    {
        Root root = root(path);
        Entry entry = root == null ? null : root.worlds.get(world);
        return entry != null && entry.folder() != null && root.folders.containsKey(entry.folder()) ? entry.folder() : null;
    }

    public String selectedFolder(Path path)
    {
        if (!Objects.equals(path, this.selectedRoot) || this.selectedFolder == null) return null;
        Root root = root(path);
        return root != null && root.folders.containsKey(this.selectedFolder) ? this.selectedFolder : null;
    }

    public void select(Path path, String folder)
    {
        this.selectedRoot = path;
        this.selectedFolder = folder;
        this.expandedRoots.put(path, true);
        if (folder != null) this.closedFolders.remove(folder);
    }

    public boolean rootExpanded(Path path)
    {
        return this.expandedRoots.getOrDefault(path, path.equals(CustomSavesPath.getCurrentPath()));
    }

    public void toggleRoot(Path path)
    {
        this.expandedRoots.put(path, !rootExpanded(path));
    }

    public void expandRoot(Path path)
    {
        this.expandedRoots.put(path, true);
    }

    public boolean isExpanded(String folder) { return !this.closedFolders.contains(folder); }

    public void toggleFolder(String folder)
    {
        if (!this.closedFolders.add(folder)) this.closedFolders.remove(folder);
    }

    public List<String> order(Path path)
    {
        Root root = root(path);
        return root == null || root.order == null ? List.of() : List.copyOf(root.order);
    }

    public boolean move(Path path, String world, String folder, String before, List<String> listed)
    {
        Root old = root(path);
        if (world.equals(before) || folder != null && (old == null || !old.folders.containsKey(folder))) return false;
        return edit(path, root -> {
            root.order.retainAll(listed);
            for (String id : listed) if (!root.order.contains(id)) root.order.add(id);
            root.order.remove(world);
            int index = root.order.indexOf(before);
            if (index < 0) root.order.add(world);
            else root.order.add(index, world);
            updateWorld(root, world, new Entry(folder, isFavorite(path, world)));
        });
    }

    private boolean edit(Path path, Consumer<Root> change)
    {
        if (!this.writable) return false;
        String key = key(path);
        Root old = this.data.roots.get(key);
        Root next = new Root();
        if (old != null)
        {
            next.note = old.note;
            next.folders.putAll(old.folders);
            next.worlds.putAll(old.worlds);
            if (old.order != null) next.order.addAll(old.order);
        }
        change.accept(next);
        this.data.roots.put(key, next);
        Path temporary = null;
        try
        {
            Files.createDirectories(this.file.getParent());
            temporary = Files.createTempFile(this.file.getParent(), "world-folders-", ".tmp");
            Files.writeString(temporary, GSON.toJson(this.data));
            try { Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING); }
            return true;
        }
        catch (IOException e)
        {
            if (old == null) this.data.roots.remove(key);
            else this.data.roots.put(key, old);
            XaeroWorldBinding.LOGGER.error("Unable to save world folders", e);
            return false;
        }
        finally
        {
            if (temporary != null)
            {
                try { Files.deleteIfExists(temporary); }
                catch (IOException e) { XaeroWorldBinding.LOGGER.warn("Unable to clean up world folders temporary file", e); }
            }
        }
    }
}
//#endif
