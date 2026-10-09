package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.KeymapLayout;
import io.github.halfmasa.xaerobinding.feature.IgnoredKeySelection;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class IgnoredKeysScreen extends GuiBase
{
    private static final int KEYBOARD_TOP = 56;
    private final IgnoredKeySelection draft = new IgnoredKeySelection(
            Configs.KEYBIND_IGNORED_KEYS.getStringValue(), Configs.KEYBIND_IGNORED_KEYS.getDefaultStringValue());
    private final boolean inverted = Configs.KEYBIND_INVERT_IGNORED_KEYS.getBooleanValue();
    private final List<KeymapKeyboardLayout.Key> cells = new ArrayList<>();
    private List<KeymapKeyboardStyle.Binding> bindings = List.of();
    private boolean layout122 = ((KeymapLayout) Configs.KEYMAP_KEYBOARD_LAYOUT.getOptionListValue()).isExtended();

    public IgnoredKeysScreen(Screen parent)
    {
        this.setParent(parent);
        this.setTitle(StringUtils.translate(this.inverted
                ? "halfmasa.gui.ignored_keys.allow_title" : "halfmasa.gui.ignored_keys.title"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        this.bindings = KeymapKeyboardBindings.current(this.mc);
        this.cells.clear();
        this.cells.addAll((this.layout122 ? KeymapKeyboardLayout.keys122(10, KEYBOARD_TOP, this.getScreenWidth() - 20)
                : KeymapKeyboardLayout.keys(10, KEYBOARD_TOP, this.getScreenWidth() - 20)).stream()
                .filter(key -> key.codes().size() == 1).toList());
        this.addButton(new KeymapBrowserScreen.BrowserButton(this.getScreenWidth() - 110, 27, 100, 20,
                StringUtils.translate(this.layout122 ? "halfmasa.gui.keymap_browser.layout_122"
                        : "halfmasa.gui.keymap_browser.layout_104")), (button, mouseButton) -> {
                    this.layout122 = !this.layout122;
                    this.initGui();
                });
        int bottom = this.getScreenHeight() - 28;
        this.addButton(new KeymapBrowserScreen.BrowserButton(10, bottom, 64, 20, StringUtils.translate("halfmasa.gui.keymap_browser.reset")),
                (button, mouseButton) -> this.draft.reset());
        this.addButton(new KeymapBrowserScreen.BrowserButton(80, bottom, 108, 20, StringUtils.translate("halfmasa.gui.keymap_browser.save_exit")),
                (button, mouseButton) -> this.saveAndExit());
        this.addButton(new KeymapBrowserScreen.BrowserButton(194, bottom, 116, 20, StringUtils.translate("halfmasa.gui.keymap_browser.discard_exit")),
                (button, mouseButton) -> this.onClose());
    }

    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    {
        String hint = StringUtils.translate(this.inverted ? "halfmasa.gui.ignored_keys.allow_hint" : "halfmasa.gui.ignored_keys.hint");
        hint = this.mc.font.plainSubstrByWidth(hint, Math.max(1, this.getScreenWidth() - 130));
        graphics.drawString(this.mc.font, hint, 10, 33, 0xFFD8D8E0, false);
        var ignored = this.draft.keys();
        for (var cell : this.cells)
        {
            boolean excluded = IgnoredKeySelection.isIgnored(cell.code(), ignored, this.inverted);
            var indicators = KeymapKeyboardStyle.indicators(this.bindings, cell.codes(), excluded);
            KeymapKeyboardRenderer.drawKey(graphics, this.mc.font, cell, indicators, false, cell.contains(mouseX, mouseY));
        }
        int y = KEYBOARD_TOP + (this.layout122 ? KeymapKeyboardLayout.HEIGHT_122 : KeymapKeyboardLayout.HEIGHT) + 6;
        String legend = StringUtils.translate("halfmasa.gui.ignored_keys.legend");
        y = this.drawWrapped(graphics, legend, y, this.getScreenHeight() - 50);
        String names = StringUtils.translate(this.inverted ? "halfmasa.gui.ignored_keys.allowed" : "halfmasa.gui.ignored_keys.selected")
                + KeymapInputNames.names(ignored);
        this.drawWrapped(graphics, names, y + 6, this.getScreenHeight() - 34);
    }

    private int drawWrapped(GuiContext graphics, String text, int y, int bottom)
    {
        int width = this.getScreenWidth() - 20;
        while (!text.isEmpty() && y + 9 <= bottom)
        {
            String part = this.mc.font.plainSubstrByWidth(text, width);
            if (part.isEmpty()) break;
            text = text.substring(part.length());
            if (!text.isEmpty() && y + 22 >= bottom) part = this.mc.font.plainSubstrByWidth(part, width - 12) + "…";
            graphics.drawString(this.mc.font, part, 10, y, 0xFFE0E0E8, false);
            y += 12;
        }
        return y;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (InputCompat.isPrimaryMouseButton(event.button()))
        {
            for (var cell : this.cells)
            {
                if (cell.contains(event.x(), event.y()))
                {
                    this.draft.toggle(cell.code());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void saveAndExit()
    {
        if (this.hasChanges())
        {
            Configs.KEYBIND_IGNORED_KEYS.setValueFromString(this.draft.serialize());
            Configs.KEYMAP_KEYBOARD_LAYOUT.setOptionListValue(KeymapLayout.of(this.layout122));
            new Configs().save();
        }
        this.closeGui(true);
    }

    @Override
    public void onClose()
    {
        if (this.hasChanges()) GuiBase.openGui(new DiscardScreen(this));
        else this.closeGui(true);
    }

    private boolean hasChanges()
    {
        return this.draft.hasChanges()
                || KeymapLayout.of(this.layout122) != Configs.KEYMAP_KEYBOARD_LAYOUT.getOptionListValue();
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (event.key() == InputCompat.escapeKeyCode())
        {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    public static boolean isEditingKeys(Screen screen)
    {
        return screen instanceof IgnoredKeysScreen || screen instanceof DiscardScreen;
    }

    private static final class DiscardScreen extends ConfirmScreen
    {
        private final IgnoredKeysScreen owner;

        private DiscardScreen(IgnoredKeysScreen owner)
        {
            super(discard -> {
                if (discard) owner.closeGui(true);
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
}
//#endif
