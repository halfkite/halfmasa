package io.github.halfmasa.xaerobinding.waypoint;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.InfoUtils;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class WaypointChatSender implements IClientTickHandler
{
    private static final WaypointChatSender INSTANCE = new WaypointChatSender();
    private final Queue<String> pending = new ArrayDeque<>();
    private int cooldown;

    private WaypointChatSender()
    {
    }

    public static WaypointChatSender getInstance()
    {
        return INSTANCE;
    }

    public void enqueue(boolean allSets) throws ReflectiveOperationException
    {
        List<String> messages = WaypointBundleService.createChatShareMessages(allSets);
        this.pending.addAll(messages);
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "halfmasa.message.chat_queued", messages.size());
    }

    @Override
    public void onClientTick(Minecraft minecraft)
    {
        if (minecraft.player == null)
        {
            this.pending.clear();
            this.cooldown = 0;
            return;
        }
        if (this.pending.isEmpty())
        {
            return;
        }
        if (this.cooldown-- > 0)
        {
            return;
        }

        String message = this.pending.remove();
        //#if MC >= 26.2
        minecraft.gui.hud.getChat().addRecentChat(message);
        //#else
        //$$ minecraft.gui.getChat().addRecentChat(message);
        //#endif
        minecraft.player.connection.sendChat(message);
        this.cooldown = Configs.CHAT_SEND_INTERVAL.getIntegerValue();
    }
}
