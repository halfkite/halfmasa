package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;

/** One geometry for rendering, hit testing and scrolling across both waypoint sources. */
public record WaypointGridLayout(int columns, int visibleRows, int maxScroll, int scroll,
                                 List<Cell> cells, int left, int top, int width, int height)
{
    public record Cell(int index, int x, int y, int width, int height)
    {
        public boolean contains(double px, double py)
        { return px >= x && px < x + width && py >= y && py < y + height; }
    }

    public static WaypointGridLayout create(int count, int left, int top, int width, int height,
                                            boolean compact, int fontHeight, int requestedScroll)
    {
        count = Math.max(0, count); width = Math.max(0, width); height = Math.max(0, height);
        int gap = 4;
        int columns = compact ? Math.max(1, (width + gap) / (148 + gap)) : 1;
        int rowHeight = Math.max(compact ? 22 : 30, fontHeight + (compact ? 8 : fontHeight + 10));
        int stride = rowHeight + gap;
        int visible = width == 0 ? 0 : (height + gap) / stride;
        int totalRows = (int)(((long)count + columns - 1) / columns);
        int maxScroll = Math.max(0, totalRows - visible);
        int scroll = Math.max(0, Math.min(requestedScroll, maxScroll));
        List<Cell> cells = new ArrayList<>();
        long first = (long)scroll * columns;
        long end = Math.min(count, first + (long)visible * columns);
        for (long index = first; index < end; index++)
        {
            int column = (int)(index % columns);
            int x = left + column * (width + gap) / columns;
            int right = left + (column + 1) * (width + gap) / columns - gap;
            cells.add(new Cell((int)index, x, top + (int)((index - first) / columns) * stride,
                    Math.max(0, right - x), rowHeight));
        }
        return new WaypointGridLayout(columns, visible, maxScroll, scroll, List.copyOf(cells), left, top, width, height);
    }

    public Cell at(double x, double y)
    { return cells.stream().filter(cell -> cell.contains(x, y)).findFirst().orElse(null); }

    public boolean contains(double x, double y)
    { return x >= left && x < left + width && y >= top && y < top + height; }
}
//#endif
