package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.Font;

public final class KeymapKeyboardRenderer
{
    public static final int MARKER_SIZE = 3;
    private KeymapKeyboardRenderer() {}

    public static void drawKey(GuiContext graphics, Font font, KeymapKeyboardLayout.Key key,
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
                : key.code() == 44 ? StringUtils.translate("key.keyboard.space") : key.label();
        int textWidth = font.width(label);
        // Always reserve the same two marker columns so labels and dots never move with status.
        int labelRight = right - 9;
        float scale = Math.min(1.0F, Math.max(1, labelRight - left - 4) / (float) Math.max(1, textWidth));
        graphics.pose().pushMatrix();
        graphics.pose().translate((left + labelRight) / 2.0F, top + key.height() / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, label, -textWidth / 2, -4, 0xFFF0F0F0, false);
        graphics.pose().popMatrix();
        // Six fixed slots, all 3x3. Missing statuses leave their slots empty.
        if (indicators.vanilla()) marker(graphics, right, top, 0, 0, KeymapKeyboardStyle.VANILLA_DOT);
        if (indicators.malilib()) marker(graphics, right, top, 0, 1, KeymapKeyboardStyle.MALILIB_DOT);
        if (indicators.combination()) marker(graphics, right, top, 0, 2, KeymapKeyboardStyle.COMBINATION_DOT);
        if (indicators.singleConflict()) marker(graphics, right, top, 1, 0, KeymapKeyboardStyle.SINGLE_CONFLICT);
        if (indicators.combinationConflict()) marker(graphics, right, top, 1, 1, KeymapKeyboardStyle.COMBINATION_CONFLICT);
        if (indicators.ignored()) marker(graphics, right, top, 1, 2, KeymapKeyboardStyle.IGNORED_DOT);
    }

    private static void marker(GuiContext graphics, int right, int top, int column, int row, int color)
    {
        drawMarker(graphics, right - 1 - MARKER_SIZE - column * (MARKER_SIZE + 1),
                top + 2 + row * (MARKER_SIZE + 1), color);
    }

    public static void drawMarker(GuiContext graphics, int x, int y, int color)
    {
        graphics.fill(x, y, x + MARKER_SIZE, y + MARKER_SIZE, color);
    }
}
//#endif
