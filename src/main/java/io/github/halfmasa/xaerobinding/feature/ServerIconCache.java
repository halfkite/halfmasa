package io.github.halfmasa.xaerobinding.feature;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.ServerIconMatchMode;

public final class ServerIconCache
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> OBSERVED_HASHES = new HashMap<>();
    private static CacheIndex index;

    private ServerIconCache() {}

    public static boolean requestClear()
    {
        Minecraft client = Minecraft.getInstance();
        Screen parent = MinecraftClientCompat.getScreen(client);
        MinecraftClientCompat.setScreen(client, new ConfirmScreen(confirmed -> {
            if (confirmed)
            {
                clear();
            }
            MinecraftClientCompat.setScreen(client, parent);
        }, Component.translatable("halfmasa.feature.server_icon_cache.clear_confirm_title"),
                Component.translatable("halfmasa.feature.server_icon_cache.clear_confirm")));
        return true;
    }

    public static synchronized void synchronize(ServerData server)
    {
        if (!Configs.SERVER_ICON_CACHE.getBooleanValue())
        {
            return;
        }

        byte[] icon = server.getIconBytes();
        if (icon == null)
        {
            restore(server);
        }
        else
        {
            store(server, icon);
        }
    }

    public static synchronized boolean clear()
    {
        try
        {
            Path directory = cacheDirectory();
            if (Files.isDirectory(directory))
            {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(directory))
                {
                    for (Path file : files)
                    {
                        if (Files.isRegularFile(file))
                        {
                            Files.deleteIfExists(file);
                        }
                    }
                }
            }
            Files.deleteIfExists(indexFile());
            index = new CacheIndex();
            OBSERVED_HASHES.clear();
            message("halfmasa.feature.server_icon_cache.cleared");
            return true;
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.error("Failed to clear the server icon cache", exception);
            message("halfmasa.feature.server_icon_cache.clear_failed");
            return false;
        }
    }

    private static void restore(ServerData server)
    {
        CacheEntry match = loadIndex().entries.stream()
                .filter(entry -> matches(entry, server))
                .max(Comparator.comparingLong(entry -> entry.updatedAt))
                .orElse(null);
        if (match == null)
        {
            return;
        }

        try
        {
            byte[] bytes = Files.readAllBytes(cacheDirectory().resolve(match.file));
            byte[] valid = ServerData.validateIcon(bytes);
            server.setIconBytes(valid);
            OBSERVED_HASHES.put(identity(server), Arrays.hashCode(valid));
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Ignoring a damaged cached server icon {}", match.file);
            loadIndex().entries.remove(match);
            saveIndex();
        }
    }

    private static void store(ServerData server, byte[] bytes)
    {
        String identity = identity(server);
        int contentHash = Arrays.hashCode(bytes);
        if (OBSERVED_HASHES.getOrDefault(identity, Integer.MIN_VALUE) == contentHash)
        {
            return;
        }

        try
        {
            byte[] valid = ServerData.validateIcon(bytes);
            String normalizedName = normalizeName(server.name);
            String normalizedIp = normalizeAddress(server.ip);
            String fileName = digest(normalizedName + "\u0000" + normalizedIp) + ".png";

            Files.createDirectories(cacheDirectory());
            Path temporary = cacheDirectory().resolve(fileName + ".tmp");
            Files.write(temporary, valid);
            Files.move(temporary, cacheDirectory().resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            CacheIndex current = loadIndex();
            current.entries.removeIf(entry ->
                    entry.normalizedName.equals(normalizedName) && entry.normalizedIp.equals(normalizedIp));
            current.entries.add(new CacheEntry(
                    server.name,
                    server.ip,
                    normalizedName,
                    normalizedIp,
                    fileName,
                    System.currentTimeMillis()));
            prune(current);
            saveIndex();
            OBSERVED_HASHES.put(identity, contentHash);
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to cache the server icon for {}", server.ip, exception);
        }
    }

    private static boolean matches(CacheEntry entry, ServerData server)
    {
        String name = normalizeName(server.name);
        String ip = normalizeAddress(server.ip);
        ServerIconMatchMode mode = (ServerIconMatchMode) Configs.SERVER_ICON_MATCH_MODE.getOptionListValue();
        return switch (mode)
        {
            case NAME_AND_IP -> entry.normalizedName.equals(name) && entry.normalizedIp.equals(ip);
            case NAME_ONLY -> entry.normalizedName.equals(name);
            case IP_ONLY -> entry.normalizedIp.equals(ip);
        };
    }

    private static void prune(CacheIndex current)
    {
        int limit = Configs.SERVER_ICON_CACHE_LIMIT.getIntegerValue();
        current.entries.sort(Comparator.comparingLong(entry -> -entry.updatedAt));
        while (current.entries.size() > limit)
        {
            CacheEntry removed = current.entries.remove(current.entries.size() - 1);
            if (current.entries.stream().noneMatch(entry -> entry.file.equals(removed.file)))
            {
                try
                {
                    Files.deleteIfExists(cacheDirectory().resolve(removed.file));
                }
                catch (Exception ignored)
                {
                }
            }
        }
    }

    private static CacheIndex loadIndex()
    {
        if (index != null)
        {
            return index;
        }

        Path file = indexFile();
        if (Files.isReadable(file))
        {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
            {
                index = GSON.fromJson(reader, CacheIndex.class);
            }
            catch (Exception exception)
            {
                XaeroWorldBinding.LOGGER.warn("Failed to read the server icon cache index", exception);
            }
        }
        if (index == null || index.entries == null)
        {
            index = new CacheIndex();
        }
        return index;
    }

    private static void saveIndex()
    {
        try
        {
            Files.createDirectories(indexFile().getParent());
            Path temporary = indexFile().resolveSibling("index.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                GSON.toJson(loadIndex(), writer);
            }
            Files.move(temporary, indexFile(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to save the server icon cache index", exception);
        }
    }

    private static String normalizeName(String name)
    {
        return name == null ? "" : name.trim();
    }

    private static String normalizeAddress(String value)
    {
        ServerAddress address = ServerAddress.parseString(value == null ? "" : value.trim());
        return address.getHost().toLowerCase(Locale.ROOT) + ":" + address.getPort();
    }

    private static String identity(ServerData server)
    {
        return normalizeName(server.name) + "\u0000" + normalizeAddress(server.ip);
    }

    private static String digest(String value) throws Exception
    {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static Path rootDirectory()
    {
        return Configs.getHalfMasaDirectory().resolve("server-icons");
    }

    private static Path cacheDirectory()
    {
        return rootDirectory().resolve("icons");
    }

    private static Path indexFile()
    {
        return rootDirectory().resolve("index.json");
    }

    private static void message(String translationKey)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null)
        {
            //#if MC >= 26.1
            client.player.sendSystemMessage(Component.translatable(translationKey));
            //#else
            //$$ client.player.displayClientMessage(Component.translatable(translationKey), false);
            //#endif
        }
    }

    private static final class CacheIndex
    {
        private List<CacheEntry> entries = new ArrayList<>();
    }

    private static final class CacheEntry
    {
        private String name;
        private String ip;
        private String normalizedName;
        private String normalizedIp;
        private String file;
        private long updatedAt;

        private CacheEntry() {}

        private CacheEntry(
                String name,
                String ip,
                String normalizedName,
                String normalizedIp,
                String file,
                long updatedAt)
        {
            this.name = name;
            this.ip = ip;
            this.normalizedName = normalizedName;
            this.normalizedIp = normalizedIp;
            this.file = file;
            this.updatedAt = updatedAt;
        }
    }
}
