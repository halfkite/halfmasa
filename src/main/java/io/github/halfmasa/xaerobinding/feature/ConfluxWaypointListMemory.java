package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/** Only browsing state is persisted; selections and pending actions are never restored. */
public final class ConfluxWaypointListMemory
{
    public record State(String tab, String dimensionMode, String dimension, String set,
                        String search, int nativeScroll, int compactScroll)
    {
        public State
        {
            tab = "PUBLIC".equals(tab) ? "PUBLIC" : "LOCAL";
            dimensionMode = "ALL".equals(dimensionMode) || "ONLY".equals(dimensionMode) ? dimensionMode : "CURRENT";
            search = search == null ? "" : search;
            nativeScroll = Math.max(0, nativeScroll);
            compactScroll = Math.max(0, compactScroll);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, State> STATES = new HashMap<>();
    private static boolean loaded, dirty;
    private ConfluxWaypointListMemory() {}

    public static boolean enabled()
    { return Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue() && Configs.CONFLUX_MAP_REMEMBER_LIST.getBooleanValue(); }

    private static Path file()
    { return Configs.getHalfMasaDirectory().resolve("conflux-waypoint-list.json"); }

    private static void load()
    {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(file())) return;
        try (var input = Files.newBufferedReader(file()))
        {
            JsonObject root = JsonParser.parseReader(input).getAsJsonObject();
            if (!root.has("version") || !root.get("version").isJsonPrimitive()
                    || !root.has("worlds") || !root.get("worlds").isJsonObject())
                throw new JsonParseException("Missing waypoint browsing state schema");
            if (root.get("version").getAsInt() != 1) return;
            for (var entry : root.getAsJsonObject("worlds").entrySet())
            {
                State state = GSON.fromJson(entry.getValue(), State.class);
                if (state != null) STATES.put(entry.getKey(), state);
            }
        }
        catch (IOException | JsonParseException | IllegalStateException | NumberFormatException error)
        {
            STATES.clear();
            XaeroWorldBinding.LOGGER.warn("Cannot read Conflux waypoint browsing state; using opening defaults", error);
        }
    }

    public static State get(String world)
    { if (!enabled() || world == null) return null; load(); return STATES.get(world); }

    public static void save(String world, State state)
    {
        if (!enabled() || world == null || state == null) return;
        load();
        if (state.equals(STATES.put(world, state)) && !dirty) return;
        dirty = true;
        Path temporary = null;
        try
        {
            Files.createDirectories(file().getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.add("worlds", GSON.toJsonTree(STATES));
            temporary = Files.createTempFile(file().getParent(), "conflux-waypoint-list-", ".tmp");
            try (var output = Files.newBufferedWriter(temporary)) { GSON.toJson(root, output); }
            try { Files.move(temporary, file(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported)
            { Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        }
        catch (IOException error) { XaeroWorldBinding.LOGGER.warn("Cannot save Conflux waypoint browsing state", error); }
        finally
        {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (IOException error) { XaeroWorldBinding.LOGGER.warn("Cannot clean Conflux browsing state temporary file", error); }
        }
    }
}
//#endif
