package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//#if MC < 1.21.10
//$$ import net.minecraft.client.gui.screens.Screen;
//#endif
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore.ActivationContext;
import io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen.BrowserEntry;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;

/** A draft-based editor for one vanilla or MaLiLib binding. */
public final class KeybindDetailScreen extends GuiBase
{
    private final BrowserEntry entry;
    private final InputConstants.Key originalVanillaKey;
    private final InputConstants.Key defaultVanillaKey;
    private final List<Integer> originalVanillaKeys;
    private final List<Integer> defaultVanillaKeys;
    private final List<Integer> originalHotkeyKeys;
    private final List<Integer> defaultHotkeyKeys;
    private final String originalDisplayName;
    private final boolean originalHideCategory;
    private final Integer originalSectorColor;
    private final ActivationContext originalActivationContext;
    private InputConstants.Key draftVanillaKey;
    private final List<Integer> draftVanillaKeys;
    private final List<Integer> draftHotkeyKeys;
    private String draftDisplayName;
    private boolean draftHideCategory;
    private Integer draftSectorColor;
    private ActivationContext draftActivationContext;
    private final boolean originalRequireKeyOrder;
    private boolean draftRequireKeyOrder;
    private final List<Integer> pendingKeys = new ArrayList<>();
    private final Set<Integer> physicalKeysDown = new HashSet<>();
    private List<KeymapKeyboardLayout.Key> keyboardKeys = List.of();
    private boolean capturing;
    private boolean physicalCapture;
    private int keyboardY;
    private int infoY;
    private int introY;
    private int controlY;
    private int wheelY;

    public KeybindDetailScreen(BrowserEntry entry)
    {
        this.entry = entry;
        KeybindCustomizationStore.Entry customization = this.isVanilla()
                ? this.store().get(entry.mapping()) : this.store().get(entry.hotkey().getName());
        this.originalDisplayName = customization.displayName;
        this.originalHideCategory = customization.hideCategory;
        this.originalSectorColor = customization.sectorColor;
        this.originalActivationContext = customization.activationContext;
        this.originalRequireKeyOrder = customization.requireKeyOrder;
        this.draftRequireKeyOrder = this.originalRequireKeyOrder;
        this.draftDisplayName = this.originalDisplayName;
        this.draftHideCategory = this.originalHideCategory;
        this.draftSectorColor = this.originalSectorColor;
        this.draftActivationContext = this.originalActivationContext;

        if (this.isVanilla())
        {
            KeyMapping mapping = entry.mapping();
            this.originalVanillaKey = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
            this.defaultVanillaKey = mapping.getDefaultKey();
            this.originalVanillaKeys = this.readVanillaKeys(mapping, customization);
            this.defaultVanillaKeys = this.keyCodeList(this.defaultVanillaKey);
            this.originalHotkeyKeys = List.of();
            this.defaultHotkeyKeys = List.of();
            this.draftVanillaKey = this.originalVanillaKey;
            this.draftVanillaKeys = new ArrayList<>(this.originalVanillaKeys);
            this.draftHotkeyKeys = new ArrayList<>();
        }
        else
        {
            this.originalVanillaKey = InputConstants.UNKNOWN;
            this.defaultVanillaKey = InputConstants.UNKNOWN;
            this.originalVanillaKeys = List.of();
            this.defaultVanillaKeys = List.of();
            this.originalHotkeyKeys = new ArrayList<>(entry.hotkey().getKeybind().getKeys());
            this.defaultHotkeyKeys = this.readDefaultHotkeyKeys(entry.hotkey());
            this.draftHotkeyKeys = new ArrayList<>(this.originalHotkeyKeys);
            this.draftVanillaKey = InputConstants.UNKNOWN;
            this.draftVanillaKeys = new ArrayList<>();
        }
        this.setTitle(StringUtils.translate("halfmasa.gui.keybind_detail.title", entry.action()));
    }

    private List<Integer> readDefaultHotkeyKeys(IHotkey hotkey)
    {
        List<Integer> current = new ArrayList<>(hotkey.getKeybind().getKeys());
        KeybindSettings settings = hotkey.getKeybind().getSettings();
        hotkey.resetToDefault();
        List<Integer> result = new ArrayList<>(hotkey.getKeybind().getKeys());
        hotkey.getKeybind().clearKeys();
        for (int key : current) hotkey.getKeybind().addKey(key);
        hotkey.getKeybind().setSettings(settings);
        return result;
    }

    private void applyHotkeyOrderSetting(IHotkey hotkey)
    {
        KeybindSettings current = hotkey.getKeybind().getSettings();
        hotkey.getKeybind().setSettings(KeybindSettings.create(
                current.getContext(), current.getActivateOn(), current.getAllowEmpty(),
                current.getAllowExtraKeys(), this.draftRequireKeyOrder,
                current.isExclusive(), current.shouldCancel()));
    }

    private List<Integer> readVanillaKeys(KeyMapping mapping, KeybindCustomizationStore.Entry customization)
    {
        if (customization != null && customization.comboKeys != null && !customization.comboKeys.isEmpty())
        {
            return new ArrayList<>(customization.comboKeys);
        }
        return this.keyCodeList(((KeyMappingAccessor) mapping).halfmasa$getBoundKey());
    }

    private List<Integer> keyCodeList(InputConstants.Key key)
    {
        if (key == null || key.equals(InputConstants.UNKNOWN)) return new ArrayList<>();
        return new ArrayList<>(List.of(key.getType() == InputConstants.Type.MOUSE
                ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue()));
    }

    private boolean isVanilla()
    {
        return this.entry.mapping() != null;
    }

    private KeybindCustomizationStore.Entry storeEntry()
    {
        return this.store().get(this.entry.mapping());
    }

    private KeybindCustomizationStore store()
    {
        return KeybindCustomizationStore.getInstance();
    }

    private String keyText(InputConstants.Key key)
    {
        return key == null || key.equals(InputConstants.UNKNOWN)
                ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound") : key.getDisplayName().getString();
    }

    private String currentKeyText()
    {
        if (this.isVanilla()) return this.keyListText(this.draftVanillaKeys);
        if (this.draftHotkeyKeys.isEmpty()) return StringUtils.translate("halfmasa.gui.keymap_browser.unbound");
        List<String> names = new ArrayList<>();
        for (int code : this.draftHotkeyKeys)
            names.add(InputCompat.keyboardKey(code).getDisplayName().getString());
        return String.join(" + ", names);
    }

    private String keyListText(List<Integer> keys)
    {
        if (keys.isEmpty()) return StringUtils.translate("halfmasa.gui.keymap_browser.unbound");
        List<String> names = new ArrayList<>();
        for (int code : keys)
        {
            names.add(code < 0
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1))
                    : InputCompat.keyboardKey(code).getDisplayName().getString());
        }
        return String.join(" + ", names);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        int x = 10;
        int y = 34;
        this.infoY = y + 6;
        this.introY = this.infoY + 30;
        this.controlY = this.introY + this.descriptionLineCount(this.getScreenWidth() - 20) * 11 + 8;
        int orderX = Math.min(230, Math.max(10, this.getScreenWidth() - 160));
        this.addButton(new ButtonGeneric(orderX, this.infoY + 18, 150, 20,
                StringUtils.translate(this.draftRequireKeyOrder
                        ? "halfmasa.gui.keybind_detail.order_required"
                        : "halfmasa.gui.keybind_detail.order_any")),
                (button, mouseButton) -> {
                    this.draftRequireKeyOrder = !this.draftRequireKeyOrder;
                    this.initGui();
                });
        if (Configs.KEYMAP_DIRECT_REBIND.getBooleanValue())
        {
            this.addButton(new ButtonGeneric(x, this.controlY, 94, 20,
                    StringUtils.translate(this.capturing ? "halfmasa.gui.keybind_detail.capturing"
                            : "halfmasa.gui.keybind_detail.change_key")),
                    (button, mouseButton) -> this.beginCapture());
            this.addButton(new ButtonGeneric(x + 98, this.controlY, 94, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.clear_key")),
                    (button, mouseButton) -> this.clearDraftKey());
            this.addButton(new ButtonGeneric(x + 196, this.controlY, 124, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.reset_key")),
                    (button, mouseButton) -> this.resetDraftKey());
        }
        else
        {
            this.addButton(new ButtonGeneric(x, this.controlY, 94, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.clear_key")),
                    (button, mouseButton) -> this.clearDraftKey());
            this.addButton(new ButtonGeneric(x + 98, this.controlY, 124, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.reset_key")),
                    (button, mouseButton) -> this.resetDraftKey());
        }

        y = this.controlY + 28;
        if (this.isVanilla())
        {
            this.wheelY = y;
            int nameWidth = Math.min(240, Math.max(140, this.getScreenWidth() / 3));
            int nameX = x + 60;
            GuiTextFieldGeneric name = new GuiTextFieldGeneric(nameX, y + 20, nameWidth, 16, this.mc.font);
            name.setTextWrapper(this.draftDisplayName == null ? "" : this.draftDisplayName);
            name.setMaxLengthWrapper(128);
            this.addTextField(name, field -> {
                String value = field.getTextWrapper().trim();
                this.draftDisplayName = value.isEmpty() ? null : value;
                return true;
            });
            int contextX = nameX + nameWidth + 8;
            this.addButton(new ButtonGeneric(contextX, y + 18, 82, 20,
                    StringUtils.translate(this.draftActivationContext.translationKey())),
                    (button, mouseButton) -> {
                        this.draftActivationContext = this.draftActivationContext.next();
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(contextX + 86, y + 18, 92, 20,
                    StringUtils.translate(this.draftHideCategory
                            ? "halfmasa.gui.keybind_editor.category_hidden"
                            : "halfmasa.gui.keybind_editor.category_shown")),
                    (button, mouseButton) -> {
                        this.draftHideCategory = !this.draftHideCategory;
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(contextX + 182, y + 18, 96, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.reset_custom")),
                    (button, mouseButton) -> {
                        this.draftDisplayName = null;
                        this.draftHideCategory = false;
                        this.draftSectorColor = null;
                        this.draftActivationContext = ActivationContext.AUTO;
                        this.initGui();
                    });
            int colorX = contextX + 286;
            GuiTextFieldGeneric color = new GuiTextFieldGeneric(colorX, y + 18, 72, 20, this.mc.font);
            color.setTextWrapper(this.draftSectorColor == null ? "" : String.format("#%06X", this.draftSectorColor));
            color.setMaxLengthWrapper(7);
            this.addTextField(color, field -> {
                String value = field.getTextWrapper().trim();
                if (value.isEmpty()) { this.draftSectorColor = null; return true; }
                if (value.matches("#[0-9a-fA-F]{6}"))
                {
                    this.draftSectorColor = Integer.parseInt(value.substring(1), 16);
                    return true;
                }
                return false;
            });
            y += 52;
        }

        this.keyboardY = y + 20;
        this.keyboardKeys = KeymapKeyboardLayout.keys(x, this.keyboardY, this.getScreenWidth() - 20);
        int bottom = this.getScreenHeight() - 28;
        this.addButton(new ButtonGeneric(10, bottom, 110, 20,
                StringUtils.translate("halfmasa.gui.keybind_detail.save_exit")),
                (button, mouseButton) -> this.saveAndExit());
        this.addButton(new ButtonGeneric(126, bottom, 110, 20,
                StringUtils.translate("halfmasa.gui.keybind_detail.discard_exit")),
                (button, mouseButton) -> this.discardAndExit());
    }

    private void beginCapture()
    {
        if (!Configs.KEYMAP_DIRECT_REBIND.getBooleanValue())
        {
            return;
        }
        this.capturing = true;
        this.physicalCapture = true;
        this.pendingKeys.clear();
        this.physicalKeysDown.clear();
        this.initGui();
    }

    private boolean hasPending()
    {
        return !this.pendingKeys.isEmpty();
    }

    private boolean hasUnsavedChanges()
    {
        if (this.hasPending()) return true;
        if (this.isVanilla() && !this.draftVanillaKeys.equals(this.originalVanillaKeys)) return true;
        if (!this.isVanilla() && !this.draftHotkeyKeys.equals(this.originalHotkeyKeys)) return true;
        return !java.util.Objects.equals(this.draftDisplayName, this.originalDisplayName) ||
                this.draftHideCategory != this.originalHideCategory ||
                !java.util.Objects.equals(this.draftSectorColor, this.originalSectorColor) ||
                this.draftActivationContext != this.originalActivationContext ||
                this.draftRequireKeyOrder != this.originalRequireKeyOrder;
    }

    private void clearDraftKey()
    {
        this.pendingKeys.clear();
        this.capturing = false;
        this.physicalCapture = false;
        if (this.isVanilla())
        {
            this.draftVanillaKey = InputConstants.UNKNOWN;
            this.draftVanillaKeys.clear();
        }
        else this.draftHotkeyKeys.clear();
        this.initGui();
    }

    private void resetDraftKey()
    {
        this.pendingKeys.clear();
        this.capturing = false;
        this.physicalCapture = false;
        if (this.isVanilla())
        {
            this.draftVanillaKey = this.defaultVanillaKey;
            this.draftVanillaKeys.clear();
            this.draftVanillaKeys.addAll(this.defaultVanillaKeys);
            this.draftRequireKeyOrder = false;
        }
        else
        {
            this.draftHotkeyKeys.clear();
            this.draftHotkeyKeys.addAll(this.defaultHotkeyKeys);
        }
        this.initGui();
    }

    private boolean isConfirmKey(int keyCode)
    {
        //#if MC >= 1.21.1
        return false;
        //#else
        //$$ return Configs.KEYMAP_CONFIRM_SETTING.getKeybind().getKeys().contains(keyCode);
        //#endif
    }

    private void captureKeyboardKey(int keyCode)
    {
        if (keyCode == InputCompat.escapeKeyCode())
        {
            this.capturing = false;
            this.physicalCapture = false;
            this.pendingKeys.clear();
            this.physicalKeysDown.clear();
            this.initGui();
            return;
        }
        if (this.isConfirmKey(keyCode)) { this.commitPendingKeys(); return; }
        if (keyCode == InputCompat.backspaceKeyCode())
        {
            if (!this.pendingKeys.isEmpty()) this.pendingKeys.remove(this.pendingKeys.size() - 1);
            return;
        }
        if (this.isVanilla())
        {
            if (!this.pendingKeys.contains(keyCode)) this.pendingKeys.add(keyCode);
        }
        else if (!this.pendingKeys.contains(keyCode)) this.pendingKeys.add(keyCode);
        this.physicalKeysDown.add(keyCode);
    }

    private void clickKeyboardKey(KeymapKeyboardLayout.Key cell, boolean ctrlDown)
    {
        if (cell.mouse() && !this.isVanilla()) return;
        this.capturing = false;
        this.physicalCapture = false;
        if (this.isVanilla())
        {
            if (ctrlDown)
            {
                if (this.draftVanillaKeys.contains(cell.code())) this.draftVanillaKeys.remove((Integer) cell.code());
                else this.draftVanillaKeys.add(cell.code());
            }
            else
            {
                this.draftVanillaKeys.clear();
                this.draftVanillaKeys.add(cell.code());
            }
            this.draftVanillaKey = this.keyFromCode(cell.code());
        }
        else if (ctrlDown)
        {
            if (this.draftHotkeyKeys.contains(cell.code())) this.draftHotkeyKeys.remove((Integer) cell.code());
            else this.draftHotkeyKeys.add(cell.code());
        }
        else
        {
            this.draftHotkeyKeys.clear();
            this.draftHotkeyKeys.add(cell.code());
        }
        this.initGui();
    }

    private InputConstants.Key keyFromCode(int code)
    {
        return code < 0 ? InputConstants.Type.MOUSE.getOrCreate(InputCompat.layoutCodeToMouseButton(code))
                : InputCompat.keyboardKey(code);
    }

    private void commitPendingKeys()
    {
        if (this.pendingKeys.isEmpty())
        {
            this.capturing = false;
            this.physicalKeysDown.clear();
            this.initGui();
            return;
        }
        if (this.isVanilla())
        {
            this.draftVanillaKeys.clear();
            this.draftVanillaKeys.addAll(this.pendingKeys);
            this.draftVanillaKey = this.keyFromCode(this.pendingKeys.get(this.pendingKeys.size() - 1));
        }
        else
        {
            this.draftHotkeyKeys.clear();
            this.draftHotkeyKeys.addAll(this.pendingKeys);
        }
        this.pendingKeys.clear();
        this.physicalKeysDown.clear();
        this.capturing = false;
        this.physicalCapture = false;
        this.initGui();
    }

    private void saveAndExit()
    {
        if (this.hasPending()) this.commitPendingKeys();
        if (this.isVanilla())
        {
            KeybindCustomizationStore.Entry customization = this.storeEntry();
            if (this.draftVanillaKeys.size() > 1)
            {
                this.entry.mapping().setKey(InputConstants.UNKNOWN);
                customization.comboKeys = new ArrayList<>(this.draftVanillaKeys);
            }
            else
            {
                this.entry.mapping().setKey(this.draftVanillaKeys.isEmpty()
                        ? InputConstants.UNKNOWN : this.keyFromCode(this.draftVanillaKeys.get(0)));
                customization.comboKeys.clear();
            }
            KeyMapping.resetMapping();
            this.mc.options.save();
            customization.displayName = this.draftDisplayName;
            customization.hideCategory = this.draftHideCategory;
            customization.sectorColor = this.draftSectorColor;
            customization.activationContext = this.draftActivationContext;
            customization.requireKeyOrder = this.draftRequireKeyOrder && this.draftVanillaKeys.size() > 1;
            this.store().save();
        }
        else
        {
            this.entry.hotkey().getKeybind().clearKeys();
            for (int code : this.draftHotkeyKeys) this.entry.hotkey().getKeybind().addKey(code);
            this.applyHotkeyOrderSetting(this.entry.hotkey());
            InputEventHandler.getKeybindManager().updateUsedKeys();
            KeybindCustomizationStore.Entry customization = this.store().get(this.entry.hotkey().getName());
            customization.requireKeyOrder = this.draftRequireKeyOrder && this.draftHotkeyKeys.size() > 1;
            this.store().save();
            ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
        }
        this.closeToParent();
    }

    private void discardAndExit()
    {
        this.closeToParent();
    }

    private void closeToParent()
    {
        if (this.getParent() != null) this.closeGui(true);
        else GuiBase.openGui(new KeymapBrowserScreen());
    }

    private void requestExit()
    {
        if (!this.hasUnsavedChanges()) { this.closeToParent(); return; }
        MinecraftClientCompat.setScreen(this.mc, new UnsavedChangesScreen(this));
    }

    //#if MC >= 1.21.10
    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (event.key() == InputCompat.escapeKeyCode())
        {
            if (this.capturing)
            {
                this.capturing = false;
                this.physicalCapture = false;
                this.pendingKeys.clear();
                this.physicalKeysDown.clear();
                this.initGui();
            }
            else this.requestExit();
            return true;
        }
        if (this.capturing && this.physicalCapture)
        {
            this.captureKeyboardKey(event.key());
            return true;
        }
        if (this.capturing && this.isConfirmKey(event.key()))
        {
            this.commitPendingKeys();
            return true;
        }
        if (this.capturing) return true;
        if (Configs.KEYMAP_DIRECT_REBIND.getBooleanValue() && !super.keyPressed(event))
        {
            this.beginCapture();
            this.captureKeyboardKey(event.key());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        this.physicalKeysDown.remove(event.key());
        if (this.capturing && this.physicalCapture && Configs.KEYMAP_RELEASE_CONFIRM.getBooleanValue() &&
                this.physicalKeysDown.isEmpty() && this.hasPending())
        {
            this.commitPendingKeys();
            return true;
        }
        return super.keyReleased(event);
    }

    //#if MC >= 1.21.1
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (InputCompat.isPrimaryMouseButton(event.button()))
        {
            for (KeymapKeyboardLayout.Key cell : this.keyboardKeys)
            {
                if (event.x() >= cell.x() && event.x() < cell.x() + cell.width() &&
                        event.y() >= cell.y() && event.y() < cell.y() + cell.height())
                {
                    this.clickKeyboardKey(cell, event.hasControlDown());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    //#else
    //$$ @Override
    //$$ public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    //$$ {
    //$$     if (event.button() == 0)
    //$$     {
    //$$         for (KeymapKeyboardLayout.Key cell : this.keyboardKeys)
    //$$         {
    //$$             if (event.x() >= cell.x() && event.x() < cell.x() + cell.width() &&
    //$$                     event.y() >= cell.y() && event.y() < cell.y() + cell.height())
    //$$             {
    //$$                 this.clickKeyboardKey(cell, event.hasControlDown());
    //$$                 return true;
    //$$             }
    //$$         }
    //$$     }
    //$$     return super.onMouseClicked(event, doubleClick);
    //$$ }
    //#endif
    //#else
    //$$ @Override
    //$$ public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    //$$ {
    //$$     if (keyCode == 256)
    //$$     {
        //$$         if (this.capturing) { this.capturing = false; this.physicalCapture = false; this.pendingKeys.clear(); this.initGui(); }
    //$$         else this.requestExit();
    //$$         return true;
    //$$     }
    //$$     if (this.capturing && this.physicalCapture) { this.captureKeyboardKey(keyCode); return true; }
    //$$     if (this.capturing && this.isConfirmKey(keyCode)) { this.commitPendingKeys(); return true; }
    //$$     if (this.capturing) return true;
    //$$     if (Configs.KEYMAP_DIRECT_REBIND.getBooleanValue() && !super.keyPressed(keyCode, scanCode, modifiers))
    //$$     { this.beginCapture(); this.captureKeyboardKey(keyCode); return true; }
    //$$     return super.keyPressed(keyCode, scanCode, modifiers);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    //$$ {
    //$$     this.physicalKeysDown.remove(keyCode);
    //$$     if (this.capturing && this.physicalCapture && Configs.KEYMAP_RELEASE_CONFIRM.getBooleanValue() && this.physicalKeysDown.isEmpty() && this.hasPending())
    //$$     { this.commitPendingKeys(); return true; }
    //$$     return super.keyReleased(keyCode, scanCode, modifiers);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
    //$$ {
    //$$     if (button == 0)
    //$$     {
    //$$         for (KeymapKeyboardLayout.Key cell : this.keyboardKeys)
    //$$         {
    //$$             if (mouseX >= cell.x() && mouseX < cell.x() + cell.width() && mouseY >= cell.y() && mouseY < cell.y() + cell.height())
    //$$             { this.clickKeyboardKey(cell, Screen.hasControlDown()); return true; }
    //$$         }
    //$$     }
    //$$     return super.mouseClicked(mouseX, mouseY, button);
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
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.mode",
                Configs.KEYMAP_DIRECT_REBIND.getBooleanValue()
                        ? StringUtils.translate("halfmasa.gui.keybind_detail.mode_direct")
                        : StringUtils.translate("halfmasa.gui.keybind_detail.mode_click")), 10, 20, 0xFFFFC860);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.current_key") + ": " +
                this.currentKeyText(), 10, this.infoY + 5, 0xFFFFFFFF);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keymap_browser.category") + ": " +
                this.entry.modName(), 230, this.infoY + 5, 0xFF808080);
        if (Configs.KEYMAP_DIRECT_REBIND.getBooleanValue() &&
                !Configs.KEYMAP_RELEASE_CONFIRM.getBooleanValue())
        {
            this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.confirm_key") + ": " +
                    this.confirmKeyText(), 10, this.infoY + 18, 0xFFC0C0C0);
        }
        int descriptionY = this.introY;
        if (!this.isVanilla())
        {
            descriptionY += this.drawWrapped(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.malilib_note"),
                    10, descriptionY, this.getScreenWidth() - 20, 0xFFE0E0E0) * 11;
            descriptionY += 3;
        }
        if (this.capturing)
        {
            if (this.hasPending())
            {
                List<String> names = new ArrayList<>();
                for (int code : this.pendingKeys)
                {
                    names.add(code < 0
                            ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1))
                            : InputCompat.keyboardKey(code).getDisplayName().getString());
                }
                descriptionY += this.drawWrapped(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.pending",
                        String.join(" + ", names)), 10, descriptionY, this.getScreenWidth() - 20, 0xFFFFE080) * 11;
                descriptionY += 1;
                this.drawWrapped(graphics, StringUtils.translate(this.physicalCapture
                                ? "halfmasa.gui.keybind_detail.capturing_hint"
                                : "halfmasa.gui.keybind_detail.virtual_hint"),
                        10, descriptionY, this.getScreenWidth() - 20, 0xFFFFD060);
            }
            else
            {
                this.drawWrapped(graphics, StringUtils.translate(this.physicalCapture
                                ? "halfmasa.gui.keybind_detail.capturing_hint"
                                : "halfmasa.gui.keybind_detail.virtual_hint"),
                        10, descriptionY, this.getScreenWidth() - 20, 0xFFFFD060);
            }
        }
        if (this.isVanilla())
        {
            this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.wheel_group"),
                    10, this.wheelY + 5, 0xFFC0C0C0);
            this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.display_name"),
                    10, this.wheelY + 27, 0xFFE0E0E0);
        }
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.keybind_detail.keyboard"),
                10, this.keyboardY - 10, 0xFFE0E0E0);
        for (KeymapKeyboardLayout.Key cell : this.keyboardKeys)
        {
            boolean selected = this.pendingKeys.contains(cell.code()) ||
                    (!this.hasPending() && this.isCurrentCell(cell));
            int fill = selected ? 0x90605820 : 0x66202028;
            int border = selected ? 0xFFF0D080 : 0xFF606070;
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(), fill);
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + 1, border);
            graphics.fill(cell.x(), cell.y() + cell.height() - 1,
                    cell.x() + cell.width(), cell.y() + cell.height(), border);
            graphics.fill(cell.x(), cell.y(), cell.x() + 1, cell.y() + cell.height(), border);
            graphics.fill(cell.x() + cell.width() - 1, cell.y(),
                    cell.x() + cell.width(), cell.y() + cell.height(), border);
            String label = cell.mouse()
                    ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-cell.code() - 1))
                    : cell.label();
            int textX = cell.x() + Math.max(1, (cell.width() - this.mc.font.width(label)) / 2);
            this.drawString(graphics, label, textX, cell.y() + (cell.height() - 8) / 2, 0xFFE0E0E0);
        }
    }

    private String confirmKeyText()
    {
        //#if MC >= 1.21.1
        return "";
        //#else
        //$$ String text = Configs.KEYMAP_CONFIRM_SETTING.getKeybind().getKeysDisplayString();
        //$$ return text == null || text.isEmpty() ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound") : text;
        //#endif
    }

    //#if MC >= 1.21.11
    private int drawWrapped(GuiContext graphics, String text, int x, int y, int width, int color)
    //#else
    //$$ private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color)
    //#endif
    {
        String remaining = text == null ? "" : text;
        int lineY = y;
        int lines = 0;
        while (!remaining.isEmpty())
        {
            String line = this.mc.font.plainSubstrByWidth(remaining, Math.max(1, width));
            if (line.isEmpty()) break;
            this.drawString(graphics, line, x, lineY, color);
            remaining = remaining.substring(line.length()).trim();
            lineY += 11;
            lines++;
        }
        return lines;
    }

    private int descriptionLineCount(int width)
    {
        int lines = 0;
        if (!this.isVanilla())
        {
            lines += this.wrappedLineCount(StringUtils.translate("halfmasa.gui.keybind_detail.malilib_note"), width);
            lines++;
        }
        if (this.capturing)
        {
            if (this.hasPending())
            {
                List<String> names = new ArrayList<>();
                for (int code : this.pendingKeys)
                {
                    names.add(code < 0
                            ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1))
                            : InputCompat.keyboardKey(code).getDisplayName().getString());
                }
                lines += this.wrappedLineCount(StringUtils.translate("halfmasa.gui.keybind_detail.pending",
                        String.join(" + ", names)), width) + 1;
            }
            lines += this.wrappedLineCount(StringUtils.translate(this.physicalCapture
                    ? "halfmasa.gui.keybind_detail.capturing_hint"
                    : "halfmasa.gui.keybind_detail.virtual_hint"), width);
        }
        return Math.max(1, lines);
    }

    private int wrappedLineCount(String text, int width)
    {
        String remaining = text == null ? "" : text;
        int lines = 0;
        while (!remaining.isEmpty())
        {
            String line = this.mc.font.plainSubstrByWidth(remaining, Math.max(1, width));
            if (line.isEmpty()) break;
            remaining = remaining.substring(line.length()).trim();
            lines++;
        }
        return lines;
    }

    private boolean isCurrentCell(KeymapKeyboardLayout.Key cell)
    {
        if (this.isVanilla())
        {
            return this.draftVanillaKeys.contains(cell.code());
        }
        return !cell.mouse() && this.draftHotkeyKeys.contains(cell.code());
    }

    private static final class UnsavedChangesScreen extends ConfirmScreen
    {
        private final KeybindDetailScreen owner;

        private UnsavedChangesScreen(KeybindDetailScreen owner)
        {
            super(confirmed -> {
                if (confirmed) owner.saveAndExit();
                else owner.discardAndExit();
            }, Component.translatable("halfmasa.gui.keybind_detail.unsaved_title"),
                    Component.translatable("halfmasa.gui.keybind_detail.unsaved_message"),
                    Component.translatable("halfmasa.gui.keybind_detail.save_exit"),
                    Component.translatable("halfmasa.gui.keybind_detail.discard_exit"));
            this.owner = owner;
        }

        //#if MC >= 1.21.10
        @Override
        public boolean keyPressed(KeyEvent event)
        {
            if (event.key() == InputCompat.escapeKeyCode())
            {
                MinecraftClientCompat.setScreen(this.minecraft, this.owner);
                return true;
            }
            return super.keyPressed(event);
        }
        //#else
        //$$ @Override
        //$$ public boolean keyPressed(int keyCode, int scanCode, int modifiers)
        //$$ {
        //$$     if (keyCode == 256)
        //$$     {
        //$$         MinecraftClientCompat.setScreen(this.minecraft, this.owner);
        //$$         return true;
        //$$     }
        //$$     return super.keyPressed(keyCode, scanCode, modifiers);
        //$$ }
        //#endif
    }
}
