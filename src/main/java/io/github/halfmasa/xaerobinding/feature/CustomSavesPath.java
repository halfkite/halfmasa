package io.github.halfmasa.xaerobinding.feature;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

public final class CustomSavesPath
{
    private static final String CONFIG_FILE_NAME = "halfmasa.json";
    private static final String CONFIG_CATEGORY = "Ported";
    private static final String LEGACY_CONFIG_CATEGORY = "Generic";
    private static final String CONFIG_KEY = "customSavesPath";

    private CustomSavesPath()
    {
    }

    public static Path replaceDefault(Path original)
    {
        Path gameDirectory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        Path defaultSaves = gameDirectory.resolve("saves").normalize();
        if (!original.toAbsolutePath().normalize().equals(defaultSaves))
        {
            return original;
        }

        String configured = readConfiguredPath();
        if (configured == null || configured.isBlank())
        {
            return original;
        }

        try
        {
            Path replacement = Path.of(configured.trim());
            if (!replacement.isAbsolute())
            {
                replacement = gameDirectory.resolve(replacement);
            }
            replacement = replacement.toAbsolutePath().normalize();
            Files.createDirectories(replacement);
            XaeroWorldBinding.LOGGER.info("Using custom saves directory {}", replacement);
            return replacement;
        }
        catch (InvalidPathException | IOException | SecurityException exception)
        {
            XaeroWorldBinding.LOGGER.error(
                    "Invalid or inaccessible custom saves directory '{}'; using {}",
                    configured,
                    original,
                    exception);
            return original;
        }
    }

    private static String readConfiguredPath()
    {
        Path configDirectory = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDirectory.resolve("halfmasa").resolve(CONFIG_FILE_NAME);
        if (!Files.isReadable(configFile))
        {
            configFile = configDirectory.resolve(CONFIG_FILE_NAME);
        }
        if (!Files.isReadable(configFile))
        {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8))
        {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject())
            {
                return null;
            }
            JsonObject root = rootElement.getAsJsonObject();
            JsonObject category = root.getAsJsonObject(CONFIG_CATEGORY);
            if (category == null)
            {
                category = root.getAsJsonObject(LEGACY_CONFIG_CATEGORY);
            }
            if (category == null)
            {
                return null;
            }
            JsonElement value = category.get(CONFIG_KEY);
            if (value != null && value.isJsonPrimitive()) return value.getAsString();
            JsonElement paths = category.get("customSavesPaths");
            if (paths != null && paths.isJsonArray())
            {
                for (JsonElement entry : paths.getAsJsonArray())
                {
                    if (entry.isJsonPrimitive() && !entry.getAsString().isBlank()) return entry.getAsString();
                }
            }
            return null;
        }
        catch (IOException | RuntimeException exception)
        {
            XaeroWorldBinding.LOGGER.warn("Unable to read custom saves directory from {}", configFile, exception);
            return null;
        }
    }
}
