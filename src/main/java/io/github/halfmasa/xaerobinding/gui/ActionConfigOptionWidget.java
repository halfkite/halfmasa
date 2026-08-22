package io.github.halfmasa.xaerobinding.gui;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.config.ActionHotkey;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;

public final class ActionConfigOptionWidget extends WidgetConfigOption
{
    private static final int TRIGGER_WIDTH = 60;
    private static final int EXPAND_BUTTON_WIDTH = 18;
    private static final int EXPAND_BUTTON_LEFT_OFFSET = -6;
    private static final int CHILD_INDENT = 28;

    public ActionConfigOptionWidget(
            int x,
            int y,
            int width,
            int height,
            int labelWidth,
            int configWidth,
            ConfigOptionWrapper wrapper,
            int listIndex,
            IKeybindConfigGui host,
            WidgetListConfigOptionsBase<?, ?> parent)
    {
        super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
    }

    @Override
    protected void addConfigComment(int x, int y, int width, int height, String comment)
    {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        super.addConfigComment(x, y, width, height, wrapComment(comment, Math.max(1, screenWidth - 16)));
    }

    private static String wrapComment(String comment, int maxWidth)
    {
        StringBuilder wrapped = new StringBuilder(comment.length() + 16);
        String[] paragraphs = comment.split("\\n", -1);
        for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++)
        {
            String remaining = paragraphs[paragraphIndex];
            while (!remaining.isEmpty() && Minecraft.getInstance().font.width(remaining) > maxWidth)
            {
                String fitted = Minecraft.getInstance().font.plainSubstrByWidth(remaining, maxWidth);
                int split = preferredBreak(fitted);
                if (split <= 0)
                {
                    split = remaining.offsetByCodePoints(0, 1);
                }
                wrapped.append(remaining, 0, split).append('\n');
                remaining = remaining.substring(split).stripLeading();
            }
            wrapped.append(remaining);
            if (paragraphIndex + 1 < paragraphs.length)
            {
                wrapped.append('\n');
            }
        }
        return wrapped.toString();
    }

    private static int preferredBreak(String fitted)
    {
        int minimum = fitted.length() / 2;
        for (int index = fitted.length() - 1; index >= minimum; index--)
        {
            if (Character.isWhitespace(fitted.charAt(index)))
            {
                return index + 1;
            }
        }
        return fitted.length();
    }

    @Override
    protected ButtonGeneric createResetButton(int x, int y, IConfigResettable config)
    {
        if (config == Configs.CUSTOM_SAVES_PATHS)
        {
            return new DisabledResetButton(x, y);
        }
        return super.createResetButton(x, y, config);
    }

    //#if MC >= 1.21.10
    @Override
    protected void addConfigOption(int x, int y, int labelWidth, int configWidth, IConfigBase config)
    //#else
    //$$ @Override
    //$$ protected void addConfigOption(int x, int y, float zLevel, int labelWidth, int configWidth, IConfigBase config)
    //#endif
    {
        if (Configs.isExpandedChild(config))
        {
            x += CHILD_INDENT;
            labelWidth = Math.max(20, labelWidth - CHILD_INDENT);
        }

        ConfigBoolean expansion = Configs.getExpansionConfig(config);
        int expandButtonX = x + EXPAND_BUTTON_LEFT_OFFSET;
        x += EXPAND_BUTTON_WIDTH;
        labelWidth = Math.max(20, labelWidth - EXPAND_BUTTON_WIDTH);

        if (config instanceof ConfigGroupHeader)
        {
            this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getConfigGuiDisplayName());
            this.addExpandButton(expandButtonX, y, expansion);
            return;
        }

        if (!(config instanceof ActionHotkey action))
        {
            //#if MC >= 1.21.10
            super.addConfigOption(x, y, labelWidth, configWidth, config);
            //#else
            //$$ super.addConfigOption(x, y, zLevel, labelWidth, configWidth, config);
            //#endif
            this.addExpandButton(expandButtonX, y, expansion);
            return;
        }

        y += 1;
        this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getConfigGuiDisplayName());
        this.addExpandButton(expandButtonX, y, expansion);

        IConfigInfoProvider infoProvider = this.host.getHoverInfoProvider();
        String comment = infoProvider != null ? infoProvider.getHoverInfo(config) : config.getComment();
        if (comment != null)
        {
            this.addConfigComment(x, y + 5, labelWidth, 12, comment);
        }

        x += labelWidth + 10;
        String triggerText = StringUtils.translate("halfmasa.gui.trigger");
        ButtonGeneric trigger = new ButtonGeneric(
                x,
                y,
                TRIGGER_WIDTH,
                20,
                "");
        this.addButton(trigger, (button, mouseButton) -> action.trigger());
        this.addLabel(
                x + (TRIGGER_WIDTH - this.getStringWidth(triggerText)) / 2,
                y + 7,
                TRIGGER_WIDTH,
                8,
                0xFFFFFFFF,
                triggerText);

        int hotkeyX = x + TRIGGER_WIDTH + 2;
        int hotkeyWidth = configWidth - TRIGGER_WIDTH - 2;
        this.addHotkeyConfigElements(hotkeyX, y, hotkeyWidth, config.getName(), action);
    }

    private void addExpandButton(
            int x,
            int y,
            ConfigBoolean expansion)
    {
        if (expansion == null)
        {
            return;
        }

        String label = expansion.getBooleanValue() ? "[-]" : "[+]";
        ButtonGeneric expand = new ButtonGeneric(
                x,
                y + 2,
                EXPAND_BUTTON_WIDTH,
                18,
                label,
                StringUtils.translate("halfmasa.gui.expand_settings"))
                .setRenderDefaultBackground(false);
        this.addButton(expand, (button, mouseButton) -> {
            expansion.setBooleanValue(!expansion.getBooleanValue());
            if (this.host instanceof HalfMasaConfigScreen screen)
            {
                screen.refreshExpandedConfigs();
            }
        });
    }

    private static final class DisabledResetButton extends ButtonGeneric
    {
        private DisabledResetButton(int x, int y)
        {
            super(x, y, -1, 20, StringUtils.translate("malilib.gui.button.reset.caps"));
            super.setEnabled(false);
        }

        @Override
        public void setEnabled(boolean enabled)
        {
            super.setEnabled(false);
        }
    }
}
