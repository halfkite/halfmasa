package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.KeyMapping;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;

public final class KeybindCustomizationScreen extends GuiBase
{
    private final KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
    private final List<Row> rows = new ArrayList<>();
    private int page;
    private int pageSize;

    public KeybindCustomizationScreen()
    {
        this.setTitle(StringUtils.translate("halfmasa.gui.keybind_editor.title"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        this.rows.clear();
        this.pageSize = Math.max(5, (this.getScreenHeight() - 82) / 24);

        List<KeyMapping> mappings = Arrays.stream(this.mc.options.keyMappings)
                .sorted(Comparator.comparing(KeyMapping::getName))
                .toList();
        int pageCount = Math.max(1, (mappings.size() + this.pageSize - 1) / this.pageSize);
        this.page = Math.max(0, Math.min(this.page, pageCount - 1));

        int start = this.page * this.pageSize;
        int end = Math.min(mappings.size(), start + this.pageSize);
        int labelWidth = Math.max(130, this.getScreenWidth() / 4);
        int nameX = labelWidth + 18;
        int nameWidth = Math.max(110, this.getScreenWidth() / 4);

        for (int index = start; index < end; index++)
        {
            KeyMapping mapping = mappings.get(index);
            KeybindCustomizationStore.Entry data = this.store.get(mapping);
            int y = 34 + (index - start) * 24;

            GuiTextFieldGeneric name = new GuiTextFieldGeneric(nameX, y, nameWidth, 20, this.mc.font);
            name.setTextWrapper(data.displayName == null ? "" : data.displayName);
            name.setMaxLengthWrapper(128);
            this.addTextField(name, field -> {
                String value = field.getTextWrapper().trim();
                data.displayName = value.isEmpty() ? null : value;
                this.store.save();
                return true;
            });

            int hideX = nameX + nameWidth + 4;
            ButtonGeneric hide = new ButtonGeneric(
                    hideX, y, 92, 20,
                    StringUtils.translate(data.hideCategory
                            ? "halfmasa.gui.keybind_editor.category_hidden"
                            : "halfmasa.gui.keybind_editor.category_shown"));
            this.addButton(hide, (button, mouseButton) -> {
                data.hideCategory = !data.hideCategory;
                this.store.save();
                this.initGui();
            });

            int contextX = hideX + 96;
            ButtonGeneric context = new ButtonGeneric(
                    contextX, y, 82, 20,
                    StringUtils.translate(data.activationContext.translationKey()));
            this.addButton(context, (button, mouseButton) -> {
                data.activationContext = data.activationContext.next();
                this.store.save();
                this.initGui();
            });

            int colorX = contextX + 86;
            GuiTextFieldGeneric color = new GuiTextFieldGeneric(colorX, y, 72, 20, this.mc.font);
            color.setTextWrapper(data.sectorColor == null ? "" : String.format("#%06X", data.sectorColor));
            color.setMaxLengthWrapper(7);
            this.addTextField(color, field -> {
                String value = field.getTextWrapper().trim();
                if (value.isEmpty())
                {
                    data.sectorColor = null;
                    this.store.save();
                    return true;
                }
                if (value.matches("#[0-9a-fA-F]{6}"))
                {
                    data.sectorColor = Integer.parseInt(value.substring(1), 16);
                    this.store.save();
                    return true;
                }
                return false;
            });

            int resetX = colorX + 76;
            this.addButton(new ButtonGeneric(resetX, y, 52, 20,
                    StringUtils.translate("halfmasa.gui.keybind_editor.reset")),
                    (button, mouseButton) -> {
                        this.store.reset(mapping);
                        this.initGui();
                    });
            this.rows.add(new Row(mapping, 10, y + 6, nameX - 16));
        }

        int bottom = this.getScreenHeight() - 28;
        this.addButton(new ButtonGeneric(10, bottom, 74, 20,
                StringUtils.translate("halfmasa.gui.keybind_editor.previous")),
                (button, mouseButton) -> {
                    if (this.page > 0)
                    {
                        this.page--;
                        this.initGui();
                    }
                });
        this.addButton(new ButtonGeneric(88, bottom, 74, 20,
                StringUtils.translate("halfmasa.gui.keybind_editor.next")),
                (button, mouseButton) -> {
                    if (this.page + 1 < pageCount)
                    {
                        this.page++;
                        this.initGui();
                    }
                });
        this.addButton(new ButtonGeneric(168, bottom, 86, 20,
                StringUtils.translate("halfmasa.gui.keybind_editor.reload")),
                (button, mouseButton) -> {
                    this.store.reload();
                    this.initGui();
                });
        this.addButton(new ButtonGeneric(262, bottom, 86, 20,
                StringUtils.translate("halfmasa.gui.keybind_editor.browse")),
                (button, mouseButton) -> GuiBase.openGui(new KeymapBrowserScreen()));
    }

    //#if MC >= 1.21.11
    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        for (Row row : this.rows)
        {
            String label = net.minecraft.network.chat.Component.translatable(row.mapping.getName()).getString();
            label = this.mc.font.plainSubstrByWidth(label, row.maxWidth);
            this.drawString(graphics, label, row.x, row.y, 0xFFFFFFFF);
        }
        this.drawString(
                graphics,
                StringUtils.translate("halfmasa.gui.keybind_editor.page", this.page + 1),
                this.getScreenWidth() - 90,
                this.getScreenHeight() - 22,
                0xFFFFFFFF);
    }

    private record Row(KeyMapping mapping, int x, int y, int maxWidth)
    {
    }
}
