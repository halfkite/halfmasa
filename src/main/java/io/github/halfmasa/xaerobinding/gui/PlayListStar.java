package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.network.chat.Component;

public final class PlayListStar
{
    public static final int GUTTER = 16;
    private PlayListStar() {}

    public static boolean hits(int left, int top, double x, double y)
    {
        return x >= left - GUTTER && x < left && y >= top && y < top + 15;
    }

    //#if MC >= 26.1
    public static void draw(GuiGraphicsExtractor graphics, int left, int top,
    //#else
    //$$ public static void draw(GuiGraphics graphics, int left, int top,
    //#endif
            boolean favorite, int mouseX, int mouseY)
    {
        boolean hovered = hits(left, top, mouseX, mouseY);
        //#if MC >= 26.1
        graphics.text(Minecraft.getInstance().font, favorite ? "★" : "☆",
        //#else
        //$$ graphics.drawString(Minecraft.getInstance().font, favorite ? "★" : "☆",
        //#endif
                left - GUTTER + 3, top + 3,
                favorite ? 0xFFFFD45B : hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
        if (hovered)
        {
            var tooltip = Component.translatable(favorite ? "halfmasa.play_lists.unfavorite" : "halfmasa.play_lists.favorite");
            //#if MC >= 1.21.8
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            //#else
            //$$ graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            //#endif
        }
    }
}
//#endif
