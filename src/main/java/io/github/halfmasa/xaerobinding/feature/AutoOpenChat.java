//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.feature;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.platform.Window;
import io.github.halfmasa.xaerobinding.mixin.AutoOpenChatMouseAccessor;
//#if MC >= 26.3
import org.lwjgl.sdl.SDLMouse;
//#else
//$$ import org.lwjgl.glfw.GLFW;
//#endif

/** Reopens the native chat while enabled, without replacing any existing screen. */
public final class AutoOpenChat implements IClientTickHandler, IWorldLoadListener
{
    private static final AutoOpenChat INSTANCE = new AutoOpenChat();
    private Pointer pointer;

    private record Pointer(double x, double y, Level level, Object connection, long window) {}

    private AutoOpenChat() {}

    public static AutoOpenChat getInstance() { return INSTANCE; }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!Configs.AUTO_OPEN_CHAT.getBooleanValue() || !Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.getBooleanValue()
                || pointer != null && (pointer.level != client.level || pointer.connection != client.getConnection()))
        {
            clearPointer();
        }
        if (Configs.AUTO_OPEN_CHAT.getBooleanValue() && client.level != null && client.player != null
                && client.player.isAlive() && client.getConnection() != null
            && MinecraftClientCompat.getScreen(client) == null &&
            //#if MC >= 26.2
            client.gui.overlay() == null)
            //#else
            //$$ client.getOverlay() == null)
            //#endif
        {
            //#if MC >= 26.2
            client.gui.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
            //#elseif MC >= 1.21.10
            //$$ client.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
            //#else
            //$$ MinecraftClientCompat.setScreen(client, new ChatScreen(""));
            //#endif
            if (MinecraftClientCompat.getScreen(client) instanceof ChatScreen) restorePointer(client);
        }
    }

    public void clearPointer() { pointer = null; }

    @Override
    public void onWorldLoadPost(ClientLevel before, ClientLevel after, Minecraft client)
    {
        // Unloading can finish before the next tick, so release the world reference here.
        if (before != after) clearPointer();
    }

    public void rememberPointer(ChatScreen screen)
    {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.AUTO_OPEN_CHAT.getBooleanValue() || !Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.getBooleanValue())
        {
            clearPointer();
            return;
        }
        if (client.level == null || client.getConnection() == null || MinecraftClientCompat.getScreen(client) != screen
                || client.mouseHandler.isMouseGrabbed()) return;
        double x = client.mouseHandler.xpos(), y = client.mouseHandler.ypos();
        if (Double.isFinite(x) && Double.isFinite(y))
            pointer = new Pointer(x, y, client.level, client.getConnection(), windowHandle(client.getWindow()));
    }

    private void restorePointer(Minecraft client)
    {
        Pointer saved = pointer;
        clearPointer(); // Restore once at opening, never during subsequent mouse movement.
        var window = client.getWindow();
        if (saved == null || !Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.getBooleanValue()
                || saved.level != client.level || saved.connection != client.getConnection()
                || saved.window != windowHandle(window) || !client.isWindowActive() || client.mouseHandler.isMouseGrabbed()) return;
        // MouseHandler uses window coordinates, not scaled GUI or framebuffer coordinates.
        float x = (float) Math.clamp(saved.x, 0, Math.max(0, window.getScreenWidth() - 1));
        float y = (float) Math.clamp(saved.y, 0, Math.max(0, window.getScreenHeight() - 1));
        //#if MC >= 26.3
        SDLMouse.SDL_WarpMouseInWindow(window.handle(), x, y);
        client.mouseHandler.resyncMousePosition();
        //#else
        //$$ GLFW.glfwSetCursorPos(windowHandle(window), x, y);
        //$$ ((AutoOpenChatMouseAccessor) client.mouseHandler).halfmasa$setX(x);
        //$$ ((AutoOpenChatMouseAccessor) client.mouseHandler).halfmasa$setY(y);
        //#endif
        client.mouseHandler.setIgnoreFirstMove();
    }

    private static long windowHandle(Window window)
    {
        //#if MC >= 1.21.10
        return window.handle();
        //#else
        //$$ return window.getWindow();
        //#endif
    }

    public void onIntentionalClose(ChatScreen screen)
    {
        // Command suggestions can consume Esc without closing chat. Only a completed close stops us.
        if (MinecraftClientCompat.getScreen(Minecraft.getInstance()) != screen) stop();
    }

    public void onSubmitted(ChatScreen screen, String message)
    {
        if (!Configs.AUTO_OPEN_CHAT.getBooleanValue()) return;
        String sent = screen.normalizeChatMessage(message);
        if (sent.isEmpty()) return;
        for (String configured : Configs.AUTO_OPEN_CHAT_STOP_MESSAGES.getStrings())
        {
            if (configured != null && !configured.isBlank()
                    && sent.equals(screen.normalizeChatMessage(configured)))
            {
                stop();
                return;
            }
        }
    }

    private void stop()
    {
        clearPointer();
        if (!Configs.AUTO_OPEN_CHAT.getBooleanValue()) return;
        Configs.AUTO_OPEN_CHAT.setBooleanValue(false);
        new Configs().save();
    }
}
//#endif
