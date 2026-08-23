package io.github.halfmasa.xaerobinding.feature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fi.dy.masa.malilib.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelStorageSource;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.SelectWorldScreenAccessor;
import io.github.halfmasa.xaerobinding.mixin.WorldSelectionListAccessor;

public final class CustomSavesPath
{
    private static Path currentPath;

    private CustomSavesPath()
    {
    }

    public static Path replaceDefault(Path original)
    {
        Path defaultSaves = getDefaultPath();
        if (!normalize(original).equals(defaultSaves))
        {
            return original;
        }

        Path selected = resolveInitialPath();
        if (selected == null)
        {
            currentPath = defaultSaves;
            return original;
        }

        try
        {
            ensureDirectory(selected);
            currentPath = selected;
            XaeroWorldBinding.LOGGER.info("Using saves directory {}", selected);
            return selected;
        }
        catch (IOException | SecurityException exception)
        {
            XaeroWorldBinding.LOGGER.error("Unable to use saves directory {}; using {}", selected, original, exception);
            currentPath = defaultSaves;
            return original;
        }
    }

    public static Path getDefaultPath()
    {
        return normalize(FabricLoader.getInstance().getGameDir().resolve("saves"));
    }

    public static Path getCurrentPath()
    {
        if (currentPath != null)
        {
            return currentPath;
        }

        Path active = resolveConfiguredPath(Configs.CUSTOM_SAVES_ACTIVE_PATH.getStringValue());
        return active != null ? active : getDefaultPath();
    }

    public static List<PathOption> getOptions()
    {
        List<PathOption> options = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        Path defaultPath = getDefaultPath();
        options.add(new PathOption(defaultPath, defaultPath, true, true));
        seen.add(defaultPath);

        List<String> configured = new ArrayList<>(Configs.CUSTOM_SAVES_PATHS.getStrings());
        String legacy = Configs.CUSTOM_SAVES_PATH.getStringValue();
        if (configured.isEmpty() && legacy != null && !legacy.isBlank())
        {
            configured.add(legacy);
        }

        for (String value : configured)
        {
            if (value == null || value.isBlank())
            {
                continue;
            }

            Path path = resolveConfiguredPath(value);
            if (path != null && !seen.add(path))
            {
                continue;
            }
            options.add(new PathOption(path, path, isAvailable(path), false, value));
        }
        return options;
    }

    public static boolean switchTo(Path path)
    {
        Path normalized = normalize(path);
        try
        {
            ensureDirectory(normalized);
            LevelStorageSource source = Minecraft.getInstance().getLevelSource();
            ((LevelStorageSourceAccess) (Object) source).halfmasa$setBaseDir(normalized);
            currentPath = normalized;
            String configured = normalized.equals(getDefaultPath()) ? "" : normalized.toString();
            Configs.CUSTOM_SAVES_ACTIVE_PATH.setValueFromString(configured);
            ConfigManager.getInstance().onConfigsChanged(XaeroWorldBinding.MOD_ID);
            XaeroWorldBinding.LOGGER.info("Switched saves directory to {}", normalized);
            return true;
        }
        catch (IOException | RuntimeException exception)
        {
            XaeroWorldBinding.LOGGER.error("Unable to switch saves directory to {}", normalized, exception);
            return false;
        }
    }

    public static void applyLoadedSelection()
    {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null || client.getSingleplayerServer() != null)
        {
            return;
        }

        Path selected = resolveInitialPath();
        try
        {
            ensureDirectory(selected);
            LevelStorageSource source = client.getLevelSource();
            ((LevelStorageSourceAccess) (Object) source).halfmasa$setBaseDir(selected);
            currentPath = selected;
            XaeroWorldBinding.LOGGER.info("Restored saves directory {}", selected);
        }
        catch (IOException | RuntimeException exception)
        {
            XaeroWorldBinding.LOGGER.error("Unable to restore saves directory {}; using the current directory", selected, exception);
        }
    }

    public static boolean addConfiguredPath(String value)
    {
        if (value == null || value.isBlank())
        {
            return false;
        }

        String configured = value.trim();
        Path path = resolveConfiguredPath(configured);
        if (path == null)
        {
            return false;
        }

        for (PathOption option : getOptions())
        {
            if (option.path() != null && option.path().equals(path))
            {
                return false;
            }
        }

        List<String> paths = new ArrayList<>(Configs.CUSTOM_SAVES_PATHS.getStrings());
        paths.add(configured);
        Configs.CUSTOM_SAVES_PATHS.setStrings(paths);
        ConfigManager.getInstance().onConfigsChanged(XaeroWorldBinding.MOD_ID);
        return true;
    }

    public static boolean editConfiguredPath(PathOption option, String value)
    {
        if (option == null || option.defaultPath() || value == null || value.isBlank())
        {
            return false;
        }

        String configured = value.trim();
        Path replacement = resolveConfiguredPath(configured);
        if (replacement == null)
        {
            return false;
        }

        for (PathOption existing : getOptions())
        {
            if (existing != option && !existing.defaultPath() && existing.path() != null &&
                    existing.path().equals(replacement) && !sameOption(existing, option))
            {
                return false;
            }
        }

        Path target = option.path();
        if (target != null && target.equals(getCurrentPath()) && !switchTo(replacement))
        {
            return false;
        }

        List<String> paths = new ArrayList<>(Configs.CUSTOM_SAVES_PATHS.getStrings());
        int index = paths.indexOf(option.configuredValue());
        if (index < 0 && target != null)
        {
            for (int candidate = 0; candidate < paths.size(); candidate++)
            {
                if (target.equals(resolveConfiguredPath(paths.get(candidate))))
                {
                    index = candidate;
                    break;
                }
            }
        }

        if (index >= 0)
        {
            paths.set(index, configured);
            Configs.CUSTOM_SAVES_PATHS.setStrings(paths);
        }
        else
        {
            String legacy = Configs.CUSTOM_SAVES_PATH.getStringValue();
            if (legacy == null || legacy.isBlank() ||
                    !(legacy.trim().equals(option.configuredValue()) || target != null && target.equals(resolveConfiguredPath(legacy))))
            {
                return false;
            }
            Configs.CUSTOM_SAVES_PATH.setValueFromString(configured);
        }

        ConfigManager.getInstance().onConfigsChanged(XaeroWorldBinding.MOD_ID);
        return true;
    }

    public static boolean removeConfiguredPath(PathOption option)
    {
        if (option == null || option.defaultPath())
        {
            return false;
        }

        Path target = option.path();
        if (target != null && target.equals(getCurrentPath()) && !switchTo(getDefaultPath()))
        {
            return false;
        }

        List<String> paths = new ArrayList<>(Configs.CUSTOM_SAVES_PATHS.getStrings());
        boolean changed = paths.removeIf(value -> {
            Path path = resolveConfiguredPath(value);
            return target == null ? value.trim().equals(option.configuredValue()) : target.equals(path);
        });

        String legacy = Configs.CUSTOM_SAVES_PATH.getStringValue();
        if (legacy != null && !legacy.isBlank())
        {
            Path legacyPath = resolveConfiguredPath(legacy);
            if (legacy.trim().equals(option.configuredValue()) || target != null && target.equals(legacyPath))
            {
                Configs.CUSTOM_SAVES_PATH.setValueFromString("");
                changed = true;
            }
        }

        if (changed)
        {
            Configs.CUSTOM_SAVES_PATHS.setStrings(paths);
            ConfigManager.getInstance().onConfigsChanged(XaeroWorldBinding.MOD_ID);
        }
        return changed;
    }

    public static void refreshWorldList(SelectWorldScreen screen)
    {
        SelectWorldScreenAccessor screenAccessor = (SelectWorldScreenAccessor) (Object) screen;
        WorldSelectionList list = screenAccessor.halfmasa$getWorldList();
        if (list != null)
        {
            ((WorldSelectionListAccessor) (Object) list).halfmasa$reloadWorldList();
        }
        //#if MC >= 1.21.11
        screen.resize(screen.width, screen.height);
        //#else
        //$$ screen.resize(Minecraft.getInstance(), screen.width, screen.height);
        //#endif
    }

    private static Path resolveInitialPath()
    {
        if (Configs.hasPersistedCustomSavesActivePath())
        {
            String active = Configs.CUSTOM_SAVES_ACTIVE_PATH.getStringValue();
            if (active == null || active.isBlank())
            {
                return getDefaultPath();
            }

            Path configured = resolveConfiguredPath(active);
            if (configured != null && isAvailable(configured))
            {
                return configured;
            }
        }

        for (PathOption option : getOptions())
        {
            if (!option.defaultPath() && option.available())
            {
                return option.path();
            }
        }
        return getDefaultPath();
    }

    private static boolean sameOption(PathOption first, PathOption second)
    {
        return first.configuredValue().equals(second.configuredValue()) &&
                (first.path() == null ? second.path() == null : first.path().equals(second.path()));
    }

    private static Path resolveConfiguredPath(String configured)
    {
        if (configured == null || configured.isBlank())
        {
            return null;
        }

        try
        {
            Path path = Path.of(configured.trim());
            if (!path.isAbsolute())
            {
                path = FabricLoader.getInstance().getGameDir().resolve(path);
            }
            return normalize(path);
        }
        catch (InvalidPathException exception)
        {
            return null;
        }
    }

    private static boolean isAvailable(Path path)
    {
        if (path == null)
        {
            return false;
        }
        try
        {
            return !Files.exists(path) || Files.isDirectory(path);
        }
        catch (SecurityException exception)
        {
            return false;
        }
    }

    private static void ensureDirectory(Path path) throws IOException
    {
        Files.createDirectories(path);
        if (!Files.isDirectory(path))
        {
            throw new IOException("Path is not a directory: " + path);
        }
    }

    private static Path normalize(Path path)
    {
        return path.toAbsolutePath().normalize();
    }

    public record PathOption(Path resolvedPath, Path path, boolean available, boolean defaultPath, String configuredValue)
    {
        public PathOption(Path resolvedPath, Path path, boolean available, boolean defaultPath)
        {
            this(resolvedPath, path, available, defaultPath, "");
        }

        public boolean invalid()
        {
            return resolvedPath == null;
        }
    }
}
