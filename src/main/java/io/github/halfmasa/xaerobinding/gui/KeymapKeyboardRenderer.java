package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.Font;

public final class KeymapKeyboardRenderer
{
    public static final int MARKER_SIZE = 3;
    private KeymapKeyboardRenderer() {}

    //#if MC >= 1.21.11
    public static void drawKey(GuiContext graphics, Font font, KeymapKeyboardLayout.Key key,
    //#else
    //$$ public static void drawKey(GuiGraphics graphics, Font font, KeymapKeyboardLayout.Key key,
    //#endif
            KeymapKeyboardStyle.Indicators indicators, boolean selected, boolean hovered)
    {
        int left = key.x();
        int top = key.y();
        int right = left + key.width();
        int bottom = top + key.height();
        graphics.fill(left, top, right, bottom, indicators.fill());
        int border = selected ? KeymapKeyboardStyle.SELECTED_BORDER : hovered ? 0xFFE4E4ED : 0xFF59616E;
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        String label = key.mouse() ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-key.code() - 1))
                : key.code() == io.github.halfmasa.xaerobinding.compat.InputCompat.layoutKeyCode(32) ? StringUtils.translate("key.keyboard.space") : key.label();
        int textWidth = font.width(label);
        // Always reserve the same two marker columns so labels and dots never move with status.
        int labelRight = right - 9;
        float scale = Math.min(1.0F, Math.max(1, labelRight - left - 4) / (float) Math.max(1, textWidth));
        //#if MC >= 1.21.8
        graphics.pose().pushMatrix();
        //#else
        //$$ graphics.pose().pushPose();
        //#endif
        //#if MC >= 1.21.8
        graphics.pose().translate((left + labelRight) / 2.0F, top + key.height() / 2.0F);
        //#else
        //$$ graphics.pose().translate((left + labelRight) / 2.0F, top + key.height() / 2.0F, 0.0F);
        //#endif
        //#if MC >= 1.21.8
        graphics.pose().scale(scale, scale);
        //#else
        //$$ graphics.pose().scale(scale, scale, 1.0F);
        //#endif
        graphics.drawString(font, label, -textWidth / 2, -4, 0xFFF0F0F0, false);
        //#if MC >= 1.21.8
        graphics.pose().popMatrix();
        //#else
        //$$ graphics.pose().popPose();
        //#endif
        // Six fixed slots, all 3x3. Missing statuses leave their slots empty.
        if (indicators.vanilla()) marker(graphics, right, top, 0, 0, KeymapKeyboardStyle.VANILLA_DOT);
        if (indicators.malilib()) marker(graphics, right, top, 0, 1, KeymapKeyboardStyle.MALILIB_DOT);
        if (indicators.combination()) marker(graphics, right, top, 0, 2, KeymapKeyboardStyle.COMBINATION_DOT);
        if (indicators.singleConflict()) marker(graphics, right, top, 1, 0, KeymapKeyboardStyle.SINGLE_CONFLICT);
        if (indicators.combinationConflict()) marker(graphics, right, top, 1, 1, KeymapKeyboardStyle.COMBINATION_CONFLICT);
        if (indicators.ignored()) marker(graphics, right, top, 1, 2, KeymapKeyboardStyle.IGNORED_DOT);
    }

    //#if MC >= 1.21.11
    private static void marker(GuiContext graphics, int right, int top, int column, int row, int color)
    //#else
    //$$ private static void marker(GuiGraphics graphics, int right, int top, int column, int row, int color)
    //#endif
    {
        drawMarker(graphics, right - 1 - MARKER_SIZE - column * (MARKER_SIZE + 1),
                top + 2 + row * (MARKER_SIZE + 1), color);
    }

    //#if MC >= 1.21.11
    public static void drawMarker(GuiContext graphics, int x, int y, int color)
    //#else
    //$$ public static void drawMarker(GuiGraphics graphics, int x, int y, int color)
    //#endif
    {
        graphics.fill(x, y, x + MARKER_SIZE, y + MARKER_SIZE, color);
    }
}
//#endif
