package io.github.halfmasa.xaerobinding.binding;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class WorldBindingStore
{
    public static final String FILE_NAME = "xaero-world-binding.json";
    private static final String LEGACY_FILE_NAME = ".halfmasa-xaero-binding.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Path, BindingFile> CACHE = new HashMap<>();

    private WorldBindingStore()
    {
    }

    public static String resolveMinimapRoot(String original)
    {
        return resolve(original, file -> file.minimapRootId, (file, value) -> file.minimapRootId = value);
    }

    public static String resolveWorldMapRoot(String original)
    {
        return resolve(original, file -> file.worldMapRootId, (file, value) -> file.worldMapRootId = value);
    }

    private static synchronized String resolve(
            String original,
            Function<BindingFile, String> getter,
            BindingSetter setter)
    {
        if (!Configs.ENABLE_WORLD_BINDING.getBooleanValue())
        {
            return original;
        }

        Path worldRoot = getSingleplayerWorldRoot();
        if (worldRoot == null || original == null || original.isBlank())
        {
            return original;
        }

        try
        {
            BindingFile binding = CACHE.computeIfAbsent(worldRoot, WorldBindingStore::readUnchecked);
            String stored = getter.apply(binding);
            if (stored != null && !stored.isBlank())
            {
                return stored;
            }

            setter.set(binding, original);
            save(worldRoot, binding);
            return original;
        }
        catch (RuntimeException | IOException exception)
        {
            XaeroWorldBinding.LOGGER.error("Failed to read or write {} in {}", FILE_NAME, worldRoot, exception);
            return original;
        }
    }

    public static Path getSingleplayerWorldRoot()
    {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    private static BindingFile readUnchecked(Path worldRoot)
    {
        try
        {
            Path file = bindingFile(worldRoot);
            Path legacyFile = worldRoot.resolve(LEGACY_FILE_NAME);
            boolean migrateLegacyFile = !Files.exists(file) && Files.exists(legacyFile);
            Path source = migrateLegacyFile ? legacyFile : file;
            if (!Files.exists(source))
            {
                return new BindingFile();
            }

            BindingFile binding;
            try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8))
            {
                binding = GSON.fromJson(reader, BindingFile.class);
                if (binding == null || binding.schema != 1)
                {
                    throw new IOException("Unsupported or empty binding file");
                }
            }

            if (migrateLegacyFile)
            {
                migrateLegacyFile(worldRoot, legacyFile, binding);
            }
            return binding;
        }
        catch (IOException exception)
        {
            throw new IllegalStateException(exception);
        }
    }

    private static void save(Path worldRoot, BindingFile binding) throws IOException
    {
        Path file = bindingFile(worldRoot);
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(FILE_NAME + ".tmp");

        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
        {
            GSON.toJson(binding, writer);
        }

        try
        {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ignored)
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path bindingFile(Path worldRoot)
    {
        return worldRoot.resolve("config").resolve("halfmasa").resolve(FILE_NAME);
    }

    private static void migrateLegacyFile(Path worldRoot, Path legacyFile, BindingFile binding)
    {
        try
        {
            save(worldRoot, binding);
            Files.deleteIfExists(legacyFile);
            XaeroWorldBinding.LOGGER.info("Migrated {} to {}", legacyFile, bindingFile(worldRoot));
        }
        catch (IOException exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to migrate legacy world binding file {}", legacyFile, exception);
        }
    }

    private interface BindingSetter
    {
        void set(BindingFile file, String value);
    }

    private static final class BindingFile
    {
        int schema = 1;
        String minimapRootId;
        String worldMapRootId;
    }
}
