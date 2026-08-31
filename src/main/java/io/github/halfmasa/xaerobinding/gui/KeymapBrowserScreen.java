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
    private static final int CELL_HEIGHT = 16;
    private static final int CELL_GAP = 2;
    private static final int KEY_COLUMN_WIDTH = 110;
    private static final int HEADER_HEIGHT = 48;
    private static final int KEYBOARD_ROWS = 6;
    private static final int KEYBOARD_HEIGHT = KEYBOARD_ROWS * (CELL_HEIGHT + CELL_GAP) + 8;
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_ROW_HEIGHT = 14;

    private final List<BrowserEntry> allEntries = new ArrayList<>();
    private final List<BrowserEntry> visibleEntries = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<KeyCell> keyCells = new ArrayList<>();
    private final Set<Integer> selectedCombo = new HashSet<>();
    private String search = "";
    private int categoryIndex;
    private boolean showKeyboard = true;
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

    public record BrowserEntry(KeyMapping mapping, IHotkey hotkey, String modName, String category,
            String action, String displayName, String keyText, boolean conflicted)
    {
        boolean isVanilla()
        {
            return this.mapping != null;
        }
    }

    private record KeyCell(int code, int x, int y, int width, int height, boolean mouse)
    {
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        this.rebuildEntries();

        int controlsY = 26;
        int rightMargin = 10;
        int keyboardButtonWidth = 86;
        int categoryButtonWidth = 190;
        int keyboardX = this.getScreenWidth() - keyboardButtonWidth - rightMargin;
        int categoryX = keyboardX - categoryButtonWidth - 6;
        int searchX = 28;
        int searchWidth = Math.min(300, Math.max(140, categoryX - searchX - 12));

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

        int bottom = this.getScreenHeight() - 28;
        this.addButton(new ButtonGeneric(10, bottom, 110, 20,
                StringUtils.translate("halfmasa.gui.keymap_browser.open_editor")),
                (button, mouseButton) -> GuiBase.openGui(new KeybindCustomizationScreen()));
        this.addButton(new ButtonGeneric(126, bottom, 74, 20,
                StringUtils.translate("halfmasa.gui.keymap_browser.done")),
                (button, mouseButton) -> this.onClose());

        this.refilter();
    }

    private void rebuildEntries()
    {
        this.allEntries.clear();
        this.categories.clear();

        Map<Integer, Integer> keyboardUses = new HashMap<>();
        Map<Integer, Integer> mouseUses = new HashMap<>();

        List<KeyMapping> mappings = Arrays.stream(this.mc.options.keyMappings).toList();
        for (KeyMapping mapping : mappings)
        {
            if (!mapping.isUnbound())
            {
                InputConstants.Key key = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
                if (key.getType() == InputConstants.Type.KEYSYM)
                {
                    keyboardUses.merge(key.getValue(), 1, Integer::sum);
                }
                else if (key.getType() == InputConstants.Type.MOUSE)
                {
                    mouseUses.merge(key.getValue(), 1, Integer::sum);
                }
            }
        }
        for (KeybindCategory category : InputEventHandler.getKeybindManager().getKeybindCategories())
        {
            for (IHotkey hotkey : category.getHotkeys())
            {
                for (int code : hotkey.getKeybind().getKeys())
                {
                    keyboardUses.merge(code, 1, Integer::sum);
                }
            }
        }

        List<BrowserEntry> entries = new ArrayList<>();
        for (KeyMapping mapping : mappings)
        {
            InputConstants.Key key = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
            boolean unbound = mapping.isUnbound();
            String keyText = unbound ? "" : mapping.getTranslatedKeyMessage().getString();
            boolean conflicted = !unbound &&
                    (key.getType() == InputConstants.Type.KEYSYM
                            ? keyboardUses.getOrDefault(key.getValue(), 0)
                            : mouseUses.getOrDefault(key.getValue(), 0)) > 1;
            String category = specialCategory(categoryLabel(mapping));
            KeybindCustomizationStore.Entry custom =
                    KeybindCustomizationStore.getInstance().get(mapping);
            String action = Component.translatable(mapping.getName()).getString();
            String displayName = custom != null && custom.displayName != null && !custom.displayName.isBlank()
                    ? custom.displayName : null;
            entries.add(new BrowserEntry(mapping, null, category, category,
                    action, displayName, keyText, conflicted));
        }
        for (KeybindCategory category : InputEventHandler.getKeybindManager().getKeybindCategories())
        {
            for (IHotkey hotkey : category.getHotkeys())
            {
                String display = hotkey.getKeybind().getKeysDisplayString();
                boolean conflicted = false;
                for (int code : hotkey.getKeybind().getKeys())
                {
                    if (keyboardUses.getOrDefault(code, 0) > 1)
                    {
                        conflicted = true;
                        break;
                    }
                }
                String name = hotkey.getPrettyName() == null || hotkey.getPrettyName().isEmpty()
                        ? hotkey.getName() : hotkey.getPrettyName();
                entries.add(new BrowserEntry(null, hotkey, category.getModName(), category.getCategory(),
                        name, null, display == null ? "" : display, conflicted));
            }
        }

        entries.sort(Comparator
                .comparing((BrowserEntry entry) -> entry.modName().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> entry.action().toLowerCase(Locale.ROOT)));
        this.allEntries.addAll(entries);
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
        String query = this.search.trim().toLowerCase(Locale.ROOT);
        this.visibleEntries.clear();
        for (BrowserEntry entry : this.allEntries)
        {
            if (this.categoryIndex > 0 && !this.categories.get(this.categoryIndex - 1).equals(entry.modName()))
            {
                continue;
            }
            if (!this.selectedCombo.isEmpty() && !this.entryKeySet(entry).equals(this.selectedCombo))
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
        this.scrollOffset = 0;
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
            if (!entry.mapping().isUnbound())
            {
                InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
                keys.add(key.getType() == InputConstants.Type.MOUSE ? -(key.getValue() + 1) : key.getValue());
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
        int visibleCount = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        int maxOffset = Math.max(0, this.visibleEntries.size() - visibleCount);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int x = 10;
        int width = this.getScreenWidth() - 20;
        int contentWidth = width - 14;

        this.drawString(graphics,
                StringUtils.translate("halfmasa.gui.keymap_browser.count",
                        this.visibleEntries.size(), this.allEntries.size()),
                x + width - 120, 12, 0xFFC0C0C0);
        this.drawMagnifier(graphics, 12, 30);

        if (this.showKeyboard)
        {
            this.drawKeyboard(graphics, x, HEADER_HEIGHT, width, mouseX, mouseY);
        }
        if (!this.selectedCombo.isEmpty())
        {
            this.drawString(graphics, this.keyFilterLabel(), x, listTop - 12, 0xFFFFC860);
        }

        for (int index = 0; index < visibleCount; index++)
        {
            int entryIndex = this.scrollOffset + index;
            if (entryIndex >= this.visibleEntries.size())
            {
                break;
            }
            int y = listTop + index * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + contentWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered)
            {
                this.drawRect(graphics, x, y, x + contentWidth, y + ROW_HEIGHT, 0x30FFFFFF);
            }

            BrowserEntry entry = this.visibleEntries.get(entryIndex);

            int categoryWidth = Math.max(90, contentWidth / 4);
            String category = this.mc.font.plainSubstrByWidth(entry.modName(), categoryWidth - 6);
            this.drawString(graphics, category, x + 2, y + 5, 0xFF808080);

            int actionX = x + categoryWidth;
            int keyX = x + contentWidth - KEY_COLUMN_WIDTH;
            int contextX = keyX - 34;
            String action = this.mc.font.plainSubstrByWidth(
                    entry.displayName() != null ? entry.displayName() : entry.action(),
                    contextX - actionX - 8);
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

            String keyText = entry.keyText().isEmpty()
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound")
                    : entry.keyText();
            int keyColor = entry.keyText().isEmpty() ? 0xFF707070 : entry.conflicted() ? 0xFFFFC860 : 0xFFE0E0E0;
            keyText = this.mc.font.plainSubstrByWidth(keyText, KEY_COLUMN_WIDTH - 8);
            int keyWidth = this.mc.font.width(keyText);
            this.drawString(graphics, keyText, keyX + KEY_COLUMN_WIDTH - 8 - keyWidth, y + 5, keyColor);

            if (hovered && entry.conflicted())
            {
                this.drawConflictTooltip(graphics, entry, mouseX, mouseY);
            }
        }

        this.scrollbarX = x + width - 7;
        this.scrollbarTrackTop = listTop;
        this.scrollbarTrackBottom = listBottom;
        this.scrollbarMaxOffset = maxOffset;
        this.scrollbarVisibleCount = visibleCount;
        if (this.visibleEntries.size() > visibleCount)
        {
            int trackX = this.scrollbarX;
            this.drawRect(graphics, trackX, listTop, trackX + 2, listBottom, 0x30FFFFFF);
            int trackHeight = listBottom - listTop;
            int thumbHeight = Math.max(12, trackHeight * visibleCount / this.visibleEntries.size());
            int thumbY = listTop + (trackHeight - thumbHeight) * this.scrollOffset / Math.max(1, maxOffset);
            this.drawRect(graphics, trackX, thumbY, trackX + 2, thumbY + thumbHeight, this.draggingScrollbar
                    ? 0xFFD0C090 : 0x90FFFFFF);
        }
        if (this.draggingScrollbar && this.scrollbarMaxOffset > 0)
        {
            int trackHeight = this.scrollbarTrackBottom - this.scrollbarTrackTop;
            int thumbHeight = Math.max(12, trackHeight * this.scrollbarVisibleCount / this.visibleEntries.size());
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
        int gap = 12;
        int mainWidth = (int) ((width - 2 * gap) * 0.635D);
        int clusterWidth = (int) ((width - 2 * gap) * 0.155D);
        int numpadWidth = width - 2 * gap - mainWidth - clusterWidth;
        int numpadX = x + width - numpadWidth;
        int navX = numpadX - gap - clusterWidth;
        int rowStep = CELL_HEIGHT + CELL_GAP;

        String[][] mainRows = {
                {"ESC:256:1", "_:0:1", "F1:290:1", "F2:291:1", "F3:292:1", "F4:293:1", "_:0:0.5",
                        "F5:294:1", "F6:295:1", "F7:296:1", "F8:297:1", "_:0:0.5",
                        "F9:298:1", "F10:299:1", "F11:300:1", "F12:301:1"},
                {"`:96:1", "1:48:1", "2:49:1", "3:50:1", "4:51:1", "5:52:1", "6:53:1", "7:54:1",
                        "8:55:1", "9:56:1", "0:57:1", "-:45:1", "=:61:1", "⌫:259:2"},
                {"TAB:258:1.5", "Q:81:1", "W:87:1", "E:69:1", "R:82:1", "T:84:1", "Y:89:1", "U:85:1",
                        "I:73:1", "O:79:1", "P:80:1", "[:91:1", "]:93:1", "\\\\:92:1.5"},
                {"CAPS:280:1.75", "A:65:1", "S:83:1", "D:68:1", "F:70:1", "G:71:1", "H:72:1", "J:74:1",
                        "K:75:1", "L:76:1", ";:59:1", "':39:1", "ENTER:257:2.25"},
                {"⇧:340:2.25", "Z:90:1", "X:88:1", "C:67:1", "V:86:1", "B:66:1", "N:78:1", "M:77:1",
                        ",:44:1", ".:46:1", "/:47:1", "⇧:344:2.75"},
                {"CTRL:341:1.25", "WIN:343:1.25", "ALT:342:1.25", "　:32:6.25", "ALT:346:1.25",
                        "WIN:347:1.25", "MENU:348:1.25", "CTRL:345:1.25"}
        };
        for (int rowIndex = 0; rowIndex < mainRows.length; rowIndex++)
        {
            this.drawKeyRow(graphics, mainRows[rowIndex], x,
                    y + rowIndex * rowStep, mainWidth, 15.0F, mouseX, mouseY);
        }
        // PRT/SCR/PAU sit above the nav cluster, aligned to its three columns
        String[] prtRow = {"PRT:283:1", "SCR:281:1", "PAU:284:1"};
        this.drawKeyRow(graphics, prtRow, navX, y, clusterWidth, 3.0F, mouseX, mouseY);

        String[][] navRows = {
                {"INS:260:1", "HOM:268:1", "PGU:266:1"},
                {"DEL:261:1", "END:269:1", "PGD:267:1"},
                {"_:0:1", "↑:264:1", "_:0:1"},
                {"←:263:1", "↓:265:1", "→:262:1"}
        };
        for (int rowIndex = 0; rowIndex < navRows.length; rowIndex++)
        {
            this.drawKeyRow(graphics, navRows[rowIndex], navX,
                    y + (rowIndex + 2) * rowStep, clusterWidth, 3.0F, mouseX, mouseY);
        }

        String[][] numpadRows = {
                {"NUM:282:1", "/:331:1", "*:332:1", "-:333:1"},
                {"7:327:1", "8:328:1", "9:329:1", "+:334:1:2"},
                {"4:324:1", "5:325:1", "6:326:1", "_:0:1"},
                {"1:321:1", "2:322:1", "3:323:1", "⏎:335:1:2"},
                {"0:320:2", ".:330:1", "_:0:1"}
        };
        for (int rowIndex = 0; rowIndex < numpadRows.length; rowIndex++)
        {
            this.drawKeyRow(graphics, numpadRows[rowIndex], numpadX,
                    y + rowIndex * rowStep, numpadWidth, 4.0F, mouseX, mouseY);
        }

        String[] mouseRow = {"L:-1:1", "M:-3:1", "R:-2:1"};
        this.drawKeyRow(graphics, mouseRow, numpadX,
                y + 5 * rowStep, numpadWidth, 3.0F, mouseX, mouseY);
    }

    //#if MC >= 1.21.11
    private void drawKeyRow(GuiContext graphics, String[] cells, int x, int y, int width,
            float totalUnits, int mouseX, int mouseY)
    //#else
    //$$ private void drawKeyRow(GuiGraphics graphics, String[] cells, int x, int y, int width,
    //$$         float totalUnits, int mouseX, int mouseY)
    //#endif
    {
        float unit = width / totalUnits;
        float cellX = x;
        for (String cell : cells)
        {
            String[] parts = cell.split(":");
            int cellCode = Integer.parseInt(parts[1]);
            boolean mouseKey = cellCode < 0;
            int displayCode = mouseKey ? -cellCode - 1 : cellCode;
            float units = Float.parseFloat(parts[2]);
            int cellWidth = (int) (unit * units) - CELL_GAP;
            int cellHeight = parts.length > 3 && "2".equals(parts[3])
                    ? 2 * CELL_HEIGHT + CELL_GAP : CELL_HEIGHT;

            if (cellCode != 0)
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

                int fill = 0x66202028;
                if (hasVanilla && hasMalilib)
                {
                    fill = 0x66205038;
                }
                else if (hasVanilla)
                {
                    fill = 0x66203850;
                }
                else if (hasMalilib)
                {
                    fill = 0x66285028;
                }
                if (selected)
                {
                    fill = 0x90605820;
                }
                this.drawRect(graphics, (int) cellX, y, (int) cellX + cellWidth, y + cellHeight, fill);
                int border = selected ? 0xFFF0D080 : (hasVanilla || hasMalilib) ? 0xFF787888 : 0xFF3C3C46;
                this.drawRect(graphics, (int) cellX, y, (int) cellX + cellWidth, y + 1, border);
                this.drawRect(graphics, (int) cellX, y + cellHeight - 1, (int) cellX + cellWidth, y + cellHeight, border);
                this.drawRect(graphics, (int) cellX, y, (int) cellX + 1, y + cellHeight, border);
                this.drawRect(graphics, (int) cellX + cellWidth - 1, y, (int) cellX + cellWidth, y + cellHeight, border);

                String label = mouseKey
                        ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + displayCode)
                        : parts[0];
                int textX = (int) cellX + Math.max(1, (cellWidth - this.mc.font.width(label)) / 2);
                this.drawString(graphics, label, textX, y + (cellHeight - 8) / 2, 0xFFE0E0E0);

                this.keyCells.add(new KeyCell(cellCode, (int) cellX, y, cellWidth, cellHeight, mouseKey));
            }
            cellX += cellWidth + CELL_GAP;
        }
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
    private void drawConflictTooltip(GuiContext graphics, BrowserEntry entry, int mouseX, int mouseY)
    //#else
    //$$ private void drawConflictTooltip(GuiGraphics graphics, BrowserEntry entry, int mouseX, int mouseY)
    //#endif
    {
        List<String> lines = new ArrayList<>();
        lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.conflict"));
        for (BrowserEntry other : this.visibleEntries)
        {
            if (other != entry && !other.keyText().isEmpty() && other.keyText().equals(entry.keyText()))
            {
                lines.add(other.modName() + ": " + other.action());
            }
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
            for (KeyCell cell : this.keyCells)
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
            int index = this.scrollOffset + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (index >= 0 && index < this.visibleEntries.size())
            {
                BrowserEntry entry = this.visibleEntries.get(index);
                // Ctrl+click on the row cycles its wheel activation context in place
                if (ctrlDown && entry.isVanilla())
                {
                    KeybindCustomizationStore.Entry data =
                            KeybindCustomizationStore.getInstance().get(entry.mapping());
                    data.activationContext = data.activationContext.next();
                    KeybindCustomizationStore.getInstance().save();
                    return true;
                }
                GuiBase.openGui(new KeybindDetailScreen(entry));
                return true;
            }
        }
        return false;
    }

    private void handleScrollbarDrag(double mouseY)
    {
        int trackHeight = this.scrollbarTrackBottom - this.scrollbarTrackTop;
        int thumbHeight = this.visibleEntries.isEmpty() ? 12
                : Math.max(12, trackHeight * this.scrollbarVisibleCount / this.visibleEntries.size());
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
            long windowHandle = Minecraft.getInstance().getWindow().handle();
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
