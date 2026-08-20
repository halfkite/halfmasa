package io.github.halfmasa.xaerobinding.waypoint;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

public final class WaypointClientActions
{
    private WaypointClientActions()
    {
    }

    public static boolean copyBundle()
    {
        return run(() -> {
            WaypointBundleService.ExportResult result = WaypointBundleService.exportCurrentWorld();
            Minecraft.getInstance().keyboardHandler.setClipboard(result.text());
            success("halfmasa.message.copied", result.waypointCount(), result.setCount());
        });
    }

    public static boolean importBundle()
    {
        return run(() -> {
            WaypointBundleService.ImportResult result = WaypointBundleService.importIntoCurrentWorld(
                    Minecraft.getInstance().keyboardHandler.getClipboard());
            success("halfmasa.message.imported", result.importedCount(), result.duplicateCount());
        });
    }

    public static boolean shareCurrentSet()
    {
        return run(() -> WaypointChatSender.getInstance().enqueue(false));
    }

    public static boolean shareAll()
    {
        return run(() -> WaypointChatSender.getInstance().enqueue(true));
    }

    public static boolean dedupeCurrentSet()
    {
        return run(() -> success(
                "halfmasa.message.deduped",
                WaypointBundleService.removeDuplicatesFromCurrentSet()));
    }

    public static boolean dedupeAll()
    {
        return run(() -> success(
                "halfmasa.message.deduped",
                WaypointBundleService.removeDuplicatesFromAllSets()));
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
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, exception.getMessage());
            return false;
        }
    }

    private static void success(String key, Object... args)
    {
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, key, args);
    }

    private interface ThrowingAction
    {
        void run() throws Exception;
    }
}
