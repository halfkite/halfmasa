package io.github.halfmasa.xaerobinding.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class MinecraftClientCompat
{
    private MinecraftClientCompat() {}

    public static void setOverlayMessage(Minecraft client, net.minecraft.network.chat.Component message, boolean animate)
    {
        //#if MC >= 26.2
        client.gui.hud.setOverlayMessage(message, animate);
        //#else
        //$$ client.gui.setOverlayMessage(message, animate);
        //#endif
    }

    public static Screen getScreen(Minecraft client)
    {
        //#if MC >= 26.2
        return client.gui.screen();
        //#else
        //$$ return client.screen;
        //#endif
    }

    public static void setScreen(Minecraft client, Screen screen)
    {
        //#if MC >= 26.2
        client.setScreenAndShow(screen);
        //#else
        //$$ client.setScreen(screen);
        //#endif
    }

    public static void clearOverlay(Minecraft client)
    {
        //#if MC >= 26.2
        client.gui.setOverlay(null);
        //#else
        //$$ client.setOverlay(null);
        //#endif
    }

    public static Camera getMainCamera(Minecraft client)
    {
        //#if MC >= 26.2
        return client.gameRenderer.mainCamera();
        //#else
        //$$ return client.gameRenderer.getMainCamera();
        //#endif
    }

    public static void reloadLevelRenderer(Minecraft client)
    {
        //#if MC >= 26.2
        client.levelExtractor.allChanged();
        //#else
        //$$ client.levelRenderer.allChanged();
        //#endif
    }
}
