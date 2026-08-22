package io.github.halfmasa.xaerobinding.feature;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

public final class BetterSavedHotbarStorage
{
    private static final String DIRECTORY_NAME = "better-saved-hotbars";

    private BetterSavedHotbarStorage()
    {
    }

    public static Path prepare(Path gameDirectory, String fileName)
    {
        Path original = gameDirectory.resolve(fileName);
        Path directory = gameDirectory.resolve("config").resolve("halfmasa").resolve(DIRECTORY_NAME);
        Path target = directory.resolve(fileName);

        try
        {
            Files.createDirectories(directory);
            if (Files.notExists(target) && Files.isRegularFile(original))
            {
                Files.copy(original, target);
                XaeroWorldBinding.LOGGER.info("Migrated saved hotbars from {} to {}", original, target);
            }
            return target;
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn(
                    "Failed to prepare halfmasa saved-hotbar storage {}; using {}",
                    target,
                    original,
                    exception);
            return original;
        }
    }
}
