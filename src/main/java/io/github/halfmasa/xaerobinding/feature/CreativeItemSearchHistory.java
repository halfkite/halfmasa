package io.github.halfmasa.xaerobinding.feature;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService.Channel;

/** Adds a recent-item prefix to the creative search tab */
public final class CreativeItemSearchHistory
{
    private static final int COLUMNS = 9;
    private static final Map<CreativeModeInventoryScreen, State> STATES = new WeakHashMap<>();

    private CreativeItemSearchHistory() {}

    public static void rebuild(
            CreativeModeInventoryScreen screen,
            CreativeModeTab tab,
            NonNullList<ItemStack> items,
            String searchText)
    {
        State previous = STATES.get(screen);
        boolean expanded = previous == null || previous.expanded;
        if (!isEnabledOn(tab, searchText))
        {
            STATES.remove(screen);
            return;
        }

        State state = new State(copy(items), expanded);
        STATES.put(screen, state);
        apply(state, items);
    }

    public static boolean ensureApplied(
            CreativeModeInventoryScreen screen,
            CreativeModeTab tab,
            NonNullList<ItemStack> items,
            String searchText)
    {
        State state = STATES.get(screen);
        if (!isEnabledOn(tab, searchText))
        {
            if (state != null)
            {
                replace(items, state.source);
                STATES.remove(screen);
                return true;
            }
            return false;
        }

        if (state == null)
        {
            rebuild(screen, tab, items, searchText);
            return true;
        }
        return false;
    }

    public static boolean restoreSource(CreativeModeInventoryScreen screen, NonNullList<ItemStack> items)
    {
        State state = STATES.get(screen);
        if (state == null)
        {
            return false;
        }
        replace(items, state.source);
        STATES.remove(screen);
        return true;
    }

    public static boolean isApplied(CreativeModeInventoryScreen screen)
    {
        return STATES.containsKey(screen);
    }

    public static boolean toggle(
            CreativeModeInventoryScreen screen,
            int visibleSlotIndex,
            float scrollOffset,
            NonNullList<ItemStack> items)
    {
        State state = STATES.get(screen);
        if (state == null || visibleSlotIndex != 0 || !isAtTop(scrollOffset))
        {
            return false;
        }
        state.expanded = !state.expanded;
        apply(state, items);
        return true;
    }

    public static boolean isHistorySlot(
            CreativeModeInventoryScreen screen,
            int visibleSlotIndex,
            float scrollOffset)
    {
        State state = STATES.get(screen);
        if (state == null || !isAtTop(scrollOffset) || visibleSlotIndex < 0)
        {
            return false;
        }
        int prefixSize = state.expanded ? Configs.ITEM_SEARCH_HISTORY_ROWS.getIntegerValue() * COLUMNS : 1;
        return visibleSlotIndex < prefixSize;
    }

    public static void refreshHistory(CreativeModeInventoryScreen screen, NonNullList<ItemStack> items)
    {
        State state = STATES.get(screen);
        if (state != null)
        {
            apply(state, items);
        }
    }

    public static void renderToggle(
            CreativeModeInventoryScreen screen,
            GuiGraphics graphics,
            int x,
            int y,
            float scrollOffset)
    {
        State state = STATES.get(screen);
        if (state == null || !isAtTop(scrollOffset))
        {
            return;
        }

        graphics.fill(x, y, x + 16, y + 16, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF6B6B6B);
        String label = state.expanded ? "[-]" : "[+]";
        int textX = x + (16 - Minecraft.getInstance().font.width(label)) / 2;
        graphics.drawString(Minecraft.getInstance().font, label, textX, y + 5, 0xFFFFFFFF, true);
    }

    private static boolean isEnabledOn(CreativeModeTab tab, String searchText)
    {
        boolean searching = searchText != null && !searchText.trim().isEmpty();
        return Configs.ITEM_SEARCH_HISTORY.getBooleanValue() && tab == CreativeModeTabs.searchTab() &&
                (!searching || Configs.ITEM_SEARCH_HISTORY_DURING_SEARCH.getBooleanValue());
    }

    private static boolean isAtTop(float scrollOffset)
    {
        return scrollOffset <= 0.0001F;
    }

    private static void apply(State state, NonNullList<ItemStack> target)
    {
        target.clear();
        target.add(ItemStack.EMPTY);
        if (state.expanded)
        {
            int totalSlots = Configs.ITEM_SEARCH_HISTORY_ROWS.getIntegerValue() * COLUMNS;
            List<ItemStack> history = ItemSearchHistoryService.getInstance().get(Channel.CREATIVE);
            int historyLimit = Math.min(history.size(), totalSlots - 1);
            for (int index = 0; index < historyLimit; index++)
            {
                target.add(history.get(index).copy());
            }
            while (target.size() < totalSlots)
            {
                target.add(ItemStack.EMPTY);
            }
        }
        state.source.forEach(stack -> target.add(stack.copy()));
    }

    private static List<ItemStack> copy(List<ItemStack> source)
    {
        return source.stream().map(ItemStack::copy).toList();
    }

    private static void replace(NonNullList<ItemStack> target, List<ItemStack> source)
    {
        target.clear();
        source.forEach(stack -> target.add(stack.copy()));
    }

    private static final class State
    {
        private final List<ItemStack> source;
        private boolean expanded;

        private State(List<ItemStack> source, boolean expanded)
        {
            this.source = source;
            this.expanded = expanded;
        }
    }
}
