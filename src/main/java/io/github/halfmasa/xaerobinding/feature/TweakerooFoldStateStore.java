package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Persists Tweakeroo-only expansion state without touching tweakeroo.json. */
public final class TweakerooFoldStateStore
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type DATA_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private static final Map<String, Boolean> STATES = new HashMap<>();
    private static boolean loaded;

    private TweakerooFoldStateStore()
    {
    }

    public static synchronized boolean isExpanded(String groupId)
    {
        ensureLoaded();
        return STATES.getOrDefault(groupId, false);
    }

    public static synchronized void setExpanded(String groupId, boolean expanded)
    {
        ensureLoaded();
        if (expanded)
        {
            STATES.put(groupId, true);
        }
        else
        {
            STATES.remove(groupId);
        }
        save();
    }

    private static void ensureLoaded()
    {
        if (loaded)
        {
            return;
        }
        loaded = true;
        Path file = file();
        if (!Files.isReadable(file))
        {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            Map<String, Boolean> values = GSON.fromJson(reader, DATA_TYPE);
            if (values != null)
            {
                values.forEach((key, value) -> {
                    if (key != null && !key.isBlank() && Boolean.TRUE.equals(value))
                    {
                        STATES.put(key, true);
                    }
                });
            }
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to read Tweakeroo fold state", exception);
        }
    }

    private static void save()
    {
        Path file = file();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try
        {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                GSON.toJson(STATES, DATA_TYPE, writer);
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
            XaeroWorldBinding.LOGGER.warn("Failed to save Tweakeroo fold state", exception);
        }
    }

    private static Path file()
    {
        return Configs.getHalfMasaDirectory().resolve("tweakeroo-folds.json");
    }
}
