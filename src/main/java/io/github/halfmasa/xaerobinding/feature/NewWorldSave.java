package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.util.FileUtil;
//#if MC >= 26.1
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings;
//#else
//$$ import net.minecraft.world.level.storage.WorldData;
//#endif
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;

public final class NewWorldSave
{
    private NewWorldSave() {}

    public static LevelStorageAccess prepare(LevelStorageSource storage, String folderName, Path packs) throws IOException
    {
        Path directory = reserveDirectory(storage.getBaseDir(), folderName);
        LevelStorageAccess access = null;
        try
        {
            access = storage.createAccess(directory.getFileName().toString());
            if (packs != null) copyPacks(packs, access.getLevelPath(LevelResource.DATAPACK_DIR));
            return access;
        }
        catch (IOException | RuntimeException failure)
        {
            if (access != null) discard(access, failure);
            else
            {
                try { Files.deleteIfExists(directory); }
                catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    private static Path reserveDirectory(Path root, String name) throws IOException
    {
        // The vanilla name finder checks existence, but a second creator can claim that name before we do.
        for (int attempt = 0; attempt < 8; attempt++)
        {
            Path candidate = root.resolve(FileUtil.findAvailableName(root, name, ""));
            try { return Files.createDirectory(candidate); }
            catch (FileAlreadyExistsException conflict) { name = candidate.getFileName().toString(); }
        }
        throw new IOException("Could not reserve an unused world folder in " + root);
    }

    private static void copyPacks(Path source, Path target) throws IOException
    {
        try (var paths = Files.walk(source))
        {
            for (Path file : (Iterable<Path>) paths::iterator)
            {
                Path destination = target.resolve(source.relativize(file));
                if (Files.isSymbolicLink(file)) throw new IOException("Unexpected symbolic link in world data packs: " + file);
                if (Files.isDirectory(file)) Files.createDirectories(destination);
                else Files.copy(file, destination);
            }
        }
    }

    //#if MC >= 26.1
    public static void saveForLater(LevelStorageAccess access, LayeredRegistryAccess<RegistryLayer> registries,
            WorldDataAndGenSettings settings, Optional<GameRules> rules) throws IOException
    //#else
    //$$ public static void saveForLater(LevelStorageAccess access, LayeredRegistryAccess<RegistryLayer> registries,
    //$$         WorldData settings) throws IOException
    //#endif
    {
        try
        {
            //#if MC >= 26.1
            Path root = access.getLevelPath(LevelResource.ROOT);
            LevelStorageSource.writeWorldGenSettings(registries.compositeAccess(), root, settings.genSettings());
            if (rules.isPresent()) LevelStorageSource.writeGameRules(settings.data(), root, rules.get());
            // Publish level.dat last so the world list never sees an incomplete save.
            access.saveDataTag(settings.data());
            access.getUnfixedDataTagWithFallback();
            //#else
            //$$ access.saveDataTag(registries.compositeAccess(), settings);
            //$$ access.getDataTag();
            //#endif
        }
        catch (IOException | RuntimeException failure)
        {
            discard(access, failure);
            throw failure;
        }
        access.close();
    }

    private static void discard(LevelStorageAccess access, Throwable failure)
    {
        try { access.deleteLevel(); }
        catch (IOException | RuntimeException cleanup) { failure.addSuppressed(cleanup); }
        finally { access.safeClose(); }
    }
}
//#endif
