package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** Classic main rows and a right-side area for every additional button. */
public final class ClassicPauseLayout
{
    public enum Role { RETURN, ADVANCEMENTS, STATS, MODS, OPTIONS, WORLD_OPTIONS, EXIT }
    public record Size(int width, int height) {}
    public record Rect(int x, int y, int width, int height) {}
    public record Layout(Map<Role, Rect> main, List<Rect> extras) {}
    private static final int FULL_WIDTH = 204, BUTTON_HEIGHT = 20, GAP = 4, MARGIN = 10;
    private ClassicPauseLayout() {}

    public static Layout arrange(int screenWidth, int screenHeight, boolean mods, List<Size> extraSizes)
    {
        int rows = mods ? 5 : 4;
        int mainHeight = rows * (BUTTON_HEIGHT + GAP) - GAP;
        int top = Math.max(MARGIN, Math.min(screenHeight / 4 + 8, screenHeight - MARGIN - mainHeight));
        int extraHeight = extraSizes.stream().mapToInt(Size::height).max().orElse(BUTTON_HEIGHT);
        extraHeight = Math.max(1, Math.min(extraHeight, Math.max(1, screenHeight - top - MARGIN)));
        int rowsPerColumn = Math.max(1, (screenHeight - MARGIN - top + GAP) / (extraHeight + GAP));
        int columns = extraSizes.isEmpty() ? 0 : (extraSizes.size() + rowsPerColumn - 1) / rowsPerColumn;
        int minimumSideWidth = columns == 0 ? 0 : columns * (BUTTON_HEIGHT + GAP) - GAP;
        int mainWidth = Math.max(1, Math.min(FULL_WIDTH, screenWidth - 2 * MARGIN -
                (columns == 0 ? 0 : GAP + minimumSideWidth)));
        int left = Math.max(MARGIN, Math.min((screenWidth - mainWidth) / 2,
                screenWidth - MARGIN - mainWidth - (columns == 0 ? 0 : GAP + minimumSideWidth)));
        int halfWidth = Math.max(1, (mainWidth - GAP) / 2);
        var main = new EnumMap<Role, Rect>(Role.class);
        main.put(Role.RETURN, new Rect(left, top, mainWidth, BUTTON_HEIGHT));
        main.put(Role.ADVANCEMENTS, new Rect(left, top + 24, halfWidth, BUTTON_HEIGHT));
        main.put(Role.STATS, new Rect(left + halfWidth + GAP, top + 24, Math.max(1, mainWidth - halfWidth - GAP), BUTTON_HEIGHT));
        int next = top + 48;
        if (mods) { main.put(Role.MODS, new Rect(left, next, mainWidth, BUTTON_HEIGHT)); next += 24; }
        main.put(Role.OPTIONS, new Rect(left, next, halfWidth, BUTTON_HEIGHT));
        main.put(Role.WORLD_OPTIONS, new Rect(left + halfWidth + GAP, next, Math.max(1, mainWidth - halfWidth - GAP), BUTTON_HEIGHT));
        main.put(Role.EXIT, new Rect(left, next + 24, mainWidth, BUTTON_HEIGHT));
        var extras = new ArrayList<Rect>();
        if (columns != 0)
        {
            int sideX = left + mainWidth + GAP;
            int available = Math.max(1, screenWidth - MARGIN - sideX);
            int columnWidth = Math.max(1, (available - (columns - 1) * GAP) / columns);
            int preferred = extraSizes.stream().mapToInt(Size::width).max().orElse(BUTTON_HEIGHT);
            columnWidth = Math.min(columnWidth, Math.max(1, preferred));
            for (int i = 0; i < extraSizes.size(); i++)
            {
                var size = extraSizes.get(i);
                extras.add(new Rect(sideX + (i / rowsPerColumn) * (columnWidth + GAP),
                        top + (i % rowsPerColumn) * (extraHeight + GAP),
                        Math.max(1, Math.min(size.width(), columnWidth)), Math.max(1, Math.min(size.height(), extraHeight))));
            }
        }
        return new Layout(Map.copyOf(main), List.copyOf(extras));
    }
}
//#endif
