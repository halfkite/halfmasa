package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen.BrowserEntry;

/**
 * Detail editor for one binding: rebind the key directly, and for vanilla
 * bindings the wheel customization (display name, activation context, sector
 * color) is edited here too, merging the old wheel editor controls.
 */
public final class KeybindDetailScreen extends GuiBase
{
    private final BrowserEntry entry;
    private final List<Integer> pendingKeys = new ArrayList<>();
    private boolean capturing;
    private int infoY;
    private int wheelY;

    public KeybindDetailScreen(BrowserEntry entry)
    {
        this.entry = entry;
        this.setTitle(StringUtils.translate("halfmasa.gui.keybind_detail.title", entry.action()));
    }

    private boolean isVanilla()
    {
        return this.entry.mapping() != null;
    }

    private String currentKeyText()
    {
        if (this.isVanilla())
        {
            KeyMapping mapping = this.entry.mapping();
            return mapping.isUnbound() ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound")
                    : mapping.getTranslatedKeyMessage().getString();
        }
        String display = this.entry.hotkey().getKeybind().getKeysDisplayString();
        return display == null || display.isEmpty()
                ? StringUtils.translate("halfmasa.gui.keymap_browser.unbound") : display;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();

        int x = 10;
        int y = 34;
        this.addButton(new ButtonGeneric(x, y, 100, 20, this.capturing
                ? StringUtils.translate("halfmasa.gui.keybind_detail.capturing")
                : StringUtils.translate("halfmasa.gui.keybind_detail.change_key")),
                (button, mouseButton) -> this.capturing = !this.capturing);
        this.addButton(new ButtonGeneric(x + 104, y, 110, 20,
                StringUtils.translate("halfmasa.gui.keybind_detail.reset_key")),
                (button, mouseButton) -> {
                    this.resetKey();
                    this.capturing = false;
                    this.initGui();
                });
        y += 26;
        this.infoY = y;

        if (this.isVanilla())
        {
            y += 26;
            this.wheelY = y;

            KeybindCustomizationStore.Entry data =
                    KeybindCustomizationStore.getInstance().get(this.entry.mapping());

            int nameX = x + 60;
            GuiTextFieldGeneric name = new GuiTextFieldGeneric(
                    nameX, y + 22, Math.min(220, Math.max(120, this.getScreenWidth() / 3)), 16, this.mc.font);
            name.setTextWrapper(data.displayName == null ? "" : data.displayName);
            name.setMaxLengthWrapper(128);
            this.addTextField(name, field -> {
                String value = field.getTextWrapper().trim();
                if (!value.equals(this.entry.displayName()))
                {
                    data.displayName = value.isEmpty() ? null : value;
                    this.store().save();
                }
                return true;
            });
            int contextX = nameX + Math.min(220, Math.max(120, this.getScreenWidth() / 3)) + 8;
            this.addButton(new ButtonGeneric(contextX, y + 20, 82, 20,
                    StringUtils.translate(data.activationContext.translationKey())),
                    (button, mouseButton) -> {
                        data.activationContext = data.activationContext.next();
                        this.store().save();
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(contextX + 86, y + 20, 92, 20,
                    StringUtils.translate(data.hideCategory
                            ? "halfmasa.gui.keybind_editor.category_hidden"
                            : "halfmasa.gui.keybind_editor.category_shown")),
                    (button, mouseButton) -> {
                        data.hideCategory = !data.hideCategory;
                        this.store().save();
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(contextX + 182, y + 20, 96, 20,
                    StringUtils.translate("halfmasa.gui.keybind_detail.reset_custom")),
                    (button, mouseButton) -> {
                        this.store().reset(this.entry.mapping());
                        this.initGui();
                    });

            int colorX = contextX + 286;
            GuiTextFieldGeneric color = new GuiTextFieldGeneric(colorX, y + 20, 72, 20, this.mc.font);
            color.setTextWrapper(data.sectorColor == null ? "" : String.format("#%06X", data.sectorColor));
            color.setMaxLengthWrapper(7);
            this.addTextField(color, field -> {
                String value = field.getTextWrapper().trim();
                if (value.isEmpty())
                {
                    data.sectorColor = null;
                    this.store().save();
                    return true;
                }
                if (value.matches("#[0-9a-fA-F]{6}"))
                {
                    data.sectorColor = Integer.parseInt(value.substring(1), 16);
                    this.store().save();
                    return true;
                }
                return false;
            });
        }

        int bottom = this.getScreenHeight() - 28;
        this.addButton(new ButtonGeneric(10, bottom, 90, 20,
                StringUtils.translate("halfmasa.gui.keybind_detail.back")),
                (button, mouseButton) -> GuiBase.openGui(new KeymapBrowserScreen()));
        this.addButton(new ButtonGeneric(106, bottom, 74, 20,
                StringUtils.translate("halfmasa.gui.keymap_browser.done")),
                (button, mouseButton) -> this.onClose());
    }

    private KeybindCustomizationStore store()
    {
        return KeybindCustomizationStore.getInstance();
    }

    private void resetKey()
    {
        if (this.isVanilla())
        {
            this.entry.mapping().setKey(this.entry.mapping().getDefaultKey());
            this.mc.options.save();
        }
        else
        {
            this.entry.hotkey().resetToDefault();
            ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
        }
    }

    private void captureKeyboardKey(int keyCode)
    {
        if (keyCode == 256)
        {
            this.capturing = false;
            this.pendingKeys.clear();
            this.initGui();
            return;
        }
        if (keyCode == 259)
        {
            if (!this.pendingKeys.isEmpty())
            {
                this.pendingKeys.remove(this.pendingKeys.size() - 1);
            }
            return;
        }
        if (keyCode == 257)
        {
            this.commitPendingKeys();
            return;
        }
        Integer boxed = keyCode;
        if (this.pendingKeys.contains(boxed))
        {
            this.pendingKeys.remove(boxed);
        }
        else
        {
            this.pendingKeys.add(boxed);
        }
    }

    private void commitPendingKeys()
    {
        if (this.pendingKeys.isEmpty())
        {
            this.capturing = false;
            this.initGui();
            return;
        }
        if (this.isVanilla())
        {
            // vanilla key mappings hold a single key; the most recent press wins
            InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(
                    this.pendingKeys.get(this.pendingKeys.size() - 1));
            this.entry.mapping().setKey(key);
            this.mc.options.save();
        }
        else
        {
            this.entry.hotkey().getKeybind().clearKeys();
            for (int code : this.pendingKeys)
            {
                this.entry.hotkey().getKeybind().addKey(code);
            }
            ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
        }
        this.pendingKeys.clear();
        this.capturing = false;
        this.initGui();
    }

    //#if MC >= 1.21.10
    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (this.capturing)
        {
            this.captureKeyboardKey(event.key());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (this.capturing)
        {
            if (this.isVanilla())
            {
                this.pendingKeys.clear();
                this.pendingKeys.add(InputConstants.Type.MOUSE.getOrCreate(event.button()).getValue());
                this.commitPendingKeys();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    //#else
    //$$ @Override
    //$$ public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    //$$ {
    //$$     if (this.capturing)
    //$$     {
    //$$         this.captureKeyboardKey(keyCode);
    //$$         return true;
    //$$     }
    //$$     return super.keyPressed(keyCode, scanCode, modifiers);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
    //$$ {
    //$$     if (this.capturing)
    //$$     {
    //$$         if (this.isVanilla())
    //$$         {
    //$$             this.pendingKeys.clear();
    //$$             this.pendingKeys.add(InputConstants.Type.MOUSE.getOrCreate(button).getValue());
    //$$             this.commitPendingKeys();
    //$$         }
    //$$         return true;
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
        if (this.capturing)
        {
            this.drawString(graphics,
                    StringUtils.translate("halfmasa.gui.keybind_detail.capturing_hint"),
                    10, 20, 0xFFFFC860);
        }
        if (!this.pendingKeys.isEmpty())
        {
            List<String> names = new ArrayList<>();
            for (int code : this.pendingKeys)
            {
                names.add(code < 0 ? "M" + (-code - 1) + "?"
                        : InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString());
            }
            this.drawString(graphics,
                    StringUtils.translate("halfmasa.gui.keybind_detail.pending",
                            String.join(" + ", names)),
                    10, 30, 0xFFFFFF80);
        }
        this.drawString(graphics,
                StringUtils.translate("halfmasa.gui.keybind_detail.current_key") + ": " + this.currentKeyText(),
                10, this.infoY + 5, 0xFFFFFFFF);
        this.drawString(graphics,
                StringUtils.translate("halfmasa.gui.keymap_browser.category") + ": " + this.entry.modName(),
                230, this.infoY + 5, 0xFF808080);
        if (this.isVanilla())
        {
            this.drawString(graphics,
                    StringUtils.translate("halfmasa.gui.keybind_detail.wheel_group"),
                    10, this.wheelY + 5, 0xFFC0C0C0);
            this.drawString(graphics,
                    StringUtils.translate("halfmasa.gui.keybind_detail.display_name"),
                    10, this.wheelY + 27, 0xFFE0E0E0);
        }
        if (!this.isVanilla())
        {
            this.drawString(graphics,
                    StringUtils.translate("halfmasa.gui.keybind_detail.malilib_note"),
                    10, 26, 0xFF808080);
        }
    }
}
