package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif

import com.mojang.blaze3d.platform.InputConstants;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;

/**
 * Combined key binding center: a scrollable searchable list of every binding
 * (vanilla key mappings plus masa-family malilib hotkeys), an on-screen
 * full-size keyboard whose keys filter the list (Ctrl+click to stack several
 * keys) and show which bindings sit on each key, and one-click access to the
 * detail editor for rebinding and wheel customization.
 */
public final class KeymapBrowserScreen extends GuiBase
{
    private static final int ROW_HEIGHT = 18;
    private static final int KEY_COLUMN_WIDTH = 110;
    private static final int HEADER_HEIGHT = 48;
    private static final int KEYBOARD_HEIGHT = KeymapKeyboardLayout.HEIGHT;
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_ROW_HEIGHT = 14;
    private static final int FOLD_COLUMN_WIDTH = 18;
    private static final int SINGLE_CONFLICT_FILL = 0xB8783838;
    private static final int COMBINATION_CONFLICT_FILL = 0xB0401414;
    private static final int SINGLE_CONFLICT_BORDER = 0xFFE09090;
    private static final int COMBINATION_CONFLICT_BORDER = 0xFFB04040;

    private final List<BrowserEntry> allEntries = new ArrayList<>();
    private final List<BrowserEntry> visibleEntries = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<KeymapKeyboardLayout.Key> keyCells = new ArrayList<>();
    private final Set<Integer> selectedCombo = new HashSet<>();
    private String search = "";
    private int categoryIndex;
    private boolean showKeyboard = true;
    private boolean showConflictsOnly;
    private final Set<String> collapsedGroups = new HashSet<>();
    private boolean categoryPanelOpen;
    private int categoryPanelScroll;
    private int categoryPanelX;
    private int categoryPanelY;
    private int categoryPanelRows;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarX;
    private int scrollbarTrackTop;
    private int scrollbarTrackBottom;
    private int scrollbarMaxOffset;
    private int scrollbarVisibleCount;

    public KeymapBrowserScreen()
    {
        this.setTitle(StringUtils.translate("halfmasa.gui.keymap_browser.title"));
    }

    private enum ConflictType
    {
        NONE,
        SINGLE,
        COMBINATION
    }

    private record ConflictKinds(boolean single, boolean combination)
    {
        boolean any()
        {
            return this.single || this.combination;
        }

        boolean both()
        {
            return this.single && this.combination;
        }
    }

    public record BrowserEntry(KeyMapping mapping, IHotkey hotkey, String modName, String category,
            String action, String displayName, String keyText, ConflictType conflictType)
    {
        boolean isVanilla()
        {
            return this.mapping != null;
        }

        boolean conflicted()
        {
            return this.conflictType != ConflictType.NONE;
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        this.rebuildEntries();
        this.refilter(false);

        int controlsY = 26;
        int rightMargin = 10;
        int keyboardButtonWidth = 86;
        int conflictButtonWidth = 112;
        int categoryButtonWidth = 190;
        int keyboardX = this.getScreenWidth() - keyboardButtonWidth - rightMargin;
        int conflictX = keyboardX - conflictButtonWidth - 6;
        int categoryX = conflictX - categoryButtonWidth - 6;
        int foldAllX = categoryX - 88;
        int searchX = 28;
        int searchWidth = Math.min(300, Math.max(120, foldAllX - searchX - 12));

        GuiTextFieldGeneric searchField = new GuiTextFieldGeneric(searchX, controlsY, searchWidth, 16, this.mc.font);
        searchField.setTextWrapper(this.search);
        searchField.setMaxLengthWrapper(64);
        this.addTextField(searchField, field -> {
            String value = field.getTextWrapper();
            if (!value.equals(this.search))
            {
                this.search = value;
                this.refilter();
            }
            return true;
        });

        this.addButton(new ButtonGeneric(categoryX, controlsY - 2, categoryButtonWidth, 20,
                this.categoryButtonLabel()), (button, mouseButton) -> {
                    this.categoryPanelOpen = !this.categoryPanelOpen;
                    this.categoryPanelScroll = 0;
                });
        this.addButton(new ButtonGeneric(keyboardX, controlsY - 2, keyboardButtonWidth, 20,
                StringUtils.translate(this.showKeyboard
                        ? "halfmasa.gui.keymap_browser.keyboard_hide"
                        : "halfmasa.gui.keymap_browser.keyboard_show")),
                (button, mouseButton) -> {
                    this.showKeyboard = !this.showKeyboard;
                    this.initGui();
                });
        this.addButton(new ButtonGeneric(conflictX, controlsY - 2, conflictButtonWidth, 20,
                StringUtils.translate(this.showConflictsOnly
                        ? "halfmasa.gui.keymap_browser.show_all"
                        : "halfmasa.gui.keymap_browser.conflicts_only")),
                (button, mouseButton) -> {
                    this.showConflictsOnly = !this.showConflictsOnly;
                    this.refilter();
                    this.initGui();
                });
        this.addButton(new ButtonGeneric(foldAllX, controlsY - 2, 82, 20,
                StringUtils.translate(this.allGroupsCollapsed()
                        ? "halfmasa.gui.keymap_browser.expand_all"
                        : "halfmasa.gui.keymap_browser.fold_all")),
                (button, mouseButton) -> {
                    if (this.allGroupsCollapsed())
                    {
                        this.collapsedGroups.clear();
                    }
                    else
                    {
                        this.collapsedGroups.clear();
                        for (BrowserEntry entry : this.visibleEntries)
                        {
                            this.collapsedGroups.add(this.groupKey(entry));
                        }
                    }
                    this.scrollOffset = 0;
                    this.initGui();
                });

        int bottom = this.getScreenHeight() - 28;
        this.addButton(new ButtonGeneric(10, bottom, 110, 20,
                StringUtils.translate("halfmasa.gui.keymap_browser.open_editor")),
                (button, mouseButton) -> GuiBase.openGui(new KeybindCustomizationScreen()));
        this.addButton(new ButtonGeneric(126, bottom, 74, 20,
                StringUtils.translate("halfmasa.gui.keymap_browser.done")),
                (button, mouseButton) -> this.onClose());
    }

    private void rebuildEntries()
    {
        this.allEntries.clear();
        this.categories.clear();

        List<KeyMapping> mappings = Arrays.stream(this.mc.options.keyMappings).toList();

        List<BrowserEntry> entries = new ArrayList<>();
        for (KeyMapping mapping : mappings)
        {
            KeybindCustomizationStore.Entry custom =
                    KeybindCustomizationStore.getInstance().get(mapping);
            boolean customCombo = custom.comboKeys != null && !custom.comboKeys.isEmpty();
            boolean unbound = mapping.isUnbound() && !customCombo;
            String keyText = customCombo ? this.keyListText(custom.comboKeys) : (unbound ? "" : keyText(mapping));
            String category = specialCategory(categoryLabel(mapping));
            String action = Component.translatable(mapping.getName()).getString();
            String displayName = custom != null && custom.displayName != null && !custom.displayName.isBlank()
                    ? custom.displayName : null;
            entries.add(new BrowserEntry(mapping, null, category, category,
                    action, displayName, keyText, ConflictType.NONE));
        }
        for (KeybindCategory category : InputEventHandler.getKeybindManager().getKeybindCategories())
        {
            for (IHotkey hotkey : category.getHotkeys())
            {
                String display = hotkey.getKeybind().getKeysDisplayString();
                String name = translatedHotkeyName(hotkey);
                entries.add(new BrowserEntry(null, hotkey, category.getModName(), category.getCategory(),
                        name, null, display == null ? "" : display, ConflictType.NONE));
            }
        }

        Map<Integer, Integer> keyUses = new HashMap<>();
        for (BrowserEntry entry : entries)
        {
            Set<Integer> keys = Set.copyOf(this.entryKeySet(entry));
            if (keys.isEmpty())
            {
                continue;
            }
            for (int code : keys)
            {
                keyUses.merge(code, 1, Integer::sum);
            }
        }

        entries.sort(Comparator
                .comparing((BrowserEntry entry) -> entry.modName().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> entry.action().toLowerCase(Locale.ROOT)));
        for (BrowserEntry entry : entries)
        {
            Set<Integer> keys = Set.copyOf(this.entryKeySet(entry));
            ConflictType conflictType = ConflictType.NONE;
            boolean sharesKey = keys.stream().anyMatch(code -> keyUses.getOrDefault(code, 0) > 1);
            if (sharesKey && keys.size() > 1)
            {
                conflictType = ConflictType.COMBINATION;
            }
            else if (sharesKey)
            {
                conflictType = ConflictType.SINGLE;
            }
            this.allEntries.add(new BrowserEntry(entry.mapping(), entry.hotkey(), entry.modName(), entry.category(),
                    entry.action(), entry.displayName(), entry.keyText(), conflictType));
        }
        for (BrowserEntry entry : this.allEntries)
        {
            if (!this.categories.contains(entry.modName()))
            {
                this.categories.add(entry.modName());
            }
        }
    }

    private static String categoryLabel(KeyMapping mapping)
    {
        //#if MC >= 1.21.10
        return mapping.getCategory().label().getString();
        //#else
        //$$ return Component.translatable(mapping.getCategory()).getString();
        //#endif
    }

    private static String translatedHotkeyName(IHotkey hotkey)
    {
        String translated = hotkey.getTranslatedName();
        if (translated != null && !translated.isBlank() && !translated.equals(hotkey.getName()))
        {
            return translated;
        }
        String prettyName = hotkey.getPrettyName();
        return prettyName == null || prettyName.isEmpty() ? hotkey.getName() : prettyName;
    }

    private boolean isDebugOnlyMapping(KeyMapping mapping)
    {
        //#if MC >= 26.2
        if (this.mc.options != null && this.mc.options.debugKeys != null)
        {
            for (KeyMapping debugKey : this.mc.options.debugKeys)
            {
                if (debugKey == mapping && mapping != this.mc.options.keyDebugModifier)
                {
                    return true;
                }
            }
        }
        //#endif
        return false;
    }

    /**
     * JEI registers a separate binding category per feature group; fold them
     * all into one JEI category so its bindings stay together in the browser.
     */
    private static String specialCategory(String label)
    {
        if (label.toUpperCase(Locale.ROOT).startsWith("JEI"))
        {
            return "JEI";
        }
        return label;
    }

    private String categoryButtonLabel()
    {
        String current = this.categoryIndex <= 0
                ? StringUtils.translate("halfmasa.gui.keymap_browser.category_all")
                : this.categories.get(this.categoryIndex - 1);
        return StringUtils.translate("halfmasa.gui.keymap_browser.category") + ": " + current;
    }

    private void refilter()
    {
        this.refilter(true);
    }

    private void refilter(boolean resetScroll)
    {
        String query = this.search.trim().toLowerCase(Locale.ROOT);
        this.visibleEntries.clear();
        for (BrowserEntry entry : this.allEntries)
        {
            if (this.categoryIndex > 0 && !this.categories.get(this.categoryIndex - 1).equals(entry.modName()))
            {
                continue;
            }
            if (this.showConflictsOnly && !entry.conflicted())
            {
                continue;
            }
            if (!this.selectedCombo.isEmpty() && !this.entryKeySet(entry).containsAll(this.selectedCombo))
            {
                continue;
            }
            if (!query.isEmpty() &&
                !entry.action().toLowerCase(Locale.ROOT).contains(query) &&
                !entry.modName().toLowerCase(Locale.ROOT).contains(query) &&
                !entry.keyText().toLowerCase(Locale.ROOT).contains(query) &&
                !entryId(entry).toLowerCase(Locale.ROOT).contains(query))
            {
                continue;
            }
            this.visibleEntries.add(entry);
        }
        if (resetScroll)
        {
            this.scrollOffset = 0;
        }
    }

    private List<BrowserEntry> displayedEntries()
    {
        if (this.collapsedGroups.isEmpty())
        {
            return this.visibleEntries;
        }
        List<BrowserEntry> result = new ArrayList<>();
        Set<String> visibleGroups = new HashSet<>();
        for (BrowserEntry entry : this.visibleEntries)
        {
            String group = this.groupKey(entry);
            if (!this.collapsedGroups.contains(group) || visibleGroups.add(group))
            {
                result.add(entry);
            }
        }
        return result;
    }

    private String groupKey(BrowserEntry entry)
    {
        return entry.modName();
    }

    private boolean allGroupsCollapsed()
    {
        if (this.visibleEntries.isEmpty())
        {
            return false;
        }
        Set<String> groups = new HashSet<>();
        for (BrowserEntry entry : this.visibleEntries)
        {
            groups.add(this.groupKey(entry));
        }
        return !groups.isEmpty() && this.collapsedGroups.containsAll(groups);
    }

    private boolean isGroupStart(List<BrowserEntry> rows, int index)
    {
        return index == 0 || !this.groupKey(rows.get(index - 1)).equals(this.groupKey(rows.get(index)));
    }

    private void toggleGroup(String group)
    {
        if (!this.collapsedGroups.add(group))
        {
            this.collapsedGroups.remove(group);
        }
        this.initGui();
    }


    private static String wheelContextAbbrev(KeyMapping mapping)
    {
        KeybindCustomizationStore.Entry data =
                KeybindCustomizationStore.getInstance().get(mapping);
        return switch (data.activationContext)
        {
            case AUTO -> "轮";
            case GAMEPLAY -> "游";
            case SCREEN -> "界";
            case ANY -> "任";
            case DISABLED -> "禁";
        };
    }

    private static String entryId(BrowserEntry entry)
    {
        return entry.isVanilla() ? entry.mapping().getName() : entry.hotkey().getName();
    }

    private static int vanillaKeyCode(KeyMapping mapping)
    {
        return ((KeyMappingAccessor) mapping).halfmasa$getBoundKey().getValue();
    }

    private static int vanillaMouseCode(KeyMapping mapping)
    {
        return ((KeyMappingAccessor) mapping).halfmasa$getBoundKey().getValue();
    }

    private String actionWithKey(BrowserEntry entry)
    {
        String action = entry.displayName() != null ? entry.displayName() : entry.action();
        if (entry.keyText().isEmpty())
        {
            return action;
        }
        return action + " | " + entry.keyText();
    }

    private String keyText(KeyMapping mapping)
    {
        String keyText = mapping.getTranslatedKeyMessage().getString();
        //#if MC >= 26.2
        if (isDebugOnlyMapping(mapping) && this.mc.options != null && this.mc.options.keyDebugModifier != null)
        {
            InputConstants.Key debugModifier =
                    ((KeyMappingAccessor) this.mc.options.keyDebugModifier).halfmasa$getBoundKey();
            if (debugModifier.getType() == InputConstants.Type.KEYSYM)
            {
                keyText = debugModifier.getDisplayName().getString() + " + " + keyText;
            }
        }
        //#endif
        return keyText;
    }

    private String keyListText(List<Integer> keys)
    {
        List<String> names = new ArrayList<>();
        for (int code : keys)
        {
            names.add(code < 0
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1))
                    : InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString());
        }
        return String.join(" + ", names);
    }

    private String fitActionText(BrowserEntry entry, String fullAction, int maxWidth)
    {
        if (this.mc.font.width(fullAction) <= maxWidth)
        {
            return fullAction;
        }
        String action = entry.displayName() != null ? entry.displayName() : entry.action();
        return this.mc.font.plainSubstrByWidth(action, maxWidth);
    }

    private String[] keyboardLegendLabels()
    {
        return new String[] {
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.vanilla"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.both"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.malilib"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.unbound"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.selected"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.single"),
                StringUtils.translate("halfmasa.gui.keymap_browser.legend.combination")
        };
    }

    private int[] keyboardLegendColors()
    {
        return new int[] {
                0x66203850,
                0x66205038,
                0x66285028,
                0x66202028,
                0x90605820,
                SINGLE_CONFLICT_FILL,
                COMBINATION_CONFLICT_FILL
        };
    }

    private int[] keyboardLegendBorders()
    {
        return new int[] {
                0xFF6090C0,
                0xFF70B880,
                0xFFB88050,
                0xFF707078,
                0xFFF0D080,
                SINGLE_CONFLICT_BORDER,
                COMBINATION_CONFLICT_BORDER
        };
    }

    private int keyboardLegendHeight()
    {
        int width = Math.max(1, this.getScreenWidth() - 20);
        int cursor = 0;
        int rows = 1;
        String[] labels = this.keyboardLegendLabels();
        for (int index = 0; index < labels.length; index++)
        {
            int itemWidth = Math.min(width, this.mc.font.width(labels[index]) + 28);
            int gap = index == 6 && cursor > 0 ? 12 : 0;
            if (cursor > 0 && cursor + gap + itemWidth > width)
            {
                rows++;
                cursor = 0;
                gap = 0;
            }
            cursor += gap + itemWidth;
        }
        return rows * 12 + 4;
    }

    //#if MC >= 1.21.11
    private void drawKeyboardLegend(GuiContext graphics, int x, int y, int width)
    //#else
    //$$ private void drawKeyboardLegend(GuiGraphics graphics, int x, int y, int width)
    //#endif
    {
        int swatchSize = 8;
        int cursor = 0;
        int row = 0;
        String[] labels = this.keyboardLegendLabels();
        int[] colors = this.keyboardLegendColors();
        int[] borders = this.keyboardLegendBorders();
        for (int index = 0; index < labels.length; index++)
        {
            int itemWidth = Math.min(width, this.mc.font.width(labels[index]) + 28);
            int gap = index == 6 && cursor > 0 ? 12 : 0;
            if (cursor > 0 && cursor + gap + itemWidth > width)
            {
                row++;
                cursor = 0;
                gap = 0;
            }
            int itemX = x + cursor + gap;
            int itemY = y + row * 12;
            this.drawRect(graphics, itemX, itemY, itemX + swatchSize, itemY + swatchSize, borders[index]);
            this.drawRect(graphics, itemX + 1, itemY + 1, itemX + swatchSize - 1,
                    itemY + swatchSize - 1, colors[index]);
            int textWidth = Math.max(1, Math.min(this.mc.font.width(labels[index]), width - cursor - swatchSize - 4));
            String label = this.mc.font.plainSubstrByWidth(labels[index], textWidth);
            this.drawString(graphics, label, itemX + swatchSize + 4, itemY + 1, 0xFFD8D8D8);
            cursor += gap + itemWidth;
        }
    }

    //#if MC >= 1.21.11
    private void drawMagnifier(GuiContext graphics, int x, int y)
    //#else
    //$$ private void drawMagnifier(GuiGraphics graphics, int x, int y)
    //#endif
    {
        int color = 0xFFC0C0C0;
        // ring
        this.drawRect(graphics, x + 1, y, x + 5, y + 1, color);
        this.drawRect(graphics, x + 1, y + 6, x + 5, y + 7, color);
        this.drawRect(graphics, x, y + 1, x + 1, y + 6, color);
        this.drawRect(graphics, x + 5, y + 1, x + 6, y + 6, color);
        // handle
        this.drawRect(graphics, x + 6, y + 6, x + 7, y + 7, color);
        this.drawRect(graphics, x + 7, y + 7, x + 9, y + 9, color);
    }

    private int listTop()
    {
        int listTop = HEADER_HEIGHT + (this.showKeyboard ? KEYBOARD_HEIGHT : 0);
        if (this.showKeyboard)
        {
            listTop += this.keyboardLegendHeight() + 3;
        }
        if (!this.selectedCombo.isEmpty())
        {
            listTop += 12;
        }
        return listTop;
    }

    private String keyFilterLabel()
    {
        List<String> labels = new ArrayList<>();
        for (int code : this.selectedCombo)
        {
            if (code < 0)
            {
                labels.add(StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1)));
            }
            else
            {
                labels.add(keyLabel(code));
            }
        }
        return StringUtils.translate("halfmasa.gui.keymap_browser.key_filter",
                String.join(" + ", labels));
    }

    /**
     * The full set of keys a binding sits on, in one combined code space;
     * mouse keys are stored as negative codes so both kinds share one set.
     */
    private java.util.Set<Integer> entryKeySet(BrowserEntry entry)
    {
        java.util.Set<Integer> keys = new HashSet<>();
        if (entry.isVanilla())
        {
            List<Integer> customCombo = KeybindCustomizationStore.getInstance().comboKeys(entry.mapping());
            if (!customCombo.isEmpty())
            {
                keys.addAll(customCombo);
            }
            else if (!entry.mapping().isUnbound())
            {
                InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
                keys.add(key.getType() == InputConstants.Type.MOUSE ? -(key.getValue() + 1) : key.getValue());
                //#if MC >= 26.2
                if (isDebugOnlyMapping(entry.mapping()) && this.mc.options != null &&
                    this.mc.options.keyDebugModifier != null)
                {
                    InputConstants.Key debugModifier =
                            ((KeyMappingAccessor) this.mc.options.keyDebugModifier).halfmasa$getBoundKey();
                    if (debugModifier.getType() == InputConstants.Type.KEYSYM)
                    {
                        keys.add(debugModifier.getValue());
                    }
                }
                //#endif
            }
        }
        else
        {
            for (int code : entry.hotkey().getKeybind().getKeys())
            {
                keys.add(code);
            }
        }
        return keys;
    }

    //#if MC >= 1.21.11
    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        int listTop = this.listTop();
        int listBottom = this.getScreenHeight() - 32;
        List<BrowserEntry> rows = this.displayedEntries();
        int visibleCount = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        int maxOffset = Math.max(0, rows.size() - visibleCount);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int x = 10;
        int width = this.getScreenWidth() - 20;
        int contentWidth = width - 14;

        this.drawString(graphics,
                StringUtils.translate("halfmasa.gui.keymap_browser.count",
                        this.displayedEntries().size(), this.allEntries.size()),
                x + width - 120, 12, 0xFFC0C0C0);
        this.drawMagnifier(graphics, 12, 30);

        if (this.showKeyboard)
        {
            this.drawKeyboard(graphics, x, HEADER_HEIGHT, width, mouseX, mouseY);
            this.drawKeyboardLegend(graphics, x, HEADER_HEIGHT + KEYBOARD_HEIGHT + 3, width);
        }
        if (!this.selectedCombo.isEmpty())
        {
            this.drawString(graphics, this.keyFilterLabel(), x, listTop - 12, 0xFFFFC860);
        }

        for (int index = 0; index < visibleCount; index++)
        {
            int entryIndex = this.scrollOffset + index;
            if (entryIndex >= rows.size())
            {
                break;
            }
            int y = listTop + index * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + contentWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered)
            {
                this.drawRect(graphics, x, y, x + contentWidth, y + ROW_HEIGHT, 0x30FFFFFF);
            }

            BrowserEntry entry = rows.get(entryIndex);
            int foldX = x;
            int rowX = x + FOLD_COLUMN_WIDTH;
            int rowWidth = contentWidth - FOLD_COLUMN_WIDTH;
            boolean groupStart = this.isGroupStart(rows, entryIndex);
            if (groupStart)
            {
                this.drawRect(graphics, foldX + 2, y + 2, foldX + 14, y + ROW_HEIGHT - 2, 0xB0202028);
                this.drawRect(graphics, foldX + 2, y + 2, foldX + 14, y + 3, 0xFF909098);
                this.drawRect(graphics, foldX + 2, y + ROW_HEIGHT - 3, foldX + 14, y + ROW_HEIGHT - 2, 0xFF909098);
                this.drawRect(graphics, foldX + 2, y + 2, foldX + 3, y + ROW_HEIGHT - 2, 0xFF909098);
                this.drawRect(graphics, foldX + 13, y + 2, foldX + 14, y + ROW_HEIGHT - 2, 0xFF909098);
                String marker = this.collapsedGroups.contains(this.groupKey(entry)) ? "+" : "-";
                this.drawString(graphics, marker, foldX + 5, y + 5, 0xFFFFFFFF);
            }

            int categoryWidth = Math.max(90, rowWidth / 4);
            String category = this.mc.font.plainSubstrByWidth(entry.modName(), categoryWidth - 6);
            this.drawString(graphics, category, rowX + 2, y + 5, 0xFF808080);

            int actionX = rowX + categoryWidth;
            int contextX = rowX + rowWidth - 30;
            int actionRight = entry.isVanilla() ? contextX : rowX + rowWidth;
            int actionWidth = Math.max(1, actionRight - actionX - 8);
            String fullAction = this.actionWithKey(entry);
            String action = this.fitActionText(entry, fullAction, actionWidth);
            this.drawString(graphics, action, actionX, y + 5, 0xFFFFFFFF);

            if (entry.isVanilla())
            {
                String contextAbbrev = wheelContextAbbrev(entry.mapping());
                boolean ctxHovered = hovered && mouseX >= contextX - 2 && mouseX < contextX + 30;
                if (ctxHovered)
                {
                    this.drawRect(graphics, contextX - 2, y, contextX + 30, y + ROW_HEIGHT, 0x40FFFFFF);
                }
                this.drawString(graphics, contextAbbrev, contextX, y + 5,
                        "禁".equals(contextAbbrev) ? 0xFFE07070 : 0xFF90C890);
            }

            if (hovered && (entry.conflicted() || !entry.keyText().isEmpty() && !action.equals(fullAction)))
            {
                this.drawEntryTooltip(graphics, entry, mouseX, mouseY);
            }
        }

        this.scrollbarX = x + width - 7;
        this.scrollbarTrackTop = listTop;
        this.scrollbarTrackBottom = listBottom;
        this.scrollbarMaxOffset = maxOffset;
        this.scrollbarVisibleCount = visibleCount;
        if (rows.size() > visibleCount)
        {
            int trackX = this.scrollbarX;
            this.drawRect(graphics, trackX, listTop, trackX + 2, listBottom, 0x30FFFFFF);
            int trackHeight = listBottom - listTop;
            int thumbHeight = Math.max(12, trackHeight * visibleCount / rows.size());
            int thumbY = listTop + (trackHeight - thumbHeight) * this.scrollOffset / Math.max(1, maxOffset);
            this.drawRect(graphics, trackX, thumbY, trackX + 2, thumbY + thumbHeight, this.draggingScrollbar
                    ? 0xFFD0C090 : 0x90FFFFFF);
        }
        if (this.draggingScrollbar && this.scrollbarMaxOffset > 0)
        {
            int trackHeight = this.scrollbarTrackBottom - this.scrollbarTrackTop;
            int thumbHeight = Math.max(12, trackHeight * this.scrollbarVisibleCount / rows.size());
            int grab = mouseY - this.scrollbarTrackTop - thumbHeight / 2;
            this.scrollOffset = Math.max(0, Math.min(this.scrollbarMaxOffset,
                    grab * this.scrollbarMaxOffset / Math.max(1, trackHeight - thumbHeight)));
        }

        if (this.categoryPanelOpen)
        {
            this.drawCategoryPanel(graphics, mouseX, mouseY);
        }
    }

    //#if MC >= 1.21.11
    private void drawCategoryPanel(GuiContext graphics, int mouseX, int mouseY)
    //#else
    //$$ private void drawCategoryPanel(GuiGraphics graphics, int mouseX, int mouseY)
    //#endif
    {
        this.categoryPanelX = this.getScreenWidth() - PANEL_WIDTH - 106;
        this.categoryPanelY = 46;
        int visibleRows = (this.getScreenHeight() - this.categoryPanelY - 40) / PANEL_ROW_HEIGHT;
        int totalRows = this.categories.size() + 1;
        this.categoryPanelRows = Math.max(3, Math.min(visibleRows, totalRows));
        int maxScroll = Math.max(0, totalRows - this.categoryPanelRows);
        this.categoryPanelScroll = Math.max(0, Math.min(this.categoryPanelScroll, maxScroll));

        int height = this.categoryPanelRows * PANEL_ROW_HEIGHT + 6;
        this.drawRect(graphics, this.categoryPanelX - 1, this.categoryPanelY - 1,
                this.categoryPanelX + PANEL_WIDTH + 1, this.categoryPanelY + height + 1, 0xFF606068);
        this.drawRect(graphics, this.categoryPanelX, this.categoryPanelY,
                this.categoryPanelX + PANEL_WIDTH, this.categoryPanelY + height, 0xF0181820);

        for (int index = 0; index < this.categoryPanelRows; index++)
        {
            int row = this.categoryPanelScroll + index;
            String label;
            boolean active;
            if (row == 0)
            {
                label = StringUtils.translate("halfmasa.gui.keymap_browser.category_all");
                active = this.categoryIndex == 0;
            }
            else if (row <= this.categories.size())
            {
                label = this.categories.get(row - 1);
                active = this.categoryIndex == row;
            }
            else
            {
                break;
            }
            int y = this.categoryPanelY + 3 + index * PANEL_ROW_HEIGHT;
            boolean hovered = mouseX >= this.categoryPanelX && mouseX < this.categoryPanelX + PANEL_WIDTH &&
                    mouseY >= y && mouseY < y + PANEL_ROW_HEIGHT;
            if (hovered)
            {
                this.drawRect(graphics, this.categoryPanelX, y - 1,
                        this.categoryPanelX + PANEL_WIDTH, y + PANEL_ROW_HEIGHT - 1, 0x40FFFFFF);
            }
            label = this.mc.font.plainSubstrByWidth(label, PANEL_WIDTH - 16);
            this.drawString(graphics, label, this.categoryPanelX + 6, y,
                    active ? 0xFFFFC860 : 0xFFE0E0E0);
        }
    }

    //#if MC >= 1.21.11
    private void drawKeyboard(GuiContext graphics, int x, int y, int width, int mouseX, int mouseY)
    //#else
    //$$ private void drawKeyboard(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY)
    //#endif
    {
        this.keyCells.clear();
        for (KeymapKeyboardLayout.Key key : KeymapKeyboardLayout.keys(x, y, width))
        {
            this.drawKeyCell(graphics, key, mouseX, mouseY);
        }
    }

    //#if MC >= 1.21.11
    private void drawKeyCell(GuiContext graphics, KeymapKeyboardLayout.Key key, int mouseX, int mouseY)
    //#else
    //$$ private void drawKeyCell(GuiGraphics graphics, KeymapKeyboardLayout.Key key, int mouseX, int mouseY)
    //#endif
    {
        int cellCode = key.code();
        boolean mouseKey = key.mouse();
        int displayCode = mouseKey ? -cellCode - 1 : cellCode;
        int left = key.x();
        int right = left + key.width();
        int y = key.y();
        int cellWidth = key.width();
        int cellHeight = key.height();
        {
            boolean hasVanilla;
            boolean hasMalilib = false;
            if (mouseKey)
            {
                hasVanilla = this.countVanillaMouse(displayCode) > 0;
            }
            else
            {
                hasVanilla = this.countVanillaKeyboard(cellCode) > 0;
                hasMalilib = this.countMalilib(cellCode) > 0;
            }
            boolean selected = this.selectedCombo.contains(cellCode);
            ConflictKinds conflictKinds = this.keyConflictKinds(cellCode);

            int sourceFill = 0x66202028;
            if (hasVanilla && hasMalilib)
            {
                sourceFill = 0x66205038;
            }
            else if (hasVanilla)
            {
                sourceFill = 0x66203850;
            }
            else if (hasMalilib)
            {
                sourceFill = 0x66285028;
            }

            int split = left + cellWidth / 2;
            if (selected)
            {
                this.drawRect(graphics, left, y, right, y + cellHeight, 0x90605820);
            }
            else if (conflictKinds.any())
            {
                this.drawRect(graphics, left, y, split, y + cellHeight, sourceFill);
                if (conflictKinds.both())
                {
                    int conflictSplitY = y + cellHeight / 2;
                    this.drawRect(graphics, split, y, right, conflictSplitY,
                            SINGLE_CONFLICT_FILL);
                    this.drawRect(graphics, split, conflictSplitY, right, y + cellHeight,
                            COMBINATION_CONFLICT_FILL);
                }
                else
                {
                    this.drawRect(graphics, split, y, right, y + cellHeight,
                            conflictKinds.combination() ? COMBINATION_CONFLICT_FILL : SINGLE_CONFLICT_FILL);
                }
            }
            else
            {
                this.drawRect(graphics, left, y, right, y + cellHeight, sourceFill);
            }

            int sourceBorder = selected ? 0xFFF0D080 : (hasVanilla || hasMalilib) ? 0xFF787888 : 0xFF3C3C46;
            this.drawRect(graphics, left, y, right, y + 1, sourceBorder);
            this.drawRect(graphics, left, y + cellHeight - 1, right, y + cellHeight, sourceBorder);
            this.drawRect(graphics, left, y, left + 1, y + cellHeight, sourceBorder);
            this.drawRect(graphics, right - 1, y, right, y + cellHeight, sourceBorder);
            if (!selected && conflictKinds.any())
            {
                if (conflictKinds.both())
                {
                    int conflictSplitY = y + cellHeight / 2;
                    this.drawRect(graphics, split, y, right, y + 1, SINGLE_CONFLICT_BORDER);
                    this.drawRect(graphics, split, conflictSplitY - 1,
                            right, conflictSplitY + 1, COMBINATION_CONFLICT_BORDER);
                    this.drawRect(graphics, split, y + cellHeight - 1,
                            right, y + cellHeight, COMBINATION_CONFLICT_BORDER);
                }
                else
                {
                    int conflictBorder = conflictKinds.combination()
                            ? COMBINATION_CONFLICT_BORDER : SINGLE_CONFLICT_BORDER;
                    this.drawRect(graphics, split, y, right, y + 1, conflictBorder);
                    this.drawRect(graphics, split, y + cellHeight - 1, right, y + cellHeight, conflictBorder);
                }
                int rightBorder = conflictKinds.combination()
                        ? COMBINATION_CONFLICT_BORDER : SINGLE_CONFLICT_BORDER;
                this.drawRect(graphics, right - 1, y, right, y + cellHeight, rightBorder);
            }

            String label = mouseKey
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + displayCode)
                    : key.label();
            this.drawCenteredFittedKeyLabel(graphics, label, left, right, y, cellHeight, 0xFFE0E0E0);

            this.keyCells.add(key);
        }
    }

    //#if MC >= 1.21.11
    private void drawCenteredFittedKeyLabel(GuiContext graphics, String label, int left, int right,
            int y, int height, int color)
    //#else
    //$$ private void drawCenteredFittedKeyLabel(GuiGraphics graphics, String label, int left, int right,
    //$$         int y, int height, int color)
    //#endif
    {
        int textWidth = this.mc.font.width(label);
        int availableWidth = Math.max(1, right - left - 4);
        float scale = Math.min(1.0F, availableWidth / (float) Math.max(1, textWidth));
        float centerX = (left + right) / 2.0F;
        float centerY = y + height / 2.0F;
        //#if MC >= 1.21.8
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        this.drawString(graphics, label, -textWidth / 2, -4, color);
        graphics.pose().popMatrix();
        //#else
        //$$ graphics.pose().pushPose();
        //$$ graphics.pose().translate(centerX, centerY, 0.0F);
        //$$ graphics.pose().scale(scale, scale, 1.0F);
        //$$ this.drawString(graphics, label, -textWidth / 2, -4, color);
        //$$ graphics.pose().popPose();
        //#endif
    }

    private int countVanillaKeyboard(int code)
    {
        int count = 0;
        for (BrowserEntry entry : this.allEntries)
        {
            if (entry.isVanilla() && !entry.mapping().isUnbound() &&
                ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey().getType() == InputConstants.Type.KEYSYM &&
                vanillaKeyCode(entry.mapping()) == code)
            {
                count++;
            }
        }
        return count;
    }

    private int countVanillaMouse(int code)
    {
        int count = 0;
        for (BrowserEntry entry : this.allEntries)
        {
            if (entry.isVanilla() && !entry.mapping().isUnbound() &&
                ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey().getType() == InputConstants.Type.MOUSE &&
                vanillaMouseCode(entry.mapping()) == code)
            {
                count++;
            }
        }
        return count;
    }

    private int countMalilib(int code)
    {
        int count = 0;
        for (BrowserEntry entry : this.allEntries)
        {
            if (!entry.isVanilla() && entry.hotkey().getKeybind().getKeys().contains(code))
            {
                count++;
            }
        }
        return count;
    }

    private ConflictKinds keyConflictKinds(int code)
    {
        boolean singleConflict = false;
        boolean combinationConflict = false;
        for (BrowserEntry entry : this.allEntries)
        {
            if (!this.entryKeySet(entry).contains(code))
            {
                continue;
            }
            if (entry.conflictType() == ConflictType.COMBINATION)
            {
                combinationConflict = true;
            }
            else if (entry.conflictType() == ConflictType.SINGLE)
            {
                singleConflict = true;
            }
        }
        return new ConflictKinds(singleConflict, combinationConflict);
    }

    private static String keyLabel(int code)
    {
        if (code >= 65 && code <= 90)
        {
            return String.valueOf((char) code);
        }
        if (code >= 48 && code <= 57)
        {
            return String.valueOf((char) code);
        }
        if (code >= 290 && code <= 301)
        {
            return "F" + (code - 289);
        }
        if (code >= 320 && code <= 329)
        {
            return "N" + (code - 320);
        }
        switch (code)
        {
            case 256: return "ESC";
            case 257: return "⏎";
            case 258: return "TAB";
            case 259: return "⌫";
            case 260: return "INS";
            case 261: return "DEL";
            case 262: return "→";
            case 263: return "←";
            case 264: return "↑";
            case 265: return "↓";
            case 266: return "PGU";
            case 267: return "PGD";
            case 268: return "HOM";
            case 269: return "END";
            case 280: return "CAPS";
            case 281: return "SCR";
            case 282: return "NUM";
            case 283: return "PRT";
            case 284: return "PAU";
            case 32: return "SPACE";
            case 330: return "N.";
            case 331: return "N/";
            case 332: return "N*";
            case 333: return "N-";
            case 334: return "N+";
            case 335: return "N⏎";
            case 340: return "L⇧";
            case 341: return "LCT";
            case 342: return "LAL";
            case 343: return "LWIN";
            case 344: return "R⇧";
            case 345: return "RCT";
            case 346: return "RAL";
            case 347: return "RWIN";
            case 348: return "MENU";
            default:
                String name = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
                return name.length() > 5 ? name.substring(0, 5) : name;
        }
    }

    //#if MC >= 1.21.11
    private void drawRect(GuiContext graphics, int x1, int y1, int x2, int y2, int color)
    //#else
    //$$ private void drawRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color)
    //#endif
    {
        if (x2 > x1 && y2 > y1)
        {
            graphics.fill(x1, y1, x2, y2, color);
        }
    }

    //#if MC >= 1.21.11
    private void drawEntryTooltip(GuiContext graphics, BrowserEntry entry, int mouseX, int mouseY)
    //#else
    //$$ private void drawEntryTooltip(GuiGraphics graphics, BrowserEntry entry, int mouseX, int mouseY)
    //#endif
    {
        List<String> lines = new ArrayList<>();
        if (entry.conflictType() == ConflictType.COMBINATION)
        {
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.legend.combination"));
        }
        else if (entry.conflictType() == ConflictType.SINGLE)
        {
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.legend.single"));
        }
        if (!entry.keyText().isEmpty())
        {
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.binding", entry.keyText()));
        }
        for (BrowserEntry other : this.orderedConflictEntries(entry))
        {
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.conflicts_with",
                    other.modName(), this.actionWithKey(other)));
        }
        if (lines.isEmpty())
        {
            return;
        }
        int width = 0;
        for (String line : lines)
        {
            width = Math.max(width, this.mc.font.width(line));
        }
        int x = Math.min(mouseX + 8, this.getScreenWidth() - width - 12);
        int y = Math.min(mouseY + 4, this.getScreenHeight() - lines.size() * 11 - 8);
        this.drawRect(graphics, x - 3, y - 2, x + width + 5, y + lines.size() * 11 + 2, 0xF0100018);
        for (int index = 0; index < lines.size(); index++)
        {
            this.drawString(graphics, lines.get(index), x, y + index * 11,
                    index == 0 ? 0xFFFFC860 : 0xFFF0F0F0);
        }
    }

    private List<Integer> orderedEntryKeys(BrowserEntry entry)
    {
        if (!entry.isVanilla())
        {
            return new ArrayList<>(entry.hotkey().getKeybind().getKeys());
        }
        List<Integer> customCombo = KeybindCustomizationStore.getInstance().comboKeys(entry.mapping());
        if (!customCombo.isEmpty())
        {
            return customCombo;
        }
        if (entry.mapping().isUnbound())
        {
            return List.of();
        }
        InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
        List<Integer> keys = new ArrayList<>();
        keys.add(key.getType() == InputConstants.Type.MOUSE ? -(key.getValue() + 1) : key.getValue());
        //#if MC >= 26.2
        if (this.isDebugOnlyMapping(entry.mapping()) && this.mc.options != null &&
                this.mc.options.keyDebugModifier != null)
        {
            InputConstants.Key modifier =
                    ((KeyMappingAccessor) this.mc.options.keyDebugModifier).halfmasa$getBoundKey();
            if (modifier.getType() == InputConstants.Type.KEYSYM)
            {
                keys.add(modifier.getValue());
            }
        }
        //#endif
        return keys;
    }

    private List<BrowserEntry> orderedConflictEntries(BrowserEntry entry)
    {
        List<Integer> keys = this.orderedEntryKeys(entry);
        if (keys.isEmpty())
        {
            return List.of();
        }
        List<BrowserEntry> result = new ArrayList<>();
        if (keys.size() == 1)
        {
            this.addConflictMatches(result, entry, other -> {
                List<Integer> otherKeys = this.orderedEntryKeys(other);
                return otherKeys.size() == 1 && otherKeys.get(0).equals(keys.get(0));
            });
            for (int position = 0; position < Math.min(3, keys.size() + 2); position++)
            {
                int keyPosition = position;
                this.addConflictMatches(result, entry, other -> {
                    List<Integer> otherKeys = this.orderedEntryKeys(other);
                    return otherKeys.size() > 1 && keyPosition < otherKeys.size() &&
                            otherKeys.get(keyPosition).equals(keys.get(0));
                });
            }
        }
        else
        {
            this.addConflictMatches(result, entry, other -> this.orderedEntryKeys(other).equals(keys));
            List<Integer> reversed = new ArrayList<>(keys);
            java.util.Collections.reverse(reversed);
            if (!reversed.equals(keys))
            {
                this.addConflictMatches(result, entry, other -> this.orderedEntryKeys(other).equals(reversed));
            }
            for (int key : keys)
            {
                this.addConflictMatches(result, entry, other -> {
                    List<Integer> otherKeys = this.orderedEntryKeys(other);
                    return otherKeys.size() == 1 && otherKeys.get(0) == key;
                });
            }
        }
        return result.size() > 8 ? new ArrayList<>(result.subList(0, 8)) : result;
    }

    private void addConflictMatches(List<BrowserEntry> result, BrowserEntry current,
            java.util.function.Predicate<BrowserEntry> predicate)
    {
        this.allEntries.stream()
                .filter(other -> other != current && predicate.test(other))
                .sorted(Comparator.comparing(other -> this.actionWithKey(other).toLowerCase(Locale.ROOT)))
                .forEach(other -> {
                    if (!result.contains(other) && result.size() < 8) result.add(other);
                });
    }

    //#if MC >= 1.21.10
    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (this.handleClick(event.x(), event.y(), event.button(), event.hasControlDown()))
        {
            return true;
        }
        return super.onMouseClicked(event, doubleClick);
    }
    //#else
    //$$ @Override
    //$$ public boolean onMouseClicked(int mouseX, int mouseY, int button)
    //$$ {
    //$$     if (this.handleClick(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasControlDown()))
    //$$     {
    //$$         return true;
    //$$     }
    //$$     return super.onMouseClicked(mouseX, mouseY, button);
    //$$ }
    //#endif

    private boolean handleClick(double mouseX, double mouseY, int button, boolean ctrlDown)
    {
        if (button != 0)
        {
            return false;
        }

        if (this.categoryPanelOpen)
        {
            int panelHeight = this.categoryPanelRows * PANEL_ROW_HEIGHT + 6;
            if (mouseX >= this.categoryPanelX && mouseX <= this.categoryPanelX + PANEL_WIDTH &&
                mouseY >= this.categoryPanelY && mouseY <= this.categoryPanelY + panelHeight)
            {
                int index = this.categoryPanelScroll +
                        (int) ((mouseY - this.categoryPanelY - 3) / PANEL_ROW_HEIGHT);
                if (index >= 0 && index <= this.categories.size())
                {
                    this.categoryIndex = index;
                    this.categoryPanelOpen = false;
                    this.refilter();
                    this.initGui();
                }
                return true;
            }
            // clicking anywhere else just closes the panel (including the
            // category button, so pressing it again toggles the panel shut)
            this.categoryPanelOpen = false;
            return true;
        }

        int listTop = this.listTop();
        int listBottom = this.getScreenHeight() - 32;

        if (this.showKeyboard && mouseY >= HEADER_HEIGHT && mouseY < listTop)
        {
            for (KeymapKeyboardLayout.Key cell : this.keyCells)
            {
                if (mouseX >= cell.x() && mouseX < cell.x() + cell.width() &&
                    mouseY >= cell.y() && mouseY < cell.y() + cell.height())
                {
                    int comboCode = cell.code();
                    if (ctrlDown)
                    {
                        if (this.selectedCombo.contains(comboCode))
                        {
                            this.selectedCombo.remove(comboCode);
                        }
                        else
                        {
                            this.selectedCombo.add(comboCode);
                        }
                    }
                    else if (this.selectedCombo.size() == 1 && this.selectedCombo.contains(comboCode))
                    {
                        this.selectedCombo.clear();
                    }
                    else
                    {
                        this.selectedCombo.clear();
                        this.selectedCombo.add(comboCode);
                    }
                    this.refilter();
                    return true;
                }
            }
        }

        if (mouseY >= this.scrollbarTrackTop && mouseY < this.scrollbarTrackBottom &&
            mouseX >= this.scrollbarX - 4 && mouseX < this.scrollbarX + 8)
        {
            this.draggingScrollbar = true;
            this.handleScrollbarDrag(mouseY);
            return true;
        }

        if (mouseY >= listTop && mouseY < listBottom && mouseX < this.scrollbarX - 4)
        {
            List<BrowserEntry> rows = this.displayedEntries();
            int index = this.scrollOffset + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (index >= 0 && index < rows.size())
            {
                BrowserEntry entry = rows.get(index);
                if (this.isGroupStart(rows, index) && mouseX >= 10 && mouseX < 10 + FOLD_COLUMN_WIDTH)
                {
                    this.toggleGroup(this.groupKey(entry));
                    return true;
                }
                // Ctrl+click on the row cycles its wheel activation context in place
                if (ctrlDown && entry.isVanilla())
                {
                    KeybindCustomizationStore.Entry data =
                            KeybindCustomizationStore.getInstance().get(entry.mapping());
                    data.activationContext = data.activationContext.next();
                    KeybindCustomizationStore.getInstance().save();
                    return true;
                }
                KeybindDetailScreen detail = new KeybindDetailScreen(entry);
                detail.setParent(this);
                GuiBase.openGui(detail);
                return true;
            }
        }
        return false;
    }

    private void handleScrollbarDrag(double mouseY)
    {
        List<BrowserEntry> rows = this.displayedEntries();
        int trackHeight = this.scrollbarTrackBottom - this.scrollbarTrackTop;
        int thumbHeight = rows.isEmpty() ? 12
                : Math.max(12, trackHeight * this.scrollbarVisibleCount / rows.size());
        int grab = (int) mouseY - this.scrollbarTrackTop - thumbHeight / 2;
        this.scrollOffset = Math.max(0, Math.min(this.scrollbarMaxOffset,
                grab * this.scrollbarMaxOffset / Math.max(1, trackHeight - thumbHeight)));
    }

    //#if MC >= 1.21.11
    @Override
    public boolean onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY)
    {
        if (this.draggingScrollbar)
        {
            this.handleScrollbarDrag(event.y());
            return true;
        }
        return super.onMouseDragged(event, deltaX, deltaY);
    }
    //#elseif MC >= 1.21.10
    //$$ @Override
    //$$ public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY)
    //$$ {
    //$$     if (this.draggingScrollbar)
    //$$     {
    //$$         this.handleScrollbarDrag(event.y());
    //$$         return true;
    //$$     }
    //$$     return super.mouseDragged(event, deltaX, deltaY);
    //$$ }
    //#endif

    //#if MC >= 1.21.10
    @Override
    public boolean onMouseReleased(MouseButtonEvent event)
    {
        this.draggingScrollbar = false;
        return super.onMouseReleased(event);
    }
    //#else
    //$$ @Override
    //$$ public boolean onMouseReleased(int mouseX, int mouseY, int button)
    //$$ {
    //$$     this.draggingScrollbar = false;
    //$$     return super.onMouseReleased(mouseX, mouseY, button);
    //$$ }
    //#endif

    //#if MC < 1.21.10
    //$$ @Override
    //$$ public boolean onMouseScrolled(int mouseX, int mouseY, double deltaX, double deltaY)
    //#else
    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
    //#endif
    {
        if (this.categoryPanelOpen &&
            mouseX >= this.categoryPanelX && mouseX <= this.categoryPanelX + PANEL_WIDTH &&
            mouseY >= this.categoryPanelY && mouseY < this.categoryPanelY +
                    this.categoryPanelRows * PANEL_ROW_HEIGHT)
        {
            this.categoryPanelScroll -= (int) Math.signum(deltaY) * 2;
            return true;
        }
        if (mouseY >= this.listTop())
        {
            //#if MC >= 1.21.10
            long windowHandle = Minecraft.getInstance().getWindow().handle();
            //#else
            //$$ long windowHandle = Minecraft.getInstance().getWindow().getWindow();
            //#endif
            boolean shiftDown = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle,
                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            boolean ctrlDown = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle,
                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            int rows = 3;
            if (ctrlDown && shiftDown)
            {
                rows = Math.max(1, Configs.FAST_SCROLLING_SECONDARY_MULTIPLIER.getIntegerValue()) * 3;
            }
            else if (ctrlDown)
            {
                rows = Math.max(1, Configs.FAST_SCROLLING_PRIMARY_MULTIPLIER.getIntegerValue()) * 3;
            }
            this.scrollOffset -= (int) Math.signum(deltaY) * rows;
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}
