package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ItemManagerHistoryPosition;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService.Channel;

/** Draws halfmasa-owned history rows around optional item-manager entry lists */
public final class ItemManagerHistoryOverlay
{
    private static final int SLOT_SIZE = 18;
    private static final int SEARCH_GAP = 2;
    private static final int JEI_GRID_MARGIN = 6;
    private static final int JEI_CONFIG_BUTTON_WIDTH = 20;
    private static final EnumMap<Channel, PanelState> STATES = new EnumMap<>(Channel.class);
    private static List<Rect> jeiObstacles = List.of();
    private static boolean reiFailureLogged;
    private static boolean jeiFailureLogged;

    static
    {
        STATES.put(Channel.REI, new PanelState());
        STATES.put(Channel.JEI, new PanelState());
    }

    private ItemManagerHistoryOverlay() {}

    public static void renderRei(Object entryList, GuiGraphics graphics, int mouseX, int mouseY)
    {
        render(Channel.REI, graphics, mouseX, mouseY);
    }

    public static void synchronizeReiLayout(Object entryList)
    {
        if (!enabled())
        {
            clear(Channel.REI);
            return;
        }

        PanelState state = STATES.get(Channel.REI);
        Screen screen = Minecraft.getInstance().screen;
        ItemManagerHistoryPosition position = currentPosition();
        int configuredRows = Configs.ITEM_MANAGER_RECIPE_HISTORY_ROWS.getIntegerValue();
        if (state.owner == entryList && state.screen == screen &&
                state.position == position && state.configuredRows == configuredRows &&
                !state.available.empty())
        {
            return;
        }

        try
        {
            findMethod(entryList.getClass(), "updateEntriesPosition", 0).invoke(entryList);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static boolean cyclePosition()
    {
        ItemManagerHistoryPosition next = currentPosition().cycle(true);
        Configs.ITEM_MANAGER_RECIPE_HISTORY_POSITION.setOptionListValue(next);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null)
        {
            Component message = Component.translatable(
                    "halfmasa.message.item_manager_recipe_history_position", next.getDisplayName());
            //#if MC >= 26.0
            client.player.sendSystemMessage(message);
            //#else
            //$$ client.player.displayClientMessage(message, true);
            //#endif
        }
        return true;
    }

    public static void renderJei(Object ingredientListOverlay, GuiGraphics graphics, int mouseX, int mouseY)
    {
        try
        {
            if (!(boolean) findMethod(ingredientListOverlay.getClass(), "isListDisplayed", 0)
                    .invoke(ingredientListOverlay))
            {
                STATES.get(Channel.JEI).area = Rect.EMPTY;
                STATES.get(Channel.JEI).available = Rect.EMPTY;
                return;
            }
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
            return;
        }
        updateJeiGridArea(ingredientListOverlay);
        render(Channel.JEI, graphics, mouseX, mouseY);
    }

    public static boolean handleReiClick(Object entryList, double mouseX, double mouseY, int button)
    {
        return handleClick(Channel.REI, STATES.get(Channel.REI).area, mouseX, mouseY, button);
    }

    public static boolean handleJeiClick(double mouseX, double mouseY, int button)
    {
        PanelState state = STATES.get(Channel.JEI);
        Minecraft client = Minecraft.getInstance();
        if (state.screen != client.screen)
        {
            return false;
        }
        return handleClick(Channel.JEI, state.area, mouseX, mouseY, button);
    }

    public static Object reserveJeiArea(Object overlay, Object available)
    {
        if (!enabled() || available == null)
        {
            clear(Channel.JEI);
            return available;
        }
        try
        {
            Rect original = readRect(available);
            if (original.empty())
            {
                clear(Channel.JEI);
                return available;
            }
            int maxColumns = readJeiMaxColumns(overlay, original.width / SLOT_SIZE);
            PanelState state = STATES.get(Channel.JEI);
            Rect searchArea = state.screen == Minecraft.getInstance().screen
                    ? state.searchArea
                    : Rect.EMPTY;
            if (searchArea.empty())
            {
                searchArea = readJeiSearchArea(overlay);
            }
            prepare(Channel.JEI, overlay, original, searchArea, SLOT_SIZE, maxColumns);
            return available;
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
            return available;
        }
    }

    public static void reserveReiArea(Object entryList)
    {
        if (!enabled())
        {
            clear(Channel.REI);
            return;
        }
        try
        {
            Object rectangle = readField(entryList, "innerBounds");
            Rect original = readReiNativeArea(entryList);
            if (original.empty())
            {
                original = readRect(rectangle);
            }
            int slotSize = ((Number) findMethod(entryList.getClass(), "entrySize", 0).invoke(null)).intValue();
            Rect searchArea = readReiSearchArea();
            Rect reserved = prepare(Channel.REI, entryList, original, searchArea, slotSize, original.width / slotSize);
            writeRect(rectangle, reserved);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static void initializeReiOverlay(Object overlay)
    {
        if (!enabled())
        {
            clear(Channel.REI);
            return;
        }
        try
        {
            Object entryList = findMethod(overlay.getClass(), "getEntryListWidget", 0).invoke(null);
            findMethod(entryList.getClass(), "updateEntriesPosition", 0).invoke(entryList);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static void reserveReiFavoritesArea(Object favoritesList)
    {
        PanelState state = STATES.get(Channel.REI);
        if (!enabled() || state.screen != Minecraft.getInstance().screen || state.area.empty())
        {
            return;
        }
        try
        {
            Object rectangle = readField(favoritesList, "favoritesBounds");
            Rect original = readRect(rectangle);
            Rect reserved = excludeVertical(original, state.area);
            writeRect(rectangle, reserved);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static void updateJeiSearchArea(Object overlay, Object searchRectangle)
    {
        if (!enabled())
        {
            return;
        }
        try
        {
            Rect search = readRect(searchRectangle);
            if (search.empty())
            {
                return;
            }
            search = new Rect(search.x, search.y,
                    search.width + JEI_CONFIG_BUTTON_WIDTH, search.height);

            PanelState state = STATES.get(Channel.JEI);
            Screen screen = Minecraft.getInstance().screen;
            boolean changed = state.screen != screen || !search.equals(state.searchArea);
            state.searchArea = search;
            state.screen = screen;
            state.owner = overlay;
            if (!state.available.empty())
            {
                Layout layout = createLayout(Channel.JEI, state.available, search,
                        state.nativeGridArea, state.expanded, state.slotSize, state.configuredColumns);
                state.area = layout.panel;
                state.buttonCell = layout.buttonCell;
                state.columns = layout.columns;
                state.rows = layout.rows;
            }

            if (changed && !state.relayoutPending)
            {
                state.relayoutPending = true;
                Minecraft.getInstance().execute(() ->
                {
                    state.relayoutPending = false;
                    if (enabled() && state.owner == overlay &&
                            state.screen == Minecraft.getInstance().screen)
                    {
                        refreshNativeLayout(Channel.JEI, state);
                    }
                });
            }
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
        }
    }

    public static void updateJeiBookmarkAreas(Object bookmarkOverlay)
    {
        if (!enabled())
        {
            return;
        }
        try
        {
            Rect screen = currentScreenRect();
            List<Rect> updatedObstacles = List.copyOf(collectJeiObstacles(bookmarkOverlay, screen));
            if (updatedObstacles.equals(jeiObstacles))
            {
                return;
            }
            jeiObstacles = updatedObstacles;
            PanelState state = STATES.get(Channel.JEI);
            if (state.screen == Minecraft.getInstance().screen && !state.available.empty())
            {
                Layout layout = createLayout(Channel.JEI, state.available, state.searchArea,
                        state.nativeGridArea, state.expanded, state.slotSize, state.configuredColumns);
                state.area = layout.panel;
                state.buttonCell = layout.buttonCell;
                state.columns = layout.columns;
                state.rows = layout.rows;
            }
        }
        catch (Throwable ignored) {}
    }

    public static int[] getJeiHistoryExclusionArea()
    {
        PanelState state = STATES.get(Channel.JEI);
        if (!enabled() || state.screen != Minecraft.getInstance().screen || state.area.empty())
        {
            return null;
        }
        return new int[] { state.area.x, state.area.y, state.area.width, state.area.height };
    }

    private static void updateJeiGridArea(Object overlay)
    {
        try
        {
            Object contents = readField(overlay, "contents");
            Object ingredientGrid = readField(contents, "ingredientGrid");
            Rect gridArea = readRect(findMethod(ingredientGrid.getClass(), "getArea", 0)
                    .invoke(ingredientGrid));
            if (gridArea.empty())
            {
                return;
            }

            PanelState state = STATES.get(Channel.JEI);
            if (gridArea.equals(state.nativeGridArea))
            {
                return;
            }
            state.nativeGridArea = gridArea;
            if (!state.available.empty())
            {
                Layout layout = createLayout(Channel.JEI, state.available, state.searchArea,
                        gridArea, state.expanded, state.slotSize, state.configuredColumns);
                state.area = layout.panel;
                state.buttonCell = layout.buttonCell;
                state.columns = layout.columns;
                state.rows = layout.rows;
            }

            if (!state.relayoutPending)
            {
                state.relayoutPending = true;
                Minecraft.getInstance().execute(() ->
                {
                    state.relayoutPending = false;
                    if (enabled() && state.owner == overlay &&
                            state.screen == Minecraft.getInstance().screen)
                    {
                        refreshNativeLayout(Channel.JEI, state);
                    }
                });
            }
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
        }
    }

    public static void reset()
    {
        jeiObstacles = List.of();
        for (PanelState state : STATES.values())
        {
            state.expanded = true;
            state.area = Rect.EMPTY;
            state.available = Rect.EMPTY;
            state.searchArea = Rect.EMPTY;
            state.nativeGridArea = Rect.EMPTY;
            state.screen = null;
            state.owner = null;
            state.relayoutPending = false;
        }
    }

    public static void resetBounds()
    {
        jeiObstacles = List.of();
        for (PanelState state : STATES.values())
        {
            state.area = Rect.EMPTY;
            state.available = Rect.EMPTY;
            state.searchArea = Rect.EMPTY;
            state.nativeGridArea = Rect.EMPTY;
            state.screen = null;
            state.owner = null;
            state.relayoutPending = false;
        }
    }

    private static void clear(Channel channel)
    {
        PanelState state = STATES.get(channel);
        state.area = Rect.EMPTY;
        state.available = Rect.EMPTY;
        state.searchArea = Rect.EMPTY;
        state.nativeGridArea = Rect.EMPTY;
        state.screen = null;
        state.owner = null;
        state.relayoutPending = false;
    }

    private static Rect prepare(
            Channel channel, Object owner, Rect nativeArea, Rect searchArea, int slotSize, int configuredColumns)
    {
        PanelState state = STATES.get(channel);
        state.owner = owner;
        state.screen = Minecraft.getInstance().screen;
        Layout layout = createLayout(channel, nativeArea, searchArea, state.nativeGridArea,
                state.expanded, slotSize, configuredColumns);
        state.area = layout.panel;
        state.available = nativeArea;
        state.searchArea = searchArea;
        state.buttonCell = layout.buttonCell;
        state.columns = layout.columns;
        state.rows = layout.rows;
        state.slotSize = slotSize;
        state.configuredColumns = configuredColumns;
        state.configuredRows = Configs.ITEM_MANAGER_RECIPE_HISTORY_ROWS.getIntegerValue();
        state.position = currentPosition();
        return excludeVertical(nativeArea, layout.panel);
    }

    private static Rect excludeVertical(Rect available, Rect excluded)
    {
        if (!excluded.intersects(available))
        {
            return available;
        }
        ItemManagerHistoryPosition position = currentPosition();
        boolean top = position == ItemManagerHistoryPosition.TOP_LEFT ||
                position == ItemManagerHistoryPosition.TOP_RIGHT;
        if (top)
        {
            int newY = Math.min(available.y + available.height, excluded.y + excluded.height);
            return new Rect(available.x, newY, available.width,
                    Math.max(0, available.y + available.height - newY));
        }
        return new Rect(available.x, available.y, available.width,
                Math.max(0, excluded.y - available.y));
    }

    private static void render(Channel channel, GuiGraphics graphics, int mouseX, int mouseY)
    {
        PanelState state = STATES.get(channel);
        if (!enabled() || state.screen != Minecraft.getInstance().screen || state.area.empty())
        {
            return;
        }
        draw(channel, state, graphics, mouseX, mouseY);
    }

    private static void draw(Channel channel, PanelState state, GuiGraphics graphics, int mouseX, int mouseY)
    {
        Layout layout = new Layout(state.area, state.columns, state.rows, state.buttonCell, state.slotSize);
        List<ItemStack> history = ItemSearchHistoryService.getInstance().get(channel);
        int capacity = Math.max(0, layout.columns * layout.rows - 1);
        int count = Math.min(history.size(), capacity);
        for (int index = 0; index < count; index++)
        {
            int cell = index >= layout.buttonCell ? index + 1 : index;
            int x = layout.panel.x + cell % layout.columns * layout.slotSize;
            int y = layout.panel.y + cell / layout.columns * layout.slotSize;
            int itemOffset = Math.max(0, (layout.slotSize - 16) / 2);
            ItemStack stack = history.get(index);
            graphics.renderItem(stack, x + itemOffset, y + itemOffset);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x + itemOffset, y + itemOffset);
            if (mouseX >= x && mouseX < x + layout.slotSize && mouseY >= y && mouseY < y + layout.slotSize &&
                    Minecraft.getInstance().screen != null)
            {
                Minecraft.getInstance().screen.setTooltipForNextRenderPass(stack.getHoverName());
            }
        }

        if (state.expanded)
        {
            drawDashedBorder(graphics, layout.panel);
        }
        int buttonX = layout.panel.x + layout.buttonCell % layout.columns * layout.slotSize;
        int buttonY = layout.panel.y + layout.buttonCell / layout.columns * layout.slotSize;
        drawToggle(graphics, buttonX, buttonY, layout.slotSize, state.expanded, mouseX, mouseY);
    }

    private static void drawDashedBorder(GuiGraphics graphics, Rect panel)
    {
        int color = 0xB0FFFFFF;
        for (int x = panel.x; x < panel.x + panel.width; x += 6)
        {
            graphics.fill(x, panel.y, Math.min(x + 3, panel.x + panel.width), panel.y + 1, color);
            graphics.fill(x, panel.y + panel.height - 1, Math.min(x + 3, panel.x + panel.width), panel.y + panel.height, color);
        }
        for (int y = panel.y; y < panel.y + panel.height; y += 6)
        {
            graphics.fill(panel.x, y, panel.x + 1, Math.min(y + 3, panel.y + panel.height), color);
            graphics.fill(panel.x + panel.width - 1, y, panel.x + panel.width, Math.min(y + 3, panel.y + panel.height), color);
        }
    }

    private static void drawToggle(
            GuiGraphics graphics, int x, int y, int slotSize, boolean expanded, int mouseX, int mouseY)
    {
        String label = expanded ? "[-]" : "[+]";
        int textX = x + (slotSize - Minecraft.getInstance().font.width(label)) / 2;
        int textY = y + (slotSize - 8) / 2;
        int color = inside(mouseX, mouseY, x, y, slotSize, slotSize) ? 0xFFFFFFFF : 0xFFE0E0E0;
        graphics.drawString(Minecraft.getInstance().font, label, textX, textY, color, true);
    }

    private static boolean handleClick(Channel channel, Rect area, double mouseX, double mouseY, int button)
    {
        if (!enabled() || area.empty())
        {
            return false;
        }

        PanelState state = STATES.get(channel);
        Layout layout = new Layout(state.area, state.columns, state.rows, state.buttonCell, state.slotSize);
        int buttonX = layout.panel.x + layout.buttonCell % layout.columns * layout.slotSize;
        int buttonY = layout.panel.y + layout.buttonCell / layout.columns * layout.slotSize;
        if (inside(mouseX, mouseY, buttonX, buttonY, layout.slotSize, layout.slotSize))
        {
            state.expanded = !state.expanded;
            refreshNativeLayout(channel, state);
            return true;
        }
        if (!state.expanded || !inside(
                mouseX, mouseY, layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height))
        {
            return false;
        }

        int column = (int) (mouseX - layout.panel.x) / layout.slotSize;
        int row = (int) (mouseY - layout.panel.y) / layout.slotSize;
        int cell = row * layout.columns + column;
        int index = cell > layout.buttonCell ? cell - 1 : cell;
        List<ItemStack> history = ItemSearchHistoryService.getInstance().get(channel);
        if (cell == layout.buttonCell || index < 0 || index >= history.size() ||
                index >= layout.columns * layout.rows - 1)
        {
            return true;
        }

        ItemStack stack = history.get(index);
        boolean handled = channel == Channel.REI
                ? activateRei(stack, button)
                : activateJei(stack, button);
        if (handled)
        {
            ItemSearchHistoryService.getInstance().record(channel, stack);
        }
        return true;
    }

    private static Layout createLayout(
            Channel channel, Rect available, Rect searchArea, Rect nativeGridArea,
            boolean expanded, int slotSize, int configuredColumns)
    {
        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        Rect alignmentArea = channel == Channel.JEI && !nativeGridArea.empty()
                ? nativeGridArea
                : channel == Channel.JEI && available.width > JEI_GRID_MARGIN * 2
                        ? new Rect(available.x + JEI_GRID_MARGIN, available.y,
                                available.width - JEI_GRID_MARGIN * 2, available.height)
                        : available;
        int availableColumns = Math.max(1, Math.min(configuredColumns, alignmentArea.width / slotSize));
        int columns = expanded ? Math.min(availableColumns, Math.max(1, screenWidth / slotSize)) : 1;
        int availableRows = Math.max(1, screenHeight / slotSize);
        int rows = expanded
                ? Math.min(Configs.ITEM_MANAGER_RECIPE_HISTORY_ROWS.getIntegerValue(), availableRows)
                : 1;
        int width = columns * slotSize;
        int height = rows * slotSize;
        ItemManagerHistoryPosition position = currentPosition();
        boolean right = position == ItemManagerHistoryPosition.TOP_RIGHT ||
                position == ItemManagerHistoryPosition.BOTTOM_RIGHT;
        boolean bottom = position == ItemManagerHistoryPosition.BOTTOM_LEFT ||
                position == ItemManagerHistoryPosition.BOTTOM_RIGHT;
        Rect screen = new Rect(6, 6, screenWidth - 12, screenHeight - 12);
        Rect panel = anchored(screen, width, height, position);
        if (right && alignmentArea.x + alignmentArea.width > screenWidth / 2)
        {
            panel = new Rect(
                    Math.max(screen.x, alignmentArea.x + alignmentArea.width - width),
                    panel.y,
                    width,
                    height);
        }
        else if (!right && alignmentArea.x < screenWidth / 2)
        {
            panel = new Rect(
                    Math.min(screen.x + screen.width - width, Math.max(screen.x, alignmentArea.x)),
                    panel.y,
                    width,
                    height);
        }
        if (channel == Channel.REI)
        {
            panel = avoidReiControls(panel, screen, position, STATES.get(channel).owner);
        }
        else if (channel == Channel.JEI)
        {
            panel = avoidJeiControls(panel, screen, position);
        }
        panel = avoidSearchField(panel, searchArea, screen);
        boolean panelRight = panel.x + panel.width / 2 >= screenWidth / 2;
        int buttonColumn = panelRight ? columns - 1 : 0;
        int buttonRow = bottom ? rows - 1 : 0;
        return new Layout(panel, columns, rows, buttonRow * columns + buttonColumn, slotSize);
    }

    private static ItemManagerHistoryPosition currentPosition()
    {
        return (ItemManagerHistoryPosition) Configs.ITEM_MANAGER_RECIPE_HISTORY_POSITION.getOptionListValue();
    }

    private static Rect avoidReiControls(
            Rect panel, Rect screen, ItemManagerHistoryPosition position, Object entryList)
    {
        boolean top = position == ItemManagerHistoryPosition.TOP_LEFT ||
                position == ItemManagerHistoryPosition.TOP_RIGHT;
        Integer chosenY = findGapY(panel, screen, readReiObstacles(entryList, screen), top);
        return chosenY == null ? panel : new Rect(panel.x, chosenY, panel.width, panel.height);
    }

    private static Rect avoidSearchField(Rect panel, Rect searchArea, Rect screen)
    {
        if (searchArea.empty() || !panel.intersects(searchArea))
        {
            return panel;
        }

        int screenBottom = screen.y + screen.height;
        int searchCenter = searchArea.y + searchArea.height / 2;
        int screenCenter = screen.y + screen.height / 2;
        int preferredY = searchCenter >= screenCenter
                ? searchArea.y - panel.height - SEARCH_GAP
                : searchArea.y + searchArea.height + SEARCH_GAP;
        int clampedY = Math.max(screen.y, Math.min(preferredY, screenBottom - panel.height));
        return new Rect(panel.x, clampedY, panel.width, panel.height);
    }

    private static Rect avoidJeiControls(
            Rect panel, Rect screen, ItemManagerHistoryPosition position)
    {
        boolean top = position == ItemManagerHistoryPosition.TOP_LEFT ||
                position == ItemManagerHistoryPosition.TOP_RIGHT;
        List<Rect> obstacles = readJeiObstacles(screen);
        Integer chosenY = findJeiGapY(panel, screen, obstacles, top);
        return chosenY == null
                ? panel
                : new Rect(panel.x, chosenY, panel.width, panel.height);
    }

    private static Integer findJeiGapY(
            Rect panel, Rect screen, List<Rect> obstacles, boolean top)
    {
        return findGapY(panel, screen, obstacles, top);
    }

    private static Integer findGapY(
            Rect panel, Rect screen, List<Rect> obstacles, boolean top)
    {
        int screenBottom = screen.y + screen.height;
        List<int[]> blocked = new ArrayList<>();
        for (Rect obstacle : obstacles)
        {
            boolean horizontalOverlap = panel.x < obstacle.x + obstacle.width &&
                    panel.x + panel.width > obstacle.x;
            if (horizontalOverlap)
            {
                int start = Math.max(screen.y, obstacle.y - SEARCH_GAP);
                int end = Math.min(screenBottom,
                        obstacle.y + obstacle.height + SEARCH_GAP);
                if (end > start)
                {
                    blocked.add(new int[] { start, end });
                }
            }
        }
        if (blocked.isEmpty())
        {
            return panel.y;
        }

        blocked.sort((first, second) -> Integer.compare(first[0], second[0]));
        Integer nearestTop = null;
        Integer nearestBottom = null;
        int cursor = screen.y;
        for (int[] interval : blocked)
        {
            if (interval[0] > cursor && interval[0] - cursor >= panel.height)
            {
                if (nearestTop == null)
                {
                    nearestTop = cursor;
                }
                nearestBottom = interval[0] - panel.height;
            }
            cursor = Math.max(cursor, interval[1]);
        }
        if (screenBottom - cursor >= panel.height)
        {
            if (nearestTop == null)
            {
                nearestTop = cursor;
            }
            nearestBottom = screenBottom - panel.height;
        }

        Integer chosenY = top ? nearestTop : nearestBottom;
        return chosenY;
    }

    private static List<Rect> readJeiObstacles(Rect screen)
    {
        if (!jeiObstacles.isEmpty())
        {
            return jeiObstacles;
        }
        List<Rect> obstacles = new ArrayList<>();
        try
        {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Object runtime = findMethod(internalClass, "getJeiRuntime", 0).invoke(null);
            Object bookmarkOverlay = findMethod(runtime.getClass(), "getBookmarkOverlay", 0).invoke(runtime);
            obstacles.addAll(collectJeiObstacles(bookmarkOverlay, screen));
        }
        catch (Throwable ignored) {}

        if (obstacles.isEmpty())
        {
            obstacles.add(new Rect(screen.x, screen.y + screen.height - 20, 42, 20));
        }
        return obstacles;
    }

    private static List<Rect> readReiObstacles(Object entryList, Rect screen)
    {
        List<Rect> obstacles = new ArrayList<>();
        Rect searchArea = readReiSearchArea();
        if (!searchArea.empty())
        {
            obstacles.add(new Rect(searchArea.x, searchArea.y,
                    Math.min(screen.x + screen.width - searchArea.x, searchArea.width + 48),
                    searchArea.height));
        }

        if (entryList != null)
        {
            try
            {
                Object widgets = readField(entryList, "additionalWidgets");
                if (widgets instanceof Iterable<?> iterable)
                {
                    for (Object widget : iterable)
                    {
                        addWidgetBounds(obstacles, widget);
                    }
                }
            }
            catch (Throwable ignored) {}
        }

        try
        {
            Class<?> overlayClass = Class.forName("me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl");
            Object overlay = findMethod(overlayClass, "getInstance", 0).invoke(null);
            addWidgetBounds(obstacles, readField(overlay, "configButton"));

            Object favorites = findMethod(overlayClass, "getFavoritesListWidget", 0).invoke(null);
            if (favorites != null)
            {
                Rect fullBounds = readRect(readField(favorites, "fullBounds"));
                if (!fullBounds.empty())
                {
                    obstacles.add(new Rect(fullBounds.x + 4,
                            fullBounds.y + fullBounds.height - 20, 16, 16));
                }
            }
        }
        catch (Throwable ignored) {}
        return obstacles;
    }

    private static void addWidgetBounds(List<Rect> obstacles, Object widget)
    {
        if (widget == null)
        {
            return;
        }
        try
        {
            addRect(obstacles, findMethod(widget.getClass(), "getBounds", 0).invoke(widget));
        }
        catch (Throwable ignored) {}
    }

    private static List<Rect> collectJeiObstacles(Object bookmarkOverlay, Rect screen) throws Exception
    {
        List<Rect> obstacles = new ArrayList<>();
        Object bookmarkButton = readField(bookmarkOverlay, "bookmarkButton");
        Object historyButton = readField(bookmarkOverlay, "historyButton");
        addRect(obstacles, findMethod(bookmarkButton.getClass(), "getArea", 0).invoke(bookmarkButton));
        addRect(obstacles, findMethod(historyButton.getClass(), "getArea", 0).invoke(historyButton));

        if (obstacles.isEmpty())
        {
            obstacles.add(new Rect(screen.x, screen.y + screen.height - 20, 42, 20));
        }
        return obstacles;
    }

    private static Rect currentScreenRect()
    {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        return new Rect(6, 6, width - 12, height - 12);
    }

    private static void addRect(List<Rect> rectangles, Object rectangle) throws Exception
    {
        Rect value = readRect(rectangle);
        if (!value.empty())
        {
            rectangles.add(value);
        }
    }

    private static Rect anchored(Rect screen, int width, int height, ItemManagerHistoryPosition position)
    {
        boolean right = position == ItemManagerHistoryPosition.TOP_RIGHT || position == ItemManagerHistoryPosition.BOTTOM_RIGHT;
        boolean bottom = position == ItemManagerHistoryPosition.BOTTOM_LEFT || position == ItemManagerHistoryPosition.BOTTOM_RIGHT;
        int x = right ? screen.x + screen.width - width : screen.x;
        int y = bottom ? screen.y + screen.height - height : screen.y;
        return new Rect(x, y, width, height);
    }

    private static void refreshNativeLayout(Channel channel, PanelState state)
    {
        try
        {
            if (state.owner != null)
            {
                if (channel == Channel.JEI)
                {
                    findMethod(state.owner.getClass(), "onScreenPropertiesChanged", 0).invoke(state.owner);
                }
                else
                {
                    findMethod(state.owner.getClass(), "updateEntriesPosition", 0).invoke(state.owner);
                }
                return;
            }
        }
        catch (Throwable throwable)
        {
            logFailure(channel, throwable);
        }
        Layout layout = createLayout(
                channel, state.available, state.searchArea, state.nativeGridArea,
                state.expanded, state.slotSize, state.configuredColumns);
        state.area = layout.panel;
        state.columns = layout.columns;
        state.rows = layout.rows;
        state.buttonCell = layout.buttonCell;
    }

    private static int readJeiMaxColumns(Object overlay, int fallback)
    {
        try
        {
            Object contents = readField(overlay, "contents");
            Object gridConfig = readField(contents, "gridConfig");
            return Math.max(1, ((Number) findMethod(gridConfig.getClass(), "getMaxColumns", 0)
                    .invoke(gridConfig)).intValue());
        }
        catch (Throwable ignored)
        {
            return Math.max(1, fallback);
        }
    }

    private static Rect readJeiSearchArea(Object overlay)
    {
        try
        {
            Object screenPropertiesCache = readField(overlay, "screenPropertiesCache");
            Object optionalProperties = findMethod(screenPropertiesCache.getClass(), "getGuiProperties", 0)
                    .invoke(screenPropertiesCache);
            if (optionalProperties instanceof Optional<?> optional && optional.isPresent())
            {
                Object guiProperties = optional.get();
                Object displayArea = findMethod(overlay.getClass(), "createDisplayArea", 1)
                        .invoke(null, guiProperties);
                Object clientConfig = readField(overlay, "clientConfig");
                boolean centered = (boolean) findMethod(overlay.getClass(), "isSearchBarCentered", 2)
                        .invoke(null, clientConfig, guiProperties);
                Object searchAndConfigArea = findMethod(overlay.getClass(), "getSearchAndConfigArea", 3)
                        .invoke(overlay, displayArea, centered, guiProperties);
                return readRect(searchAndConfigArea);
            }
        }
        catch (Throwable ignored) {}

        try
        {
            Object searchField = readField(overlay, "searchField");
            return readRect(readField(searchField, "area"));
        }
        catch (Throwable ignored)
        {
            return Rect.EMPTY;
        }
    }

    private static Rect readReiSearchArea()
    {
        try
        {
            Class<?> overlayClass = Class.forName("me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl");
            Object overlay = findMethod(overlayClass, "getInstance", 0).invoke(null);
            Object searchArea = findMethod(overlayClass, "getSearchFieldArea", 0).invoke(overlay);
            return readRect(searchArea);
        }
        catch (Throwable ignored)
        {
            return Rect.EMPTY;
        }
    }

    private static Rect readReiNativeArea(Object entryList)
    {
        try
        {
            Object bounds = readField(entryList, "bounds");
            Class<?> entryListClass = Class.forName(
                    "me.shedaniel.rei.impl.client.gui.widget.entrylist.EntryListWidget");
            Object rectangle = findMethod(entryListClass, "updateInnerBounds", 1)
                    .invoke(null, bounds);
            return readRect(rectangle);
        }
        catch (Throwable ignored)
        {
            return Rect.EMPTY;
        }
    }

    private static boolean activateRei(ItemStack stack, int button)
    {
        try
        {
            Class<?> entryStacks = Class.forName("me.shedaniel.rei.api.common.util.EntryStacks");
            Object entry = findMethod(entryStacks, "of", 1).invoke(null, stack.copy());
            Class<?> helperClass = Class.forName("me.shedaniel.rei.api.client.ClientHelper");
            Object helper = findMethod(helperClass, "getInstance", 0).invoke(null);
            boolean cheating = (boolean) findMethod(helper.getClass(), "isCheating", 0).invoke(helper);
            if (cheating && !Screen.hasControlDown())
            {
                return (boolean) findMethod(helper.getClass(), "tryCheatingEntry", 1).invoke(helper, entry);
            }

            Class<?> builderClass = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
            Object builder = findMethod(builderClass, "builder", 0).invoke(null);
            String action = button == 1 ? "addUsagesFor" : "addRecipesFor";
            findMethod(builder.getClass(), action, 1).invoke(builder, entry);
            return (boolean) findMethod(builder.getClass(), "open", 0).invoke(builder);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
            return false;
        }
    }

    private static boolean activateJei(ItemStack stack, int button)
    {
        try
        {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Object runtime = findMethod(internalClass, "getJeiRuntime", 0).invoke(null);
            Object helpers = findMethod(runtime.getClass(), "getJeiHelpers", 0).invoke(runtime);
            Object focusFactory = findMethod(helpers.getClass(), "getFocusFactory", 0).invoke(helpers);
            Class<?> vanillaTypes = Class.forName("mezz.jei.api.constants.VanillaTypes");
            Object itemStackType = vanillaTypes.getField("ITEM_STACK").get(null);
            Class<?> roleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object role = Enum.valueOf((Class<? extends Enum>) roleClass, button == 1 ? "INPUT" : "OUTPUT");
            Object focus = findMethod(focusFactory.getClass(), "createFocus", 3)
                    .invoke(focusFactory, role, itemStackType, stack.copy());
            Object recipesGui = findMethod(runtime.getClass(), "getRecipesGui", 0).invoke(runtime);
            findMethodAccepting(recipesGui.getClass(), "show", List.class).invoke(recipesGui, List.of(focus));
            return true;
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
            return false;
        }
    }

    private static Rect readReiArea(Object entryList)
    {
        try
        {
            Object bounds = readField(entryList, "innerBounds");
            if (bounds == null)
            {
                bounds = findMethod(entryList.getClass(), "getBounds", 0).invoke(entryList);
            }
            return readRect(bounds);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
            return Rect.EMPTY;
        }
    }

    private static Rect readJeiArea(Object overlay)
    {
        try
        {
            Object contents = readField(overlay, "contents");
            Object area = findMethod(contents.getClass(), "getBackgroundArea", 0).invoke(contents);
            return readRect(area);
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
            return Rect.EMPTY;
        }
    }

    private static Rect readRect(Object rectangle) throws Exception
    {
        if (rectangle == null)
        {
            return Rect.EMPTY;
        }
        return new Rect(
                readInt(rectangle, "x", "getX"),
                readInt(rectangle, "y", "getY"),
                readInt(rectangle, "width", "getWidth"),
                readInt(rectangle, "height", "getHeight"));
    }

    private static int readInt(Object object, String fieldName, String methodName) throws Exception
    {
        try
        {
            Field field = findField(object.getClass(), fieldName);
            field.setAccessible(true);
            return ((Number) field.get(object)).intValue();
        }
        catch (NoSuchFieldException exception)
        {
            return ((Number) findMethod(object.getClass(), methodName, 0).invoke(object)).intValue();
        }
    }

    private static void writeRect(Object rectangle, Rect value) throws Exception
    {
        Field x = findField(rectangle.getClass(), "x");
        Field y = findField(rectangle.getClass(), "y");
        Field width = findField(rectangle.getClass(), "width");
        Field height = findField(rectangle.getClass(), "height");
        x.setAccessible(true);
        y.setAccessible(true);
        width.setAccessible(true);
        height.setAccessible(true);
        x.setInt(rectangle, value.x);
        y.setInt(rectangle, value.y);
        width.setInt(rectangle, value.width);
        height.setInt(rectangle, value.height);
    }

    private static Object readField(Object object, String name) throws Exception
    {
        Field field = findField(object.getClass(), name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException
    {
        for (Class<?> current = type; current != null; current = current.getSuperclass())
        {
            try
            {
                return current.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException
    {
        for (Class<?> current = type; current != null; current = current.getSuperclass())
        {
            for (Method method : current.getDeclaredMethods())
            {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount &&
                        (Modifier.isPublic(method.getModifiers()) || current == type))
                {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        for (Method method : type.getMethods())
        {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount)
            {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Method findMethodAccepting(Class<?> type, String name, Class<?> argumentType) throws NoSuchMethodException
    {
        for (Method method : type.getMethods())
        {
            if (method.getName().equals(name) && method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(argumentType))
            {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "(" + argumentType.getName() + ")");
    }

    private static boolean enabled()
    {
        return Configs.ITEM_MANAGER_RECIPE_HISTORY.getBooleanValue();
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height)
    {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static void logFailure(Channel channel, Throwable throwable)
    {
        if (channel == Channel.REI)
        {
            if (reiFailureLogged) return;
            reiFailureLogged = true;
        }
        else
        {
            if (jeiFailureLogged) return;
            jeiFailureLogged = true;
        }
        XaeroWorldBinding.LOGGER.warn("Disabled {} item history compatibility after an API mismatch", channel, throwable);
    }

    private static final class PanelState
    {
        private boolean expanded = true;
        private Rect area = Rect.EMPTY;
        private Rect available = Rect.EMPTY;
        private Rect searchArea = Rect.EMPTY;
        private Rect nativeGridArea = Rect.EMPTY;
        private Screen screen;
        private Object owner;
        private boolean relayoutPending;
        private int columns = 1;
        private int rows = 1;
        private int buttonCell;
        private int slotSize = SLOT_SIZE;
        private int configuredColumns = 1;
        private int configuredRows;
        private ItemManagerHistoryPosition position;
    }

    private record Rect(int x, int y, int width, int height)
    {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        private boolean empty()
        {
            return this.width <= 0 || this.height <= 0;
        }

        private boolean intersects(Rect other)
        {
            return !this.empty() && !other.empty() && this.x < other.x + other.width &&
                    this.x + this.width > other.x && this.y < other.y + other.height &&
                    this.y + this.height > other.y;
        }
    }

    private record Layout(Rect panel, int columns, int rows, int buttonCell, int slotSize) {}
}
