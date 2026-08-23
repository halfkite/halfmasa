package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class ResourcePackCompatibilityBypass
{
    private static final String CATEGORY = "Ported";
    private static final String LEGACY_CATEGORY = "Client";
    private static final String CONFIG_NAME = "skipResourcePackCompatibilityCheck";
    private static volatile Boolean earlyValue;

    private ResourcePackCompatibilityBypass()
    {
    }

    public static boolean isEnabled()
    {
        if (Configs.isConfigLoaded())
        {
            return Configs.SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK.getBooleanValue();
        }

        Boolean cached = earlyValue;
        if (cached == null)
        {
            synchronized (ResourcePackCompatibilityBypass.class)
            {
                cached = earlyValue;
                if (cached == null)
                {
                    cached = readEarlyValue();
                    earlyValue = cached;
                }
            }
        }
        return cached;
    }

    private static boolean readEarlyValue()
    {
        Path configDirectory = FabricLoader.getInstance().getConfigDir();
        Path current = configDirectory.resolve("halfmasa").resolve("halfmasa.json");
        Path legacy = configDirectory.resolve("halfmasa.json");
        Path file = Files.isReadable(current) ? current : legacy;
        if (!Files.isReadable(file))
        {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject())
            {
                return false;
            }

            JsonObject root = rootElement.getAsJsonObject();
            Boolean value = readCategory(root, CATEGORY);
            if (value == null)
            {
                value = readCategory(root, LEGACY_CATEGORY);
            }
            return Boolean.TRUE.equals(value);
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn(
                    "Failed to read the resource pack compatibility setting early from {}",
                    file.toAbsolutePath(),
                    exception);
            return false;
        }
    }

    private static Boolean readCategory(JsonObject root, String categoryName)
    {
        JsonElement categoryElement = root.get(categoryName);
        if (categoryElement == null || !categoryElement.isJsonObject())
        {
            return null;
        }

        JsonElement value = categoryElement.getAsJsonObject().get(CONFIG_NAME);
        if (value == null)
        {
            return null;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean())
        {
            return value.getAsBoolean();
        }
        if (value.isJsonObject())
        {
            JsonElement enabled = value.getAsJsonObject().get("enabled");
            if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean())
            {
                return enabled.getAsBoolean();
            }
        }
        return null;
    }
}
