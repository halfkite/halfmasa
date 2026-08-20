package io.github.halfmasa.xaerobinding.feature;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
//#if MC >= 26.2
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif

public final class ImeOverlay
{
    private static volatile String composition;
    private static volatile int compositionCaret;
    private static volatile String[] candidates;
    private static volatile boolean alphaMode;
    private static volatile long showAlphaUntil;
    private static volatile int caretX;
    private static volatile int caretY;
    private static volatile int[] compositionExtent = new int[] {0, 0, 1, 1};

    private ImeOverlay() {}

    public static void setCaret(int x, int y)
    {
        caretX = x;
        caretY = y;
    }

    public static void setComposition(String value, int caret)
    {
        composition = value == null || value.isEmpty() ? null : value;
        compositionCaret = Math.max(0, caret);
        showAlphaUntil = 0L;
    }

    public static void setCandidates(String[] values)
    {
        candidates = values == null ? null : Arrays.copyOf(values, values.length);
    }

    public static void setAlphaMode(boolean alpha)
    {
        alphaMode = alpha;
        showAlphaUntil = System.currentTimeMillis() + 2200L;
    }

    public static int[] compositionExtent()
    {
        return Arrays.copyOf(compositionExtent, compositionExtent.length);
    }

    public static boolean isComposing()
    {
        return composition != null;
    }

    public static void clear()
    {
        composition = null;
        candidates = null;
        showAlphaUntil = 0L;
    }

    //#if MC >= 26.2
    public static void render(GuiGraphicsExtractor graphics)
    //#else
    //$$ public static void render(GuiGraphics graphics)
    //#endif
    {
        if (!ImeService.getInstance().isNativeActive())
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        String composing = composition;
        String[] currentCandidates = candidates;
        int x = Math.max(0, Math.min(caretX, client.getWindow().getGuiScaledWidth() - 20));
        int y = Math.max(0, Math.min(caretY, client.getWindow().getGuiScaledHeight() - 20));
        int compositionWidth = 2;
        int compositionHeight = client.font.lineHeight + 4;

        if (composing != null)
        {
            compositionWidth = client.font.width(composing) + 8;
            x = Math.min(x, Math.max(0, client.getWindow().getGuiScaledWidth() - compositionWidth));
            graphics.fill(x, y, x + compositionWidth, y + compositionHeight, 0xE0202020);
            int caret = Math.min(compositionCaret, composing.length());
            String left = composing.substring(0, caret);
            String right = composing.substring(caret);
            //#if MC >= 26.2
            graphics.text(client.font, left, x + 3, y + 2, 0xFFFFFFFF, false);
            int caretDrawX = x + 3 + client.font.width(left);
            graphics.text(client.font, right, caretDrawX + 3, y + 2, 0xFFFFFFFF, false);
            //#else
            //$$ graphics.drawString(client.font, left, x + 3, y + 2, 0xFFFFFFFF, false);
            //$$ int caretDrawX = x + 3 + client.font.width(left);
            //$$ graphics.drawString(client.font, right, caretDrawX + 3, y + 2, 0xFFFFFFFF, false);
            //#endif
            if ((System.currentTimeMillis() / 500L & 1L) == 0L)
            {
                graphics.fill(caretDrawX + 1, y + 2, caretDrawX + 2, y + 2 + client.font.lineHeight, 0xFFFFFFFF);
            }
        }

        int candidateY = y + compositionHeight;
        if (currentCandidates != null && currentCandidates.length > 0)
        {
            StringBuilder line = new StringBuilder();
            for (int index = 0; index < currentCandidates.length; index++)
            {
                if (index > 0)
                {
                    line.append("  ");
                }
                line.append(index + 1).append(' ').append(currentCandidates[index]);
            }
            String text = line.toString();
            int width = client.font.width(text) + 8;
            int drawX = Math.min(x, Math.max(0, client.getWindow().getGuiScaledWidth() - width));
            graphics.fill(drawX, candidateY, drawX + width, candidateY + client.font.lineHeight + 6, 0xE0202020);
            //#if MC >= 26.2
            graphics.text(client.font, text, drawX + 4, candidateY + 3, 0xFFFFFFFF, false);
            //#else
            //$$ graphics.drawString(client.font, text, drawX + 4, candidateY + 3, 0xFFFFFFFF, false);
            //#endif
        }

        if (showAlphaUntil > System.currentTimeMillis())
        {
            String mode = alphaMode ? "A" : "中";
            int modeX = Math.max(0, x - 18);
            graphics.fill(modeX, y, modeX + 16, y + 16, 0xE0202020);
            //#if MC >= 26.2
            graphics.text(client.font, mode, modeX + 4, y + 4, 0xFFFFFFFF, false);
            //#else
            //$$ graphics.drawString(client.font, mode, modeX + 4, y + 4, 0xFFFFFFFF, false);
            //#endif
        }

        double scale = client.getWindow().getGuiScale();
        compositionExtent = new int[] {
                (int) (x * scale),
                (int) (y * scale),
                (int) ((x + compositionWidth) * scale),
                (int) ((y + compositionHeight) * scale)
        };
    }
}
