package io.github.halfmasa.xaerobinding.waypoint;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.waypoint.WaypointBundleService.ExportScope;

public final class WaypointClientActions
{
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final WaypointOperationHistory HISTORY = new WaypointOperationHistory();

    private WaypointClientActions()
    {
    }

    public static boolean exportToClipboard(ExportScope scope)
    {
        return run(() -> {
            WaypointBundleService.ExportResult result = WaypointBundleService.export(scope);
            setClipboard(result.text());
            success("halfmasa.message.waypoint_exported_text",
                    result.waypointCount(), result.setCount(), result.dimensionCount());
        });
    }

    public static boolean exportToFile(ExportScope scope)
    {
        return run(() -> {
            WaypointBundleService.ExportResult result = WaypointBundleService.export(scope);
            String fileName = "halfmasa-xaero-" + FILE_TIME.format(LocalDateTime.now()) + ".txt";
            Path defaultPath = Minecraft.getInstance().gameDirectory.toPath().resolve(fileName).toAbsolutePath();
            String selected;
            try (MemoryStack stack = MemoryStack.stackPush())
            {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.txt")).flip();
                selected = TinyFileDialogs.tinyfd_saveFileDialog(
                        StringUtils.translate("halfmasa.dialog.waypoint_export_title"),
                        defaultPath.toString(),
                        filters,
                        StringUtils.translate("halfmasa.dialog.waypoint_text_files"));
            }
            if (selected == null)
            {
                return;
            }

            Path output = ensureTxtExtension(Path.of(selected).toAbsolutePath().normalize());
            if (Files.exists(output) && !confirmOverwrite(output))
            {
                return;
            }

            Path parent = output.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            Files.writeString(output, result.text(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            setClipboard(result.text());
            success("halfmasa.message.waypoint_exported_file", output,
                    result.waypointCount(), result.setCount(), result.dimensionCount());
        });
    }

    private static boolean confirmOverwrite(Path output)
    {
        //#if MC >= 26.1
        return TinyFileDialogs.tinyfd_messageBox(
                StringUtils.translate("halfmasa.dialog.waypoint_overwrite_title"),
                StringUtils.translate("halfmasa.dialog.waypoint_overwrite", output),
                "yesno",
                "warning",
                0) != 0;
        //#else
        //$$ return TinyFileDialogs.tinyfd_messageBox(
        //$$         StringUtils.translate("halfmasa.dialog.waypoint_overwrite_title"),
        //$$         StringUtils.translate("halfmasa.dialog.waypoint_overwrite", output),
        //$$         "yesno",
        //$$         "warning",
        //$$         false);
        //#endif
    }

    public static boolean importBundle()
    {
        return run(() -> {
            ImportSource source = readImportSource();
            WaypointBundleService.ImportResult result = tracked(
                    "halfmasa.history.waypoint_import",
                    () -> WaypointBundleService.importIntoCurrentWorld(source.text()));
            success("halfmasa.message.waypoint_imported", result.importedCount(),
                    result.duplicateCount(), result.dimensionCount(), source.description());
        });
    }

    public static boolean dedupeCurrentSet()
    {
        return run(() -> success(
                "halfmasa.message.deduped",
                tracked("halfmasa.history.waypoint_dedupe_current",
                        WaypointBundleService::removeDuplicatesFromCurrentSet)));
    }

    public static boolean dedupeAll()
    {
        return run(() -> success(
                "halfmasa.message.deduped",
                tracked("halfmasa.history.waypoint_dedupe_all",
                        WaypointBundleService::removeDuplicatesFromAllSets)));
    }

    public static boolean undo()
    {
        return run(() -> {
            WaypointOperationHistory.HistoryResult result = HISTORY.undo();
            success("halfmasa.message.waypoint_undone",
                    StringUtils.translate(result.operationTranslationKey()), result.undoSteps());
        });
    }

    public static boolean redo()
    {
        return run(() -> {
            WaypointOperationHistory.HistoryResult result = HISTORY.redo();
            success("halfmasa.message.waypoint_redone",
                    StringUtils.translate(result.operationTranslationKey()), result.redoSteps());
        });
    }

    private static ImportSource readImportSource() throws Exception
    {
        try
        {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
            {
                Object data = contents.getTransferData(DataFlavor.javaFileListFlavor);
                if (data instanceof List<?> files && !files.isEmpty() && files.get(0) instanceof File file)
                {
                    Path path = file.toPath().toAbsolutePath().normalize();
                    if (!Files.isRegularFile(path))
                    {
                        throw new IllegalArgumentException("halfmasa.error.waypoint_file_invalid");
                    }
                    return new ImportSource(Files.readString(path, StandardCharsets.UTF_8), path.toString());
                }
            }
        }
        catch (HeadlessException ignored)
        {
        }

        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank())
        {
            throw new IllegalArgumentException("halfmasa.error.waypoint_clipboard_empty");
        }
        return new ImportSource(clipboard, StringUtils.translate("halfmasa.import_source.clipboard"));
    }

    private static Path ensureTxtExtension(Path path)
    {
        String name = path.getFileName().toString();
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")
                ? path
                : path.resolveSibling(name + ".txt");
    }

    private static void setClipboard(String text)
    {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    private static <T> T tracked(String operationTranslationKey, ThrowingSupplier<T> action) throws Exception
    {
        WaypointBundleService.Snapshot before = WaypointBundleService.captureSnapshot();
        try
        {
            T result = action.get();
            WaypointBundleService.Snapshot after = WaypointBundleService.captureSnapshot();
            HISTORY.record(operationTranslationKey, before, after);
            return result;
        }
        catch (Exception exception)
        {
            try
            {
                WaypointBundleService.Snapshot current = WaypointBundleService.captureSnapshot();
                if (!WaypointBundleService.sameState(before, current))
                {
                    WaypointBundleService.restoreSnapshot(before);
                }
            }
            catch (Exception rollbackException)
            {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    private static boolean run(ThrowingAction action)
    {
        try
        {
            action.run();
            return true;
        }
        catch (Exception exception)
        {
            String message = exception.getMessage();
            if (message != null && message.startsWith("halfmasa."))
            {
                message = StringUtils.translate(message);
            }
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR,
                    message == null || message.isBlank()
                            ? StringUtils.translate("halfmasa.error.waypoint_operation_failed")
                            : message);
            return false;
        }
    }

    private static void success(String key, Object... args)
    {
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, key, args);
    }

    private record ImportSource(String text, String description) {}

    private interface ThrowingAction
    {
        void run() throws Exception;
    }

    private interface ThrowingSupplier<T>
    {
        T get() throws Exception;
    }
}
