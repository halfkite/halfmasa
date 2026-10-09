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
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#endif

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
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
//#if MC >= 26.3
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import io.github.halfmasa.xaerobinding.feature.IgnoredKeySelection;
import net.minecraft.client.gui.screens.ConfirmScreen;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import io.github.halfmasa.xaerobinding.config.KeymapBindingMode;
import io.github.halfmasa.xaerobinding.config.KeymapLayout;
//#endif
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;

/**
 * Combined key binding center: a scrollable searchable list of every binding
 * (vanilla key mappings plus masa-family malilib hotkeys), an on-screen
 * full-size keyboard whose keys filter the list (Ctrl+click to stack several
 * keys) and show which bindings sit on each key, and one-click access to the
 * rebinding controls and per-binding wheel customization.
 */
public final class KeymapBrowserScreen extends GuiBase
{
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_HEADER_HEIGHT = 16;
    private static final int HEADER_HEIGHT = 48;
    private static final int KEYBOARD_HEIGHT = KeymapKeyboardLayout.HEIGHT;
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_ROW_HEIGHT = 14;
    private static final int SINGLE_CONFLICT_FILL = 0xB8783838;
    private static final int COMBINATION_CONFLICT_FILL = 0xB0401414;
    private static final int SINGLE_CONFLICT_BORDER = 0xFFE09090;
    private static final int COMBINATION_CONFLICT_BORDER = 0xFFB04040;
    //#if MC >= 26.3
    private static int rememberedScrollOffset;
    private static final Set<String> rememberedCollapsedGroups = new HashSet<>();
    private int categoryButtonX;
    private int categoryButtonY;
    private int categoryButtonWidth;
    private int categoryPanelWidth;
    private BrowserEntry hoveredEntry;
    private List<KeymapKeyboardStyle.Binding> keyboardBindings = List.of();
    private Set<Integer> ignoredKeyboardKeys = Set.of();
    private final Map<String, List<Integer>> originalKeys = new HashMap<>();
    // Edits remain local until Save and exit, including MaLiLib bindings and settings.
    private final Map<String, KeybindCustomizationStore.Entry> draftCustomizations = new HashMap<>();
    private final Map<String, List<Integer>> draftKeys = new HashMap<>();
    private final Map<IHotkey, KeybindSettings> draftHotkeySettings = new HashMap<>();
    private final KeymapRebindDraft rebindDraft = new KeymapRebindDraft();
    private KeymapBindingMode bindingMode = (KeymapBindingMode) Configs.KEYMAP_BINDING_MODE.getOptionListValue();
    private int headerBottom = HEADER_HEIGHT;
    private int searchIconY = 30;
    //#endif

    private final List<BrowserEntry> allEntries = new ArrayList<>();
    private final List<BrowserEntry> visibleEntries = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<KeymapKeyboardLayout.Key> keyCells = new ArrayList<>();
    private BrowserEntry rebindingEntry;
    private final List<Integer> pendingRebindKeys = new ArrayList<>();
    private final Set<Integer> heldRebindKeys = new HashSet<>();
    private final Set<Integer> selectedCombo = new HashSet<>();
    private String search = "";
    private int categoryIndex;
    private boolean showKeyboard = true;
    //#if MC >= 26.3
    private boolean layout122;
    //#endif
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
        //#if MC >= 26.3
        this.scrollOffset = rememberedScrollOffset;
        this.collapsedGroups.addAll(rememberedCollapsedGroups);
        this.layout122 = ((KeymapLayout) Configs.KEYMAP_KEYBOARD_LAYOUT.getOptionListValue()).isExtended();
        //#endif
    }

    //#if MC >= 26.3
    @Override
    public void removed()
    {
        rememberedScrollOffset = Math.max(0, this.scrollOffset);
        rememberedCollapsedGroups.clear();
        rememberedCollapsedGroups.addAll(this.collapsedGroups);
        super.removed();
    }
    //#endif

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

    private record DisplayRow(String group, BrowserEntry entry, int groupCount)
    {
        boolean isGroup()
        {
            return this.entry == null;
        }
    }

    private record RowColumns(int modX, int categoryX, int actionX, int keyX,
            int resetX, int detailX, int right
            //#if MC >= 26.3
            , int wheelX, int confirmX
            //#endif
            )
    {
    }

    private int keyboardTop()
    {
        //#if MC >= 26.3
        return this.headerBottom;
        //#else
        //$$ return HEADER_HEIGHT;
        //#endif
    }

    private int listBottom()
    {
        //#if MC >= 26.3
        return this.getScreenHeight() - 14;
        //#else
        //$$ return this.getScreenHeight() - 32;
        //#endif
    }

    //#if MC >= 26.3
    @Override
    protected void drawScreenBackground(GuiContext graphics, int mouseX, int mouseY)
    {
        graphics.fill(0, 0, this.getScreenWidth(), this.getScreenHeight(),
                KeymapBackground.color(Configs.KEYMAP_BACKGROUND_TRANSPARENCY.getIntegerValue()));
    }

    private void initHeader()
    {
        String[] labels = {
                "",
                StringUtils.translate(this.allGroupsCollapsed() ? "halfmasa.gui.keymap_browser.expand_all" : "halfmasa.gui.keymap_browser.fold_all"),
                this.categoryButtonLabel(),
                StringUtils.translate(this.showConflictsOnly ? "halfmasa.gui.keymap_browser.show_all" : "halfmasa.gui.keymap_browser.conflicts_only"),
                StringUtils.translate(this.layout122 ? "halfmasa.gui.keymap_browser.layout_122" : "halfmasa.gui.keymap_browser.layout_104"),
                StringUtils.translate(this.showKeyboard ? "halfmasa.gui.keymap_browser.keyboard_hide" : "halfmasa.gui.keymap_browser.keyboard_show"),
                StringUtils.translate("halfmasa.gui.keymap_browser.binding_mode", this.bindingMode.getDisplayName()),
                StringUtils.translate("halfmasa.gui.keymap_browser.save_exit"),
                StringUtils.translate("halfmasa.gui.keymap_browser.discard_exit")
        };
        int[] widths = Arrays.stream(labels).mapToInt(label -> this.mc.font.width(label) + 8).toArray();
        widths[0] = 230;
        widths[2] = Math.max(155, widths[2]);
        var layout = KeymapHeaderLayout.arrange(this.getScreenWidth(), widths);
        this.headerBottom = layout.bottom();
        var cells = layout.cells();
        var searchCell = cells.get(0);
        this.searchIconY = searchCell.y() + 6;
        GuiTextFieldGeneric field = new GuiTextFieldGeneric(searchCell.x() + 18, searchCell.y() + 2,
                Math.max(1, searchCell.width() - 18), 16, this.mc.font);
        field.setTextWrapper(this.search);
        field.setMaxLengthWrapper(64);
        this.addTextField(field, changed -> {
            String value = changed.getTextWrapper();
            if (!value.equals(this.search)) { this.search = value; this.refilter(); }
            return true;
        });
        this.headerButton(cells.get(1), labels[1], () -> {
            if (this.allGroupsCollapsed()) this.collapsedGroups.clear();
            else for (var entry : this.visibleEntries) this.collapsedGroups.add(this.groupKey(entry));
            this.initGui();
        });
        var category = cells.get(2);
        this.categoryButtonX = category.x(); this.categoryButtonY = category.y(); this.categoryButtonWidth = category.width();
        this.updateCategoryPanelBounds();
        this.headerButton(category, labels[2], () -> {
            this.categoryPanelOpen = !this.categoryPanelOpen; this.categoryPanelScroll = 0;
        });
        this.headerButton(cells.get(3), labels[3], () -> {
            this.showConflictsOnly = !this.showConflictsOnly; this.refilter(); this.initGui();
        });
        this.headerButton(cells.get(4), labels[4], () -> {
            this.layout122 = !this.layout122; this.initGui();
        });
        this.headerButton(cells.get(5), labels[5], () -> {
            this.showKeyboard = !this.showKeyboard; this.initGui();
        });
        this.headerButton(cells.get(6), labels[6], () -> {
            this.bindingMode = this.bindingMode.cycle(true);
            if (this.bindingMode == KeymapBindingMode.VIRTUAL) this.showKeyboard = true;
            this.heldRebindKeys.clear(); this.initGui();
        });
        this.headerButton(cells.get(7), labels[7], this::saveAndExit);
        this.headerButton(cells.get(8), labels[8], this::onClose);

    }

    private void headerButton(KeymapHeaderLayout.Cell cell, String label, Runnable action)
    {
        this.addButton(this.browserButton(cell.x(), cell.y(), cell.width(), 20, label), (button, mouseButton) -> action.run());
    }

    private String keyNames(List<Integer> keys)
    {
        return keys.isEmpty() ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound")
                : keys.stream().map(KeymapInputNames::name).collect(java.util.stream.Collectors.joining(" → "));
    }


    private void confirmRebind()
    {
        if (this.rebindingEntry != null && this.rebindDraft.touched()) this.applyRebind(this.rebindDraft.keys());
    }
    //#endif

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        //#if MC >= 26.3
        String editingId = this.rebindingEntry == null ? null : entryId(this.rebindingEntry);
        //#endif
        this.rebuildEntries();
        //#if MC >= 26.3
        if (editingId != null) this.rebindingEntry = this.allEntries.stream()
                .filter(entry -> editingId.equals(entryId(entry))).findFirst().orElse(null);
        //#endif
        this.refilter(false);
        //#if MC >= 26.3
        this.initHeader();
        //#endif
        this.keyCells.clear();
        //#if MC >= 26.3
        this.keyboardBindings = this.allEntries.stream()
                .map(entry -> new KeymapKeyboardStyle.Binding(this.orderedEntryKeys(entry), entry.isVanilla(),
                        entry.conflicted())).toList();
        this.ignoredKeyboardKeys = IgnoredKeySelection.parse(Configs.KEYBIND_IGNORED_KEYS.getStringValue());
        //#endif
        //#if MC >= 26.3
        this.keyCells.addAll(this.layout122
                ? KeymapKeyboardLayout.keys122(10, this.keyboardTop(), this.getScreenWidth() - 20)
                : KeymapKeyboardLayout.keys(10, this.keyboardTop(), this.getScreenWidth() - 20));
        //#else
        //$$ this.keyCells.addAll(KeymapKeyboardLayout.keys(10, this.keyboardTop(), this.getScreenWidth() - 20));
        //#endif

        //#if MC < 26.3
        //$$         int controlsY = 26;
        //$$         int rightMargin = 10;
        //$$         int keyboardButtonWidth = 86;
        //#if MC >= 26.3
        //$$         int layoutButtonWidth = 94;
        //#endif
        //$$         int conflictButtonWidth = 112;
        //$$         int categoryButtonWidth = 190;
        //$$         int keyboardX = this.getScreenWidth() - keyboardButtonWidth - rightMargin;
        //#if MC >= 26.3
        //$$         int layoutX = keyboardX - layoutButtonWidth - 6;
        //$$         int conflictX = layoutX - conflictButtonWidth - 6;
        //#else
        //$$ int conflictX = keyboardX - conflictButtonWidth - 6;
        //#endif
        //$$         int categoryX = conflictX - categoryButtonWidth - 6;
        //$$         int foldAllX = categoryX - 88;
        //$$         int searchX = 28;
        //$$         int searchWidth = Math.min(300, Math.max(120, foldAllX - searchX - 12));
        //$$
        //$$         GuiTextFieldGeneric searchField = new GuiTextFieldGeneric(searchX, controlsY, searchWidth, 16, this.mc.font);
        //$$         searchField.setTextWrapper(this.search);
        //$$         searchField.setMaxLengthWrapper(64);
        //$$         this.addTextField(searchField, field -> {
        //$$             String value = field.getTextWrapper();
        //$$             if (!value.equals(this.search))
        //$$             {
        //$$                 this.search = value;
        //$$                 this.refilter();
        //$$             }
        //$$             return true;
        //$$         });
        //$$
        //#if MC >= 26.3
        //$$         this.categoryButtonX = categoryX;
        //$$         this.categoryButtonY = controlsY - 2;
        //$$         this.categoryButtonWidth = categoryButtonWidth;
        //$$         this.updateCategoryPanelBounds();
        //#endif
        //$$         this.addButton(this.browserButton(categoryX, controlsY - 2, categoryButtonWidth, 20,
        //$$                 this.categoryButtonLabel()), (button, mouseButton) -> {
        //$$                     this.categoryPanelOpen = !this.categoryPanelOpen;
        //$$                     this.categoryPanelScroll = 0;
        //$$                 });
        //$$         this.addButton(this.browserButton(keyboardX, controlsY - 2, keyboardButtonWidth, 20,
        //$$                 StringUtils.translate(this.showKeyboard
        //$$                         ? "halfmasa.gui.keymap_browser.keyboard_hide"
        //$$                         : "halfmasa.gui.keymap_browser.keyboard_show")),
        //$$                 (button, mouseButton) -> {
        //$$                     this.showKeyboard = !this.showKeyboard;
        //$$                     this.initGui();
        //$$                 });
        //#if MC >= 26.3
        //$$         this.addButton(this.browserButton(layoutX, controlsY - 2, layoutButtonWidth, 20,
        //$$                 StringUtils.translate(this.layout122
        //$$                         ? "halfmasa.gui.keymap_browser.layout_122"
        //$$                         : "halfmasa.gui.keymap_browser.layout_104")),
        //$$                 (button, mouseButton) -> {
        //$$                     this.layout122 = !this.layout122;
        //$$                     this.initGui();
        //$$                 });
        //#endif
        //$$         this.addButton(this.browserButton(conflictX, controlsY - 2, conflictButtonWidth, 20,
        //$$                 StringUtils.translate(this.showConflictsOnly
        //$$                         ? "halfmasa.gui.keymap_browser.show_all"
        //$$                         : "halfmasa.gui.keymap_browser.conflicts_only")),
        //$$                 (button, mouseButton) -> {
        //$$                     this.showConflictsOnly = !this.showConflictsOnly;
        //$$                     this.refilter();
        //$$                     this.initGui();
        //$$                 });
        //$$         this.addButton(this.browserButton(foldAllX, controlsY - 2, 82, 20,
        //$$                 StringUtils.translate(this.allGroupsCollapsed()
        //$$                         ? "halfmasa.gui.keymap_browser.expand_all"
        //$$                         : "halfmasa.gui.keymap_browser.fold_all")),
        //$$                 (button, mouseButton) -> {
        //$$                     if (this.allGroupsCollapsed())
        //$$                     {
        //$$                         this.collapsedGroups.clear();
        //$$                     }
        //$$                     else
        //$$                     {
        //$$                         this.collapsedGroups.clear();
        //$$                         for (BrowserEntry entry : this.visibleEntries)
        //$$                         {
        //$$                             this.collapsedGroups.add(this.groupKey(entry));
        //$$                         }
        //$$                     }
                    //#if MC < 26.3
                    //$$ this.scrollOffset = 0;
                    //#endif
        //$$                     this.initGui();
        //$$                 });
        //$$
        //#endif

        //#if MC < 26.3
        //$$ int bottom = this.getScreenHeight() - 28;
        //$$ this.addButton(this.browserButton(10, bottom, 110, 20,
        //$$         StringUtils.translate("halfmasa.gui.keymap_browser.open_editor")),
        //$$         (button, mouseButton) -> GuiBase.openGui(new KeybindCustomizationScreen()));
        //$$ this.addButton(this.browserButton(126, bottom, 74, 20,
        //$$         StringUtils.translate("halfmasa.gui.keymap_browser.done")),
        //$$         (button, mouseButton) -> this.onClose());
        //#endif
    }

    private ButtonGeneric browserButton(int x, int y, int width, int height, String label)
    {
        //#if MC >= 26.3
        return new BrowserButton(x, y, width, height, label);
        //#else
        //$$ return new ButtonGeneric(x, y, width, height, label);
        //#endif
    }

    //#if MC >= 26.3
    static final class BrowserButton extends ButtonGeneric
    {
        BrowserButton(int x, int y, int width, int height, String label)
        {
            super(x, y, width, height, label);
        }

        @Override
        public void render(GuiContext graphics, int mouseX, int mouseY, boolean selected)
        {
            if (!this.visible) return;
            this.hovered = this.isMouseOver(mouseX, mouseY);
            int background = this.hovered && this.enabled ? 0xB0404A5C : 0x80202632;
            int border = this.hovered && this.enabled ? 0xB0D0BA80 : 0x606E788C;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, background);
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 1, border);
            graphics.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, border);
            graphics.fill(this.x, this.y, this.x + 1, this.y + this.height, border);
            graphics.fill(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, border);
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            String label = font.plainSubstrByWidth(this.displayString, Math.max(1, this.width - 8));
            graphics.drawCenteredString(font, label, this.x + this.width / 2,
                    this.y + (this.height - 8) / 2, this.enabled ? 0xFFE8E8F0 : 0xFF808088);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        this.hoveredEntry = null;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        GuiContext context = GuiContext.fromGuiGraphics(graphics);
        // Draw overlays after the whole screen, in a new stratum above text fields and buttons.
        if (this.categoryPanelOpen)
        {
            context.nextStratum();
            this.drawCategoryPanel(context, mouseX, mouseY);
        }
        else if (this.hoveredEntry != null)
        {
            context.nextStratum();
            this.drawEntryTooltip(context, this.hoveredEntry, mouseX, mouseY);
        }
    }

    private void updateCategoryPanelBounds()
    {
        this.categoryPanelWidth = Math.max(1, Math.min(this.categoryButtonWidth, this.getScreenWidth() - 20));
        this.categoryPanelX = Math.max(10, Math.min(this.categoryButtonX,
                this.getScreenWidth() - this.categoryPanelWidth - 10));
        this.categoryPanelY = this.categoryButtonY + 23;
        int availableRows = Math.max(1, (this.getScreenHeight() - this.categoryPanelY - 38) / PANEL_ROW_HEIGHT);
        this.categoryPanelRows = Math.min(availableRows, this.categories.size() + 1);
    }

    private KeybindCustomizationStore.Entry customization(String id)
    {
        return this.draftCustomizations.computeIfAbsent(id,
                key -> KeybindCustomizationStore.getInstance().get(key).copy());
    }

    private KeybindCustomizationStore.Entry customization(BrowserEntry entry)
    {
        return this.customization(entryId(entry));
    }

    private KeybindSettings hotkeySettings(IHotkey hotkey)
    {
        return this.draftHotkeySettings.getOrDefault(hotkey, hotkey.getKeybind().getSettings());
    }

    private boolean requiresKeyOrder(BrowserEntry entry)
    {
        return entry.isVanilla() ? this.customization(entry).requireKeyOrder
                : this.hotkeySettings(entry.hotkey()).isOrderSensitive();
    }

    private void toggleKeyOrder(BrowserEntry entry)
    {
        boolean ordered = !this.requiresKeyOrder(entry);
        if (entry.isVanilla()) this.customization(entry).requireKeyOrder = ordered;
        else
        {
            KeybindSettings current = this.hotkeySettings(entry.hotkey());
            this.draftHotkeySettings.put(entry.hotkey(), KeybindSettings.create(
                    current.getContext(), current.getActivateOn(), current.getAllowEmpty(),
                    current.getAllowExtraKeys(), ordered, current.isExclusive(), current.shouldCancel()));
        }
    }

    private void toggleWheelParticipation(BrowserEntry entry)
    {
        KeybindCustomizationStore.Entry data = this.customization(entry);
        boolean enabled = !data.participatesInWheel(this.orderedEntryKeys(entry));
        var original = KeybindCustomizationStore.getInstance().get(entryId(entry));
        data.wheelEnabled = enabled == original.participatesInWheel(this.orderedEntryKeys(entry))
                ? original.wheelEnabled : Boolean.valueOf(enabled);
    }

    private void saveAndExit()
    {
        this.confirmRebind();
        this.cancelRebind();
        KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
        for (BrowserEntry entry : this.allEntries)
        {
            List<Integer> keys = this.draftKeys.get(entryId(entry));
            if (keys != null)
            {
                if (entry.isVanilla())
                {
                    int code = keys.isEmpty() ? 0 : keys.getFirst();
                    entry.mapping().setKey(keys.size() != 1 ? InputConstants.UNKNOWN
                            : code < 0 ? InputConstants.Type.MOUSE.getOrCreate(
                                    InputCompat.layoutCodeToMouseButton(code)) : InputCompat.keyboardKey(code));
                }
                else
                {
                    entry.hotkey().getKeybind().clearKeys();
                    for (int code : keys)
                    {
                        entry.hotkey().getKeybind().addKey(code < 0
                                ? InputCompat.layoutCodeToMouseButton(code) - 100 : code);
                    }
                }
            }
            if (!entry.isVanilla() && this.draftHotkeySettings.containsKey(entry.hotkey()))
            {
                entry.hotkey().getKeybind().setSettings(this.draftHotkeySettings.get(entry.hotkey()));
            }
        }
        this.draftCustomizations.forEach(store::put);
        KeyMapping.resetMapping();
        InputEventHandler.getKeybindManager().updateUsedKeys();
        this.mc.options.save();
        ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
        store.save();
        Configs.KEYMAP_BINDING_MODE.setOptionListValue(this.bindingMode);
        Configs.KEYMAP_KEYBOARD_LAYOUT.setOptionListValue(KeymapLayout.of(this.layout122));
        new Configs().save();
        KeybindPieManager.getInstance().invalidateCustomMappingSync();
        this.closeGui(true);
    }

    @Override
    public void onClose()
    {
        if (this.hasUnsavedChanges()) GuiBase.openGui(new DiscardConfirmationScreen(this));
        else this.closeGui(true);
    }

    private boolean hasUnsavedChanges()
    {
        if (this.bindingMode != Configs.KEYMAP_BINDING_MODE.getOptionListValue()) return true;
        if (KeymapLayout.of(this.layout122) != Configs.KEYMAP_KEYBOARD_LAYOUT.getOptionListValue()) return true;
        if (this.rebindingEntry != null && this.rebindDraft.touched()
                && !this.rebindDraft.keys().equals(this.orderedEntryKeys(this.rebindingEntry))) return true;
        var store = KeybindCustomizationStore.getInstance();
        if (this.draftCustomizations.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().sameAs(store.get(entry.getKey())))) return true;
        if (this.draftKeys.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().equals(this.originalKeys.get(entry.getKey())))) return true;
        return this.draftHotkeySettings.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().equals(entry.getKey().getKeybind().getSettings()));
    }

    private static final class DiscardConfirmationScreen extends ConfirmScreen
    {
        private final KeymapBrowserScreen owner;

        private DiscardConfirmationScreen(KeymapBrowserScreen owner)
        {
            super(confirmed -> {
                if (confirmed) owner.closeGui(true);
                else GuiBase.openGui(owner);
            }, Component.translatable("halfmasa.gui.keymap_browser.discard_title"),
                    Component.translatable("halfmasa.gui.keymap_browser.discard_message"),
                    Component.translatable("halfmasa.gui.keymap_browser.discard_confirm"),
                    Component.translatable("gui.cancel"));
            this.owner = owner;
        }

        @Override
        public void onClose()
        {
            GuiBase.openGui(this.owner);
        }
    }

    public static boolean isEditingBindings(net.minecraft.client.gui.screens.Screen screen)
    {
        return screen instanceof KeymapBrowserScreen || screen instanceof DiscardConfirmationScreen;
    }
    //#endif

    private void rebuildEntries()
    {
        this.allEntries.clear();
        this.categories.clear();

        List<KeyMapping> mappings = Arrays.stream(this.mc.options.keyMappings).toList();

        List<BrowserEntry> entries = new ArrayList<>();
        for (KeyMapping mapping : mappings)
        {
            //#if MC >= 26.3
            KeybindCustomizationStore.Entry custom = this.customization(mapping.getName());
            //#else
            //$$ KeybindCustomizationStore.Entry custom = KeybindCustomizationStore.getInstance().get(mapping);
            //#endif
            boolean customCombo = custom.comboKeys != null && !custom.comboKeys.isEmpty();
            boolean unbound = mapping.isUnbound() && !customCombo;
            //#if MC >= 26.3
            BrowserEntry preview = new BrowserEntry(mapping, null, "", "", "", null, "", ConflictType.NONE);
            String keyText = this.keyListText(this.orderedEntryKeys(preview));
            //#else
            //$$ String keyText = customCombo ? this.keyListText(custom.comboKeys) : (unbound ? "" : keyText(mapping));
            //#endif
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
                //#if MC >= 26.3
                BrowserEntry preview = new BrowserEntry(null, hotkey, "", "", "", null, "", ConflictType.NONE);
                String display = this.keyListText(this.orderedEntryKeys(preview));
                //#else
                //$$ String display = hotkey.getKeybind().getKeysDisplayString();
                //#endif
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

    private List<DisplayRow> displayedRows()
    {
        List<DisplayRow> result = new ArrayList<>();
        Map<String, Integer> groupCounts = new HashMap<>();
        for (BrowserEntry entry : this.visibleEntries)
        {
            groupCounts.merge(this.groupKey(entry), 1, Integer::sum);
        }
        //#if MC >= 26.3
        Set<BrowserEntry> bestMatches = new HashSet<>();
        if (!this.selectedCombo.isEmpty() && !this.visibleEntries.isEmpty())
        {
            int bestSize = this.visibleEntries.stream().mapToInt(entry -> this.entryKeySet(entry).size()).min().orElse(0);
            for (BrowserEntry entry : this.visibleEntries)
            {
                if (this.entryKeySet(entry).size() == bestSize) bestMatches.add(entry);
            }
            String group = StringUtils.translate("halfmasa.gui.keymap_browser.best_matches");
            result.add(new DisplayRow(group, null, bestMatches.size()));
            for (BrowserEntry entry : this.visibleEntries)
            {
                if (bestMatches.contains(entry)) result.add(new DisplayRow(group, entry, 0));
            }
            groupCounts.clear();
            for (BrowserEntry entry : this.visibleEntries)
            {
                if (!bestMatches.contains(entry)) groupCounts.merge(this.groupKey(entry), 1, Integer::sum);
            }
        }
        //#endif
        Set<String> addedGroups = new HashSet<>();
        for (BrowserEntry entry : this.visibleEntries)
        {
            //#if MC >= 26.3
            if (bestMatches.contains(entry)) continue;
            //#endif
            String group = this.groupKey(entry);
            if (addedGroups.add(group))
            {
                result.add(new DisplayRow(group, null, groupCounts.get(group)));
            }
            if (!this.collapsedGroups.contains(group))
            {
                result.add(new DisplayRow(group, entry, 0));
            }
        }
        return result;
    }

    private String groupKey(BrowserEntry entry)
    {
        return entry.modName();
    }

    //#if MC >= 26.3
    private boolean isBestMatchGroup(String group)
    {
        return !this.selectedCombo.isEmpty() &&
                group.equals(StringUtils.translate("halfmasa.gui.keymap_browser.best_matches"));
    }
    //#endif

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

    private void toggleGroup(String group)
    {
        if (!this.collapsedGroups.add(group))
        {
            this.collapsedGroups.remove(group);
        }
        //#if MC < 26.3
        //$$ this.scrollOffset = 0;
        //#endif
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
        return -InputCompat.mouseButtonToLayoutCode(
                ((KeyMappingAccessor) mapping).halfmasa$getBoundKey().getValue()) - 1;
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
            if (InputCompat.isKeyboardKey(debugModifier))
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
                    : InputCompat.keyboardKey(code).getDisplayName().getString());
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

    //#if MC >= 26.3
    private String[] keyboardLegendLabels()
    {
        return new String[] {
                StringUtils.translate("halfmasa.gui.keyboard.legend.bound"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.vanilla_dot"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.malilib_dot"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.combination"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.single_conflict"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.combination_conflict"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.ignored"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.unbound"),
                StringUtils.translate("halfmasa.gui.keyboard.legend.selected")
        };
    }
    //#else
    //$$ private String[] keyboardLegendLabels()
    //$$ {
    //$$     return new String[] {
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.vanilla"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.both"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.malilib"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.unbound"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.selected"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.single"),
    //$$             StringUtils.translate("halfmasa.gui.keymap_browser.legend.combination")
    //$$     };
    //$$ }
    //#endif

    //#if MC >= 26.3
    private int[] keyboardLegendColors()
    {
        return new int[] {KeymapKeyboardStyle.BOUND, KeymapKeyboardStyle.VANILLA_DOT,
                KeymapKeyboardStyle.MALILIB_DOT, KeymapKeyboardStyle.COMBINATION_DOT,
                KeymapKeyboardStyle.SINGLE_CONFLICT, KeymapKeyboardStyle.COMBINATION_CONFLICT,
                KeymapKeyboardStyle.IGNORED_DOT, KeymapKeyboardStyle.UNBOUND, KeymapKeyboardStyle.UNBOUND};
    }
    //#else
    //$$ private int[] keyboardLegendColors()
    //$$ {
    //$$     return new int[] {
    //$$             0x66203850,
    //$$             0x66205038,
    //$$             0x66285028,
    //$$             0x66202028,
    //$$             0x90605820,
    //$$             SINGLE_CONFLICT_FILL,
    //$$             COMBINATION_CONFLICT_FILL
    //$$     };
    //$$ }
    //#endif

    //#if MC >= 26.3
    private int[] keyboardLegendBorders()
    {
        return new int[] {0xFF59616E, KeymapKeyboardStyle.VANILLA_DOT,
                KeymapKeyboardStyle.MALILIB_DOT, KeymapKeyboardStyle.COMBINATION_DOT,
                KeymapKeyboardStyle.SINGLE_CONFLICT, KeymapKeyboardStyle.COMBINATION_CONFLICT,
                KeymapKeyboardStyle.IGNORED_DOT, 0xFF59616E, KeymapKeyboardStyle.SELECTED_BORDER};
    }
    //#else
    //$$ private int[] keyboardLegendBorders()
    //$$ {
    //$$     return new int[] {
    //$$             0xFF6090C0,
    //$$             0xFF70B880,
    //$$             0xFFB88050,
    //$$             0xFF707078,
    //$$             0xFFF0D080,
    //$$             SINGLE_CONFLICT_BORDER,
    //$$             COMBINATION_CONFLICT_BORDER
    //$$     };
    //$$ }
    //#endif

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
            //#if MC >= 26.3
            if (index >= 1 && index <= 6)
            {
                KeymapKeyboardRenderer.drawMarker(graphics, itemX + 2, itemY + 2, colors[index]);
            }
            else
            {
            //#endif
            this.drawRect(graphics, itemX, itemY, itemX + swatchSize, itemY + swatchSize, borders[index]);
            this.drawRect(graphics, itemX + 1, itemY + 1, itemX + swatchSize - 1,
                    itemY + swatchSize - 1, colors[index]);
            //#if MC >= 26.3
            }
            //#endif
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
        int listTop = this.keyboardTop() + (this.showKeyboard ? this.keyboardHeight() : 0);
        if (this.showKeyboard)
        {
            listTop += this.keyboardLegendHeight() + 3;
        }
        if (!this.selectedCombo.isEmpty())
        {
            listTop += 12;
        }
        return listTop + LIST_HEADER_HEIGHT;
    }

    private int keyboardHeight()
    {
        //#if MC >= 26.3
        return this.layout122 ? KeymapKeyboardLayout.HEIGHT_122 : KEYBOARD_HEIGHT;
        //#else
        //$$ return KEYBOARD_HEIGHT;
        //#endif
    }

    private RowColumns rowColumns(int x, int width)
    {
        int right = x + width;
        //#if MC >= 26.3
        int wheelX = right - Math.min(112, Math.max(62, width / 5));
        int detailX = wheelX - Math.min(93, Math.max(62, width / 6));
        int resetX = detailX - Math.min(46, Math.max(28, width / 12));
        int keyX = resetX - Math.max(60, width / 6);
        int categoryX = x + Math.max(1, (keyX - x) / 4);
        int actionX = categoryX + Math.max(1, (keyX - x) / 4);
        int confirmX = keyX;
        keyX += Math.min(38, Math.max(26, width / 24));
        return new RowColumns(x, categoryX, actionX, keyX, resetX, detailX, right, wheelX, confirmX);
        //#else
        //$$ int detailX = right - 78;
        //$$ int resetX = detailX - 52;
        //$$ int keyX = resetX - Math.max(105, width / 5);
        //$$ int categoryX = x + Math.max(95, width / 6);
        //$$ int actionX = categoryX + Math.max(95, width / 5);
        //$$ return new RowColumns(x, categoryX, actionX, keyX, resetX, detailX, right);
        //#endif
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
    //#if MC >= 26.3
    private java.util.Set<Integer> entryKeySet(BrowserEntry entry)
    {
        return new HashSet<>(this.orderedEntryKeys(entry));
    }
    //#else
    //$$ private java.util.Set<Integer> entryKeySet(BrowserEntry entry)
    //$$ {
    //$$     java.util.Set<Integer> keys = new HashSet<>();
    //$$     if (entry.isVanilla())
    //$$     {
    //$$         List<Integer> customCombo = KeybindCustomizationStore.getInstance().comboKeys(entry.mapping());
    //$$         if (!customCombo.isEmpty())
    //$$         {
    //$$             keys.addAll(customCombo);
    //$$         }
    //$$         else if (!entry.mapping().isUnbound())
    //$$         {
    //$$             InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
    //$$             keys.add(key.getType() == InputConstants.Type.MOUSE
    //$$                     ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue());
    //$$             //#if MC >= 26.2
    //$$             if (isDebugOnlyMapping(entry.mapping()) && this.mc.options != null &&
    //$$                 this.mc.options.keyDebugModifier != null)
    //$$             {
    //$$                 InputConstants.Key debugModifier =
    //$$                         ((KeyMappingAccessor) this.mc.options.keyDebugModifier).halfmasa$getBoundKey();
    //$$                 if (InputCompat.isKeyboardKey(debugModifier))
    //$$                 {
    //$$                     keys.add(debugModifier.getValue());
    //$$                 }
    //$$             }
    //$$             //#endif
    //$$         }
    //$$     }
    //$$     else
    //$$     {
    //$$         for (int code : entry.hotkey().getKeybind().getKeys())
    //$$         {
    //$$             keys.add(code);
    //$$         }
    //$$     }
    //$$     return keys;
    //$$ }
    //#endif

    //#if MC >= 1.21.11
    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        int listTop = this.listTop();
        int listBottom = this.listBottom();
        List<DisplayRow> rows = this.displayedRows();
        int visibleCount = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        int maxOffset = Math.max(0, rows.size() - visibleCount);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int x = 10;
        int width = this.getScreenWidth() - 20;
        int contentWidth = width - 14;
        RowColumns columns = this.rowColumns(x, contentWidth);

        this.drawString(graphics,
                StringUtils.translate("halfmasa.gui.keymap_browser.count",
                        this.visibleEntries.size(), this.allEntries.size()),
                x + width - 120, 12, 0xFFC0C0C0);
        //#if MC >= 26.3
        this.drawMagnifier(graphics, 12, this.searchIconY);
        //#else
        //$$ this.drawMagnifier(graphics, 12, 30);
        //#endif

        if (this.showKeyboard)
        {
            this.drawKeyboard(graphics, x, this.keyboardTop(), width, mouseX, mouseY);
            this.drawKeyboardLegend(graphics, x, this.keyboardTop() + this.keyboardHeight() + 3, width);
        }
        if (!this.selectedCombo.isEmpty())
        {
            this.drawString(graphics, this.keyFilterLabel(), x, listTop - LIST_HEADER_HEIGHT - 12, 0xFFFFC860);
        }

        int headerY = listTop - LIST_HEADER_HEIGHT;
        this.drawRect(graphics, x, headerY, x + contentWidth, listTop, 0xA0181A20);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_mod"),
                columns.modX() + 4, headerY + 4, 0xFFB0B0B0);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_category"),
                columns.categoryX() + 4, headerY + 4, 0xFFB0B0B0);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_action"),
                columns.actionX() + 4, headerY + 4, 0xFFB0B0B0);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_key"),
                columns.keyX() + 4, headerY + 4, 0xFFB0B0B0);

        //#if MC >= 26.3
        this.drawCenteredFittedKeyLabel(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.reset"),
                columns.resetX(), columns.detailX(), headerY, LIST_HEADER_HEIGHT, 0xFFB0B0B0);
        this.drawCenteredFittedKeyLabel(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_order"),
                columns.detailX(), columns.wheelX(), headerY, LIST_HEADER_HEIGHT, 0xFFB0B0B0);
        this.drawCenteredFittedKeyLabel(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.column_trigger"),
                columns.wheelX(), columns.right(), headerY, LIST_HEADER_HEIGHT, 0xFFB0B0B0);
        //#endif
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

            DisplayRow row = rows.get(entryIndex);
            if (row.isGroup())
            {
                this.drawRect(graphics, x, y, x + contentWidth, y + ROW_HEIGHT, 0xB0202630);
                String marker = this.collapsedGroups.contains(row.group()) ? "[+] " : "[-] ";
                //#if MC >= 26.3
                if (this.isBestMatchGroup(row.group())) marker = "";
                //#endif
                this.drawString(graphics, marker + row.group() + " (" + row.groupCount() + ")",
                        x + 6, y + 5, 0xFFFFC860);
                continue;
            }

            BrowserEntry entry = row.entry();
            String modName = this.mc.font.plainSubstrByWidth(entry.modName(),
                    columns.categoryX() - columns.modX() - 8);
            String category = this.mc.font.plainSubstrByWidth(entry.category(),
                    columns.actionX() - columns.categoryX() - 8);
            String actionName = entry.displayName() != null ? entry.displayName() : entry.action();
            String action = this.mc.font.plainSubstrByWidth(actionName,
                    Math.max(1, columns.keyX() - columns.actionX() - 8));
            this.drawString(graphics, modName, columns.modX() + 4, y + 5, 0xFFB0B0B0);
            this.drawString(graphics, category, columns.categoryX() + 4, y + 5, 0xFF888888);
            this.drawString(graphics, action, columns.actionX() + 4, y + 5, 0xFFFFFFFF);

            boolean keyHovered = hovered && mouseX >= columns.keyX() && mouseX < columns.resetX();
            int keyColor = this.rebindingEntry == entry ? 0x90605820
                    : keyHovered ? 0x70404050 : 0x30202028;
            this.drawRect(graphics, columns.keyX(), y + 1, columns.resetX() - 3, y + ROW_HEIGHT - 1, keyColor);
            String keyLabel = this.rebindingEntry == entry
                    //#if MC >= 26.3
                    ? this.rebindDraft.touched() ? this.keyNames(this.rebindDraft.keys())
                            : StringUtils.translate("halfmasa.gui.keymap_browser.choose_keys")
                    //#else
                    //$$ ? StringUtils.translate("halfmasa.gui.keymap_browser.press_key")
                    //#endif
                    : entry.keyText().isEmpty()
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound") : entry.keyText();
            String fittedKey = this.mc.font.plainSubstrByWidth(keyLabel,
                    Math.max(1, columns.resetX() - columns.keyX() - 10));
            this.drawString(graphics, fittedKey, columns.keyX() + 4, y + 5,
                    entry.conflicted() ? 0xFFFF6060 : 0xFFFFD080);
            //#if MC >= 26.3
            if (this.rebindingEntry == entry)
            {
                this.drawRect(graphics, columns.confirmX(), y + 1, columns.keyX() - 3, y + ROW_HEIGHT - 1,
                        this.rebindDraft.touched() ? 0xB02D6942 : 0x70202632);
                this.drawCenteredFittedKeyLabel(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.confirm"),
                        columns.confirmX(), columns.keyX() - 3, y, ROW_HEIGHT,
                        this.rebindDraft.touched() ? 0xFFE0E0E0 : 0xFF888888);
            }
            //#endif

            this.drawRect(graphics, columns.resetX(), y + 1, columns.detailX() - 3,
                    y + ROW_HEIGHT - 1, 0x80505050);
            this.drawCenteredFittedKeyLabel(graphics,
                    StringUtils.translate("halfmasa.gui.keymap_browser.reset"),
                    columns.resetX(), columns.detailX() - 3, y, ROW_HEIGHT, 0xFFE0E0E0);
            //#if MC >= 26.3
            boolean orderHovered = hovered && mouseX >= columns.detailX() && mouseX < columns.wheelX();
            this.drawRect(graphics, columns.detailX(), y + 1, columns.wheelX() - 3,
                    y + ROW_HEIGHT - 1, orderHovered ? 0xA0404A5C : 0x80202632);
            this.drawCenteredFittedKeyLabel(graphics,
                    StringUtils.translate(this.requiresKeyOrder(entry)
                            ? "halfmasa.gui.keymap_browser.yes" : "halfmasa.gui.keymap_browser.no"),
                    columns.detailX(), columns.wheelX() - 3, y, ROW_HEIGHT, 0xFFE0E0E0);
            boolean wheelHovered = hovered && mouseX >= columns.wheelX();
            boolean wheelEnabled = this.customization(entry).participatesInWheel(this.orderedEntryKeys(entry));
            this.drawRect(graphics, columns.wheelX(), y + 1, columns.right(),
                    y + ROW_HEIGHT - 1, wheelHovered ? 0xA0404A5C : 0x80202632);
            this.drawCenteredFittedKeyLabel(graphics,
                    StringUtils.translate(wheelEnabled
                            ? "halfmasa.gui.keymap_browser.yes" : "halfmasa.gui.keymap_browser.no"),
                    columns.wheelX(), columns.right(), y, ROW_HEIGHT, wheelEnabled ? 0xFFFFD080 : 0xFFB0B0B0);
            //#else
            //$$ this.drawRect(graphics, columns.detailX(), y + 1, columns.right(),
            //$$         y + ROW_HEIGHT - 1, 0x80505050);
            //$$ this.drawCenteredFittedKeyLabel(graphics,
            //$$         StringUtils.translate("halfmasa.gui.keymap_browser.details"),
            //$$         columns.detailX(), columns.right(), y, ROW_HEIGHT, 0xFFE0E0E0);
            //#endif

            if (hovered && (entry.conflicted() || !action.equals(actionName) || !fittedKey.equals(keyLabel)))
            {
                //#if MC >= 26.3
                this.hoveredEntry = entry;
                //#else
                //$$ this.drawEntryTooltip(graphics, entry, mouseX, mouseY);
                //#endif
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

        //#if MC < 26.3
        //$$ if (this.categoryPanelOpen)
        //$$ {
        //$$     this.drawCategoryPanel(graphics, mouseX, mouseY);
        //$$ }
        //#endif
        if (this.rebindingEntry != null)
        {
            //#if MC >= 26.3
            String hint = StringUtils.translate(this.bindingMode == KeymapBindingMode.VIRTUAL
                    ? "halfmasa.gui.keymap_browser.capture_virtual_hint" : "halfmasa.gui.keymap_browser.capture_physical_hint");
            this.drawString(graphics, this.mc.font.plainSubstrByWidth(hint, Math.max(1, this.getScreenWidth() - 20)),
                    10, this.getScreenHeight() - 12, 0xFFFFC860);
            //#else
            //$$ this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.capture_hint"),
            //$$         210, this.getScreenHeight() - 22, 0xFFFFC860);
            //#endif
        }
    }

    //#if MC >= 1.21.11
    private void drawCategoryPanel(GuiContext graphics, int mouseX, int mouseY)
    //#else
    //$$ private void drawCategoryPanel(GuiGraphics graphics, int mouseX, int mouseY)
    //#endif
    {
        //#if MC >= 26.3
        this.updateCategoryPanelBounds();
        int panelWidth = this.categoryPanelWidth;
        //#else
        //$$ int panelWidth = PANEL_WIDTH;
        //$$ this.categoryPanelX = this.getScreenWidth() - PANEL_WIDTH - 106;
        //$$ this.categoryPanelY = 46;
        //$$ int visibleRows = (this.getScreenHeight() - this.categoryPanelY - 40) / PANEL_ROW_HEIGHT;
        //$$ this.categoryPanelRows = Math.max(3, Math.min(visibleRows, this.categories.size() + 1));
        //#endif
        int totalRows = this.categories.size() + 1;
        int maxScroll = Math.max(0, totalRows - this.categoryPanelRows);
        this.categoryPanelScroll = Math.max(0, Math.min(this.categoryPanelScroll, maxScroll));

        int height = this.categoryPanelRows * PANEL_ROW_HEIGHT + 6;
        this.drawRect(graphics, this.categoryPanelX - 1, this.categoryPanelY - 1,
                this.categoryPanelX + panelWidth + 1, this.categoryPanelY + height + 1, 0xB0606878);
        this.drawRect(graphics, this.categoryPanelX, this.categoryPanelY,
                this.categoryPanelX + panelWidth, this.categoryPanelY + height, 0xE018202C);

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
            boolean hovered = mouseX >= this.categoryPanelX && mouseX < this.categoryPanelX + panelWidth &&
                    mouseY >= y && mouseY < y + PANEL_ROW_HEIGHT;
            if (hovered)
            {
                this.drawRect(graphics, this.categoryPanelX, y - 1,
                        this.categoryPanelX + panelWidth, y + PANEL_ROW_HEIGHT - 1, 0x40FFFFFF);
            }
            label = this.mc.font.plainSubstrByWidth(label, panelWidth - 16);
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
        for (KeymapKeyboardLayout.Key key : this.keyCells)
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
        //#if MC >= 26.3
        boolean ignored = IgnoredKeySelection.isIgnored(key.code(), this.ignoredKeyboardKeys,
                Configs.KEYBIND_INVERT_IGNORED_KEYS.getBooleanValue());
        var indicators = KeymapKeyboardStyle.indicators(this.keyboardBindings, key.codes(), ignored);
        KeymapKeyboardRenderer.drawKey(graphics, this.mc.font, key, indicators,
                (this.rebindingEntry != null && this.bindingMode == KeymapBindingMode.VIRTUAL
                        ? this.rebindDraft.keys() : this.selectedCombo).containsAll(key.codes()), key.contains(mouseX, mouseY));
        //#else
    //$$
    //$$     int cellCode = key.code();
    //$$     boolean mouseKey = key.mouse();
    //$$     int displayCode = mouseKey ? -cellCode - 1 : cellCode;
    //$$     int left = key.x();
    //$$     int right = left + key.width();
    //$$     int y = key.y();
    //$$     int cellWidth = key.width();
    //$$     int cellHeight = key.height();
    //$$     {
    //$$         boolean hasVanilla;
    //$$         boolean hasMalilib = false;
    //$$         if (key.codes().size() > 1)
    //$$         {
    //$$             hasVanilla = this.countCompositeKey(key.codes(), true) > 0;
    //$$             hasMalilib = this.countCompositeKey(key.codes(), false) > 0;
    //$$         }
    //$$         else if (mouseKey)
    //$$         {
    //$$             hasVanilla = this.countVanillaMouse(displayCode) > 0;
    //$$         }
    //$$         else
    //$$         {
    //$$             hasVanilla = this.countVanillaKeyboard(cellCode) > 0;
    //$$             hasMalilib = this.countMalilib(cellCode) > 0;
    //$$         }
    //$$         boolean selected = this.selectedCombo.containsAll(key.codes());
    //$$         ConflictKinds conflictKinds = this.keyConflictKinds(key.codes());
    //$$
    //$$         int sourceFill = 0x66202028;
    //$$         if (hasVanilla && hasMalilib)
    //$$         {
    //$$             sourceFill = 0x66205038;
    //$$         }
    //$$         else if (hasVanilla)
    //$$         {
    //$$             sourceFill = 0x66203850;
    //$$         }
    //$$         else if (hasMalilib)
    //$$         {
    //$$             sourceFill = 0x66285028;
    //$$         }
    //$$
            //#if MC >= 26.3
    //$$         this.drawRect(graphics, left, y, right, y + cellHeight,
    //$$                 selected ? 0x90605820 : sourceFill);
            //#else
            //$$ int split = left + cellWidth / 2;
            //$$ if (selected)
            //$$ {
            //$$     this.drawRect(graphics, left, y, right, y + cellHeight, 0x90605820);
            //$$ }
            //$$ else if (conflictKinds.any())
            //$$ {
            //$$     this.drawRect(graphics, left, y, split, y + cellHeight, sourceFill);
            //$$     if (conflictKinds.both())
            //$$     {
            //$$         int conflictSplitY = y + cellHeight / 2;
            //$$         this.drawRect(graphics, split, y, right, conflictSplitY, SINGLE_CONFLICT_FILL);
            //$$         this.drawRect(graphics, split, conflictSplitY, right, y + cellHeight,
            //$$                 COMBINATION_CONFLICT_FILL);
            //$$     }
            //$$     else
            //$$     {
            //$$         this.drawRect(graphics, split, y, right, y + cellHeight,
            //$$                 conflictKinds.combination() ? COMBINATION_CONFLICT_FILL : SINGLE_CONFLICT_FILL);
            //$$     }
            //$$ }
            //$$ else
            //$$ {
            //$$     this.drawRect(graphics, left, y, right, y + cellHeight, sourceFill);
            //$$ }
            //#endif
    //$$
    //$$         int sourceBorder = selected ? 0xFFF0D080 : (hasVanilla || hasMalilib) ? 0xFF787888 : 0xFF3C3C46;
    //$$         this.drawRect(graphics, left, y, right, y + 1, sourceBorder);
    //$$         this.drawRect(graphics, left, y + cellHeight - 1, right, y + cellHeight, sourceBorder);
    //$$         this.drawRect(graphics, left, y, left + 1, y + cellHeight, sourceBorder);
    //$$         this.drawRect(graphics, right - 1, y, right, y + cellHeight, sourceBorder);
    //$$         if (!selected && conflictKinds.any())
    //$$         {
                //#if MC >= 26.3
    //$$             int markerLeft = Math.max(left + 1, right - 4);
    //$$             int markerRight = right - 1;
    //$$             int markerMiddle = y + cellHeight / 2;
    //$$             if (conflictKinds.single())
    //$$             {
    //$$                 this.drawRect(graphics, markerLeft, y + 2, markerRight,
    //$$                         markerMiddle - 1, SINGLE_CONFLICT_BORDER);
    //$$             }
    //$$             if (conflictKinds.combination())
    //$$             {
    //$$                 this.drawRect(graphics, markerLeft, markerMiddle + 1, markerRight,
    //$$                         y + cellHeight - 2, COMBINATION_CONFLICT_BORDER);
    //$$             }
                //#else
                //$$ if (conflictKinds.both())
                //$$ {
                //$$     int conflictSplitY = y + cellHeight / 2;
                //$$     this.drawRect(graphics, split, y, right, y + 1, SINGLE_CONFLICT_BORDER);
                //$$     this.drawRect(graphics, split, conflictSplitY - 1,
                //$$             right, conflictSplitY + 1, COMBINATION_CONFLICT_BORDER);
                //$$     this.drawRect(graphics, split, y + cellHeight - 1,
                //$$             right, y + cellHeight, COMBINATION_CONFLICT_BORDER);
                //$$ }
                //$$ else
                //$$ {
                //$$     int conflictBorder = conflictKinds.combination()
                //$$             ? COMBINATION_CONFLICT_BORDER : SINGLE_CONFLICT_BORDER;
                //$$     this.drawRect(graphics, split, y, right, y + 1, conflictBorder);
                //$$     this.drawRect(graphics, split, y + cellHeight - 1, right, y + cellHeight, conflictBorder);
                //$$ }
                //$$ int rightBorder = conflictKinds.combination()
                //$$         ? COMBINATION_CONFLICT_BORDER : SINGLE_CONFLICT_BORDER;
                //$$ this.drawRect(graphics, right - 1, y, right, y + cellHeight, rightBorder);
                //#endif
    //$$         }
    //$$
    //$$         String label = mouseKey
    //$$                 ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + displayCode)
    //$$                 : key.label();
    //$$         this.drawCenteredFittedKeyLabel(graphics, label, left, right, y, cellHeight, 0xFFE0E0E0);
    //$$
    //$$     }
    //$$
        //#endif
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

    //#if MC >= 26.3
    private int countVanillaKeyboard(int code)
    {
        return (int) this.allEntries.stream().filter(BrowserEntry::isVanilla)
                .filter(entry -> this.entryKeySet(entry).contains(code)).count();
    }
    //#else
    //$$ private int countVanillaKeyboard(int code)
    //$$ {
    //$$     int count = 0;
    //$$     for (BrowserEntry entry : this.allEntries)
    //$$     {
    //$$         if (entry.isVanilla() && !entry.mapping().isUnbound() &&
    //$$             InputCompat.isKeyboardKey(((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey()) &&
    //$$             vanillaKeyCode(entry.mapping()) == code)
    //$$         {
    //$$             count++;
    //$$         }
    //$$     }
    //$$     return count;
    //$$ }
    //#endif

    //#if MC >= 26.3
    private int countVanillaMouse(int code)
    {
        return this.countVanillaKeyboard(-code - 1);
    }
    //#else
    //$$ private int countVanillaMouse(int code)
    //$$ {
    //$$     int count = 0;
    //$$     for (BrowserEntry entry : this.allEntries)
    //$$     {
    //$$         if (entry.isVanilla() && !entry.mapping().isUnbound() &&
    //$$             ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey().getType() == InputConstants.Type.MOUSE &&
    //$$             vanillaMouseCode(entry.mapping()) == code)
    //$$         {
    //$$             count++;
    //$$         }
    //$$     }
    //$$     return count;
    //$$ }
    //#endif

    //#if MC >= 26.3
    private int countMalilib(int code)
    {
        return (int) this.allEntries.stream().filter(entry -> !entry.isVanilla())
                .filter(entry -> this.entryKeySet(entry).contains(code)).count();
    }
    //#else
    //$$ private int countMalilib(int code)
    //$$ {
    //$$     int count = 0;
    //$$     for (BrowserEntry entry : this.allEntries)
    //$$     {
    //$$         if (!entry.isVanilla() && entry.hotkey().getKeybind().getKeys().contains(code))
    //$$         {
    //$$             count++;
    //$$         }
    //$$     }
    //$$     return count;
    //$$ }
    //#endif

    private int countCompositeKey(List<Integer> codes, boolean vanilla)
    {
        int count = 0;
        for (BrowserEntry entry : this.allEntries)
        {
            if (entry.isVanilla() == vanilla && this.entryKeySet(entry).containsAll(codes))
            {
                count++;
            }
        }
        return count;
    }

    private ConflictKinds keyConflictKinds(List<Integer> codes)
    {
        boolean singleConflict = false;
        boolean combinationConflict = false;
        for (BrowserEntry entry : this.allEntries)
        {
            if (!this.entryKeySet(entry).containsAll(codes))
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
        //#if MC < 26.3
        //$$ if (code >= 65 && code <= 90)
        //$$ {
            //$$ return String.valueOf((char) code);
        //$$ }
        //$$ if (code >= 48 && code <= 57)
        //$$ {
            //$$ return String.valueOf((char) code);
        //$$ }
        //$$ if (code >= 290 && code <= 301)
        //$$ {
            //$$ return "F" + (code - 289);
        //$$ }
        //$$ if (code >= 320 && code <= 329)
        //$$ {
            //$$ return "N" + (code - 320);
        //$$ }
        //$$ switch (code)
        //$$ {
            //$$ case 256: return "ESC";
            //$$ case 257: return "⏎";
            //$$ case 258: return "TAB";
            //$$ case 259: return "⌫";
            //$$ case 260: return "INS";
            //$$ case 261: return "DEL";
            //$$ case 262: return "→";
            //$$ case 263: return "←";
            //$$ case 264: return "↑";
            //$$ case 265: return "↓";
            //$$ case 266: return "PGU";
            //$$ case 267: return "PGD";
            //$$ case 268: return "HOM";
            //$$ case 269: return "END";
            //$$ case 280: return "CAPS";
            //$$ case 281: return "SCR";
            //$$ case 282: return "NUM";
            //$$ case 283: return "PRT";
            //$$ case 284: return "PAU";
            //$$ case 32: return "SPACE";
            //$$ case 330: return "N.";
            //$$ case 331: return "N/";
            //$$ case 332: return "N*";
            //$$ case 333: return "N-";
            //$$ case 334: return "N+";
            //$$ case 335: return "N⏎";
            //$$ case 340: return "L⇧";
            //$$ case 341: return "LCT";
            //$$ case 342: return "LAL";
            //$$ case 343: return "LWIN";
            //$$ case 344: return "R⇧";
            //$$ case 345: return "RCT";
            //$$ case 346: return "RAL";
            //$$ case 347: return "RWIN";
            //$$ case 348: return "MENU";
            //$$ default:
                //$$ String name = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
                //$$ return name.length() > 5 ? name.substring(0, 5) : name;
        //$$ }
        //#else
        String name = InputCompat.keyboardKey(code).getDisplayName().getString();
        return name.length() > 5 ? name.substring(0, 5) : name;
        //#endif
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
        //#if MC >= 26.3
        lines.add(entry.displayName() != null ? entry.displayName() : entry.action());
        if (entry == this.rebindingEntry && this.rebindDraft.touched())
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.pending_keys", this.keyNames(this.rebindDraft.keys())));
        //#endif
        if (entry.conflictType() == ConflictType.COMBINATION)
        {
            //#if MC >= 26.3
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.conflict_type.combination"));
            //#else
            //$$ lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.legend.combination"));
            //#endif
        }
        else if (entry.conflictType() == ConflictType.SINGLE)
        {
            //#if MC >= 26.3
            lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.conflict_type.single"));
            //#else
            //$$ lines.add(StringUtils.translate("halfmasa.gui.keymap_browser.legend.single"));
            //#endif
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
        //#if MC >= 26.3
        List<String> wrapped = new ArrayList<>();
        int maxWidth = Math.max(1, this.getScreenWidth() - 24);
        for (String line : lines)
        {
            String remaining = line;
            do
            {
                String part = this.mc.font.plainSubstrByWidth(remaining, maxWidth);
                if (part.isEmpty() && !remaining.isEmpty()) part = remaining.substring(0, 1);
                wrapped.add(part);
                remaining = remaining.substring(part.length());
            }
            while (!remaining.isEmpty());
        }
        int maxLines = Math.max(1, (this.getScreenHeight() - 20) / 11);
        if (wrapped.size() > maxLines)
        {
            wrapped = new ArrayList<>(wrapped.subList(0, maxLines));
            wrapped.set(maxLines - 1, "...");
        }
        lines = wrapped;
        //#endif
        int width = 0;
        for (String line : lines)
        {
            width = Math.max(width, this.mc.font.width(line));
        }
        int x = Math.max(6, Math.min(mouseX + 8, this.getScreenWidth() - width - 12));
        int y = Math.max(6, Math.min(mouseY + 4, this.getScreenHeight() - lines.size() * 11 - 8));
        this.drawRect(graphics, x - 3, y - 2, x + width + 5, y + lines.size() * 11 + 2, 0xF0100018);
        for (int index = 0; index < lines.size(); index++)
        {
            this.drawString(graphics, lines.get(index), x, y + index * 11,
                    index == 0 ? 0xFFFFC860 : 0xFFF0F0F0);
        }
    }

    //#if MC >= 26.3
    private List<Integer> orderedEntryKeys(BrowserEntry entry)
    {
        List<Integer> draft = this.draftKeys.get(entryId(entry));
        if (draft != null) return draft;
        if (!entry.isVanilla())
        {
            return entry.hotkey().getKeybind().getKeys().stream().map(KeymapBrowserScreen::normalizeMasaCode).toList();
        }
        List<Integer> customCombo = this.customization(entry).comboKeys;
        if (!customCombo.isEmpty()) return customCombo;
        InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
        if (InputConstants.UNKNOWN.equals(key)) return List.of();
        List<Integer> keys = new ArrayList<>();
        if (this.isDebugOnlyMapping(entry.mapping()) && this.mc.options != null && this.mc.options.keyDebugModifier != null)
        {
            KeyMapping modifier = this.mc.options.keyDebugModifier;
            List<Integer> modifierKeys = this.draftKeys.get(modifier.getName());
            if (modifierKeys != null) keys.addAll(modifierKeys);
            else
            {
                InputConstants.Key modifierKey = ((KeyMappingAccessor) modifier).halfmasa$getBoundKey();
                if (InputCompat.isKeyboardKey(modifierKey)) keys.add(modifierKey.getValue());
            }
        }
        keys.add(key.getType() == InputConstants.Type.MOUSE
                ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue());
        return keys;
    }

    private static int normalizeMasaCode(int code)
    {
        return code < -80 && code > -100 ? InputCompat.mouseButtonToLayoutCode(code + 100) : code;
    }
    //#else
    //$$ private List<Integer> orderedEntryKeys(BrowserEntry entry)
    //$$ {
    //$$     if (!entry.isVanilla())
    //$$     {
    //$$         return new ArrayList<>(entry.hotkey().getKeybind().getKeys());
    //$$     }
    //$$     List<Integer> customCombo = KeybindCustomizationStore.getInstance().comboKeys(entry.mapping());
    //$$     if (!customCombo.isEmpty())
    //$$     {
    //$$         return customCombo;
    //$$     }
    //$$     if (entry.mapping().isUnbound())
    //$$     {
    //$$         return List.of();
    //$$     }
    //$$     InputConstants.Key key = ((KeyMappingAccessor) entry.mapping()).halfmasa$getBoundKey();
    //$$     List<Integer> keys = new ArrayList<>();
    //$$     keys.add(key.getType() == InputConstants.Type.MOUSE
    //$$             ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue());
    //$$     //#if MC >= 26.2
    //$$     if (this.isDebugOnlyMapping(entry.mapping()) && this.mc.options != null &&
    //$$             this.mc.options.keyDebugModifier != null)
    //$$     {
    //$$         InputConstants.Key modifier =
    //$$                 ((KeyMappingAccessor) this.mc.options.keyDebugModifier).halfmasa$getBoundKey();
    //$$         if (InputCompat.isKeyboardKey(modifier))
    //$$         {
    //$$             keys.add(modifier.getValue());
    //$$         }
    //$$     }
    //$$     //#endif
    //$$     return keys;
    //$$ }
    //#endif

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

    //#if MC >= 26.3
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (this.categoryPanelOpen && this.handleClick(event.x(), event.y(), event.button(), event.hasControlDown())) return true;
        // Buttons and the search field must be handled before physical mouse capture.
        // GuiBase.mouseClicked returns false even when its buttons handled the event.
        if (super.onMouseClicked(event, doubleClick)) return true;
        if (this.handleClick(event.x(), event.y(), event.button(), event.hasControlDown()))
        {
            return true;
        }
        if (this.rebindingEntry != null && this.bindingMode == KeymapBindingMode.PHYSICAL)
        {
            int code = InputCompat.mouseButtonToLayoutCode(event.button());
            this.rebindDraft.press(code);
            return true;
        }
        return false;
    }
    //#elseif MC >= 1.21.10
    //$$ @Override
    //$$ public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    //$$ {
        //$$ if (this.handleClick(event.x(), event.y(), event.button(), event.hasControlDown()))
        //$$ {
            //$$ return true;
        //$$ }
        //$$ return super.onMouseClicked(event, doubleClick);
    //$$ }
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
        if (!InputCompat.isPrimaryMouseButton(button))
        {
            return false;
        }

        if (this.categoryPanelOpen)
        {
            //#if MC >= 26.3
            this.updateCategoryPanelBounds();
            int panelWidth = this.categoryPanelWidth;
            //#else
            //$$ int panelWidth = PANEL_WIDTH;
            //#endif
            int panelHeight = this.categoryPanelRows * PANEL_ROW_HEIGHT + 6;
            if (mouseX >= this.categoryPanelX && mouseX <= this.categoryPanelX + panelWidth &&
                mouseY >= this.categoryPanelY && mouseY <= this.categoryPanelY + panelHeight)
            {
                int panelRow = (int) Math.floor((mouseY - this.categoryPanelY - 3) / PANEL_ROW_HEIGHT);
                int index = this.categoryPanelScroll + panelRow;
                if (panelRow >= 0 && panelRow < this.categoryPanelRows && index <= this.categories.size())
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
        int listBottom = this.listBottom();

        if (this.showKeyboard && mouseY >= this.keyboardTop() && mouseY < listTop)
        {
            for (KeymapKeyboardLayout.Key cell : this.keyCells)
            {
                if (mouseX >= cell.x() && mouseX < cell.x() + cell.width() &&
                    mouseY >= cell.y() && mouseY < cell.y() + cell.height())
                {
                    List<Integer> comboCodes = cell.codes();
                    //#if MC >= 26.3
                    if (this.rebindingEntry != null)
                    {
                        if (this.bindingMode == KeymapBindingMode.PHYSICAL) return false;
                        this.rebindDraft.toggle(comboCodes);
                        return true;
                    }
                    //#endif
                    if (ctrlDown)
                    {
                        if (this.selectedCombo.containsAll(comboCodes))
                        {
                            this.selectedCombo.removeAll(comboCodes);
                        }
                        else
                        {
                            this.selectedCombo.addAll(comboCodes);
                        }
                    }
                    else if (this.selectedCombo.equals(Set.copyOf(comboCodes)))
                    {
                        this.selectedCombo.clear();
                    }
                    else
                    {
                        this.selectedCombo.clear();
                        this.selectedCombo.addAll(comboCodes);
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

        if (mouseY >= listTop && mouseY < listBottom && mouseX >= 10 &&
                mouseX < this.getScreenWidth() - 21)
        {
            List<DisplayRow> rows = this.displayedRows();
            int index = this.scrollOffset + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (index >= 0 && index < rows.size())
            {
                DisplayRow row = rows.get(index);
                if (row.isGroup())
                {
                    //#if MC >= 26.3
                    if (this.isBestMatchGroup(row.group())) return true;
                    //#endif
                    this.toggleGroup(row.group());
                    return true;
                }
                BrowserEntry entry = row.entry();
                RowColumns columns = this.rowColumns(10, this.getScreenWidth() - 34);
                //#if MC >= 26.3
                if (this.rebindingEntry != null && this.rebindDraft.touched())
                {
                    if (entry == this.rebindingEntry && mouseX >= columns.confirmX() && mouseX < columns.keyX())
                        this.confirmRebind();
                    return true;
                }
                if (mouseX >= columns.wheelX())
                {
                    this.toggleWheelParticipation(entry);
                    return true;
                }
                //#endif
                if (mouseX >= columns.detailX())
                {
                    //#if MC >= 26.3
                    this.toggleKeyOrder(entry);
                    //#else
                    //$$ this.openDetail(entry);
                    //#endif
                    return true;
                }
                if (mouseX >= columns.resetX())
                {
                    this.resetKey(entry);
                    return true;
                }
                if (mouseX >= columns.keyX())
                {
                    this.beginRebind(entry);
                    return true;
                }
                //#if MC < 26.3
                //$$ // Keep the existing wheel-context shortcut, without stealing the
                //$$ // explicit key and detail controls from the row.
                //$$ if (ctrlDown && entry.isVanilla())
                //$$ {
                //$$     KeybindCustomizationStore.Entry data =
                //$$             KeybindCustomizationStore.getInstance().get(entry.mapping());
                //$$     data.activationContext = data.activationContext.next();
                //$$     KeybindCustomizationStore.getInstance().save();
                //$$     return true;
                //$$ }
                //$$ this.openDetail(entry);
                //#endif
                return true;
            }
        }
        return false;
    }

    //#if MC < 26.3
    //$$ private void openDetail(BrowserEntry entry)
    //$$ {
    //$$     KeybindDetailScreen detail = new KeybindDetailScreen(entry);
    //$$     detail.setParent(this);
    //$$     GuiBase.openGui(detail);
    //$$ }
    //#endif

    private void beginRebind(BrowserEntry entry)
    {
        this.rebindingEntry = entry;
        //#if MC >= 26.3
        this.rebindDraft.reset();
        if (this.bindingMode == KeymapBindingMode.VIRTUAL && !this.showKeyboard)
        {
            this.showKeyboard = true;
            this.initGui();
        }
        //#endif
        this.pendingRebindKeys.clear();
        this.heldRebindKeys.clear();
    }

    private void cancelRebind()
    {
        this.rebindingEntry = null;
        //#if MC >= 26.3
        this.rebindDraft.reset();
        //#endif
        this.pendingRebindKeys.clear();
        this.heldRebindKeys.clear();
    }

    //#if MC >= 26.3
    private void applyRebind(List<Integer> keys)
    {
        BrowserEntry entry = this.rebindingEntry;
        if (entry == null) return;
        this.originalKeys.computeIfAbsent(entryId(entry), id -> List.copyOf(this.orderedEntryKeys(entry)));
        this.draftKeys.put(entryId(entry), List.copyOf(keys));
        if (entry.isVanilla())
        {
            KeybindCustomizationStore.Entry data = this.customization(entry);
            data.comboKeys.clear();
            if (keys.size() > 1) data.comboKeys.addAll(keys);
            else data.requireKeyOrder = false;
        }
        this.cancelRebind();
        this.initGui();
    }
    //#else
    //$$ private void applyRebind(List<Integer> keys)
    //$$ {
    //$$     BrowserEntry entry = this.rebindingEntry;
    //$$     if (entry == null) return;
    //$$     if (entry.isVanilla())
    //$$     {
    //$$         KeyMapping mapping = entry.mapping();
    //$$         KeybindCustomizationStore.Entry customization =
    //$$                 KeybindCustomizationStore.getInstance().get(mapping);
    //$$         customization.comboKeys.clear();
    //$$         if (keys.size() <= 1) customization.requireKeyOrder = false;
    //$$         if (keys.size() > 1)
    //$$         {
    //$$             mapping.setKey(InputConstants.UNKNOWN);
    //$$             customization.comboKeys.addAll(keys);
    //$$         }
    //$$         else
    //$$         {
    //$$             int code = keys.isEmpty() ? 0 : keys.get(0);
    //$$             mapping.setKey(keys.isEmpty() ? InputConstants.UNKNOWN
    //$$                     : code < 0 ? InputConstants.Type.MOUSE.getOrCreate(
    //$$                             InputCompat.layoutCodeToMouseButton(code))
    //$$                     : InputCompat.keyboardKey(code));
    //$$         }
    //$$         KeyMapping.resetMapping();
    //$$         this.mc.options.save();
    //$$         KeybindCustomizationStore.getInstance().save();
    //$$     }
    //$$     else
    //$$     {
    //$$         entry.hotkey().getKeybind().clearKeys();
    //$$         for (int code : keys)
    //$$         {
    //$$             if (code >= 0) entry.hotkey().getKeybind().addKey(code);
    //$$         }
    //$$         InputEventHandler.getKeybindManager().updateUsedKeys();
    //$$         ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
    //$$     }
    //$$     KeybindPieManager.getInstance().invalidateCustomMappingSync();
    //$$     this.cancelRebind();
    //$$     this.initGui();
    //$$ }
    //#endif

    //#if MC >= 26.3
    private void resetKey(BrowserEntry entry)
    {
        this.beginRebind(entry);
        if (entry.isVanilla())
        {
            InputConstants.Key key = entry.mapping().getDefaultKey();
            this.applyRebind(key == null || key.equals(InputConstants.UNKNOWN) ? List.of()
                    : List.of(key.getType() == InputConstants.Type.MOUSE
                    ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue()));
        }
        else
        {
            var binding = entry.hotkey().getKeybind();
            KeybindMulti defaults = KeybindMulti.fromStorageString(binding.getDefaultStringValue(), this.hotkeySettings(entry.hotkey()));
            this.applyRebind(defaults.getKeys().stream().map(KeymapBrowserScreen::normalizeMasaCode).toList());
        }
    }
    //#else
    //$$ private void resetKey(BrowserEntry entry)
    //$$ {
    //$$     this.beginRebind(entry);
    //$$     if (entry.isVanilla())
    //$$     {
    //$$         InputConstants.Key key = entry.mapping().getDefaultKey();
    //$$         this.applyRebind(key == null || key.equals(InputConstants.UNKNOWN) ? List.of()
    //$$                 : List.of(key.getType() == InputConstants.Type.MOUSE
    //$$                 ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue()));
    //$$     }
    //$$     else
    //$$     {
    //$$         entry.hotkey().resetToDefault();
    //$$         KeybindCustomizationStore.Entry customization =
    //$$                 KeybindCustomizationStore.getInstance().get(entry.hotkey().getName());
    //$$         customization.requireKeyOrder = false;
    //$$         KeybindCustomizationStore.getInstance().save();
    //$$         InputEventHandler.getKeybindManager().updateUsedKeys();
    //$$         ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
    //$$         KeybindPieManager.getInstance().invalidateCustomMappingSync();
    //$$         this.cancelRebind();
    //$$         this.initGui();
    //$$     }
    //$$ }
    //#endif

    //#if MC >= 26.3
    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (this.rebindingEntry != null)
        {
            int code = event.key();
            if (code == InputCompat.escapeKeyCode())
            {
                this.rebindDraft.clear();
                this.heldRebindKeys.clear();
            }
            else if (code > 0)
            {
                this.heldRebindKeys.add(code);
                if (this.bindingMode == KeymapBindingMode.PHYSICAL) this.rebindDraft.press(code);
            }
            return true;
        }
        if (event.key() == InputCompat.escapeKeyCode() && !this.selectedCombo.isEmpty())
        {
            this.selectedCombo.clear();
            this.refilter();
            return true;
        }
        if (event.key() == InputCompat.escapeKeyCode())
        {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (this.rebindingEntry != null)
        {
            this.heldRebindKeys.remove(event.key());
            return true;
        }
        return super.keyReleased(event);
    }
    //#endif

    private void handleScrollbarDrag(double mouseY)
    {
        List<DisplayRow> rows = this.displayedRows();
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
        //#if MC >= 26.3
        int panelWidth = this.categoryPanelWidth;
        //#else
        //$$ int panelWidth = PANEL_WIDTH;
        //#endif
        if (this.categoryPanelOpen &&
            mouseX >= this.categoryPanelX && mouseX <= this.categoryPanelX + panelWidth &&
            mouseY >= this.categoryPanelY && mouseY < this.categoryPanelY +
                    this.categoryPanelRows * PANEL_ROW_HEIGHT + 6)
        {
            this.categoryPanelScroll -= (int) Math.signum(deltaY) * 2;
            return true;
        }
        if (mouseY >= this.listTop())
        {
            boolean shiftDown = InputCompat.isKeyDown(Minecraft.getInstance(), InputConstants.KEY_LSHIFT);
            boolean ctrlDown = InputCompat.isKeyDown(Minecraft.getInstance(), InputConstants.KEY_LCONTROL);
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
