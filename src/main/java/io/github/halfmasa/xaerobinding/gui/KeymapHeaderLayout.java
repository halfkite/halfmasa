package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;

/** Keep text-sized controls intact and wrap them in order within the screen margins. */
public final class KeymapHeaderLayout
{
    public record Cell(int x, int y, int width) {}
    public record Layout(List<Cell> cells, int bottom) {}
    private KeymapHeaderLayout() {}
    public static Layout arrange(int screenWidth, int... preferredWidths)
    {
        int available = Math.max(1, screenWidth - 20);
        int x = 10, y = 24;
        List<Cell> cells = new ArrayList<>();
        for (int preferred : preferredWidths)
        {
            int width = Math.min(available, Math.max(1, preferred));
            if (x > 10 && x + width > screenWidth - 10) { x = 10; y += 24; }
            cells.add(new Cell(x, y, width));
            x += width + 4;
        }
        return new Layout(List.copyOf(cells), y + 24);
    }
}
//#endif
