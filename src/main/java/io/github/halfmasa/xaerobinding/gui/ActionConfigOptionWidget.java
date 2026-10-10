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
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.config.ActionHotkey;
import io.github.halfmasa.xaerobinding.config.ActionConfig;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;
import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;
//#if MC >= 1.21.1
import fi.dy.masa.malilib.gui.GuiBase;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
//#endif

public final class ActionConfigOptionWidget extends WidgetConfigOption
{
    private static final int TRIGGER_WIDTH = 60;
    private static final int EXPAND_BUTTON_WIDTH = 18;
    private static final int EXPAND_BUTTON_LEFT_OFFSET = -6;
    private static final int CHILD_INDENT = 28;
    private static final int RESET_BUTTON_GAP = 5;
    private static final int INLINE_RESET_WIDTH = 58;
    private static final int INLINE_CONTROL_GAP = 4;
    private static final int GROUP_BUTTON_WIDTH = 18;
    private static final int GROUP_BUTTON_GAP = 2;
    private final ActionConfigListWidget actionList;
    private final ConfigExpansionProvider expansionProvider;
    private boolean inlineNumericLayout;

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
            WidgetListConfigOptionsBase<?, ?> parent,
            ActionConfigListWidget actionList,
            ConfigExpansionProvider expansionProvider)
    {
        super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
        this.actionList = actionList;
        this.expansionProvider = expansionProvider;
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
        ButtonGeneric reset = super.createResetButton(x, y, config);
        if (this.inlineNumericLayout)
        {
            reset.setWidth(INLINE_RESET_WIDTH);
        }
        return reset;
    }

    //#if MC >= 1.21.10
    @Override
    protected void addConfigOption(int x, int y, int labelWidth, int configWidth, IConfigBase config)
    //#else
    //$$ @Override
    //$$ protected void addConfigOption(int x, int y, float zLevel, int labelWidth, int configWidth, IConfigBase config)
    //#endif
    {
        ConfigExpansionProvider provider = this.getExpansionProvider();
        if (provider.isExpandedChild(config))
        {
            x += CHILD_INDENT;
            labelWidth = Math.max(20, labelWidth - CHILD_INDENT);
        }

        ConfigBoolean expansion = provider.getExpansionConfig(config);
        int expandButtonX = x + EXPAND_BUTTON_LEFT_OFFSET;
        x += EXPAND_BUTTON_WIDTH;
        labelWidth = Math.max(20, labelWidth - EXPAND_BUTTON_WIDTH);

        //#if MC >= 1.21.1
        if (config == Configs.KEYBIND_IGNORED_KEYS)
        {
            this.addIgnoredKeysOption(x, y, labelWidth, configWidth, config);
            this.addExpandButton(expandButtonX, y, config, expansion);
            this.addCustomGroupControls(config, y + 1);
            return;
        }
        //#endif

        if (config instanceof ConfigGroupHeader)
        {
            this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getConfigGuiDisplayName());
            this.addExpandButton(expandButtonX, y, config, expansion);
            return;
        }

        if (config instanceof ActionConfig action)
        {
            this.addActionConfig(x, y, labelWidth, configWidth, action, expansion, expandButtonX);
            return;
        }

        if (!(config instanceof ActionHotkey actionHotkey))
        {
            IConfigBase inlineCompanion = provider.getInlineCompanion(config);
            if (inlineCompanion instanceof IHotkey inlineHotkey)
            {
                this.addInlineCompanion(x, y, labelWidth, configWidth, config, inlineHotkey);
                this.addExpandButton(expandButtonX, y, config, expansion);
                this.addCustomGroupControls(config, y + 1);
                return;
            }
            //#if MC >= 1.21.10
            super.addConfigOption(x, y, labelWidth, configWidth, config);
            //#else
            //$$ super.addConfigOption(x, y, zLevel, labelWidth, configWidth, config);
            //#endif
            this.addExpandButton(expandButtonX, y, config, expansion);
            this.addCustomGroupControls(config, y + 1);
            return;
        }

        y += 1;
        this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getConfigGuiDisplayName());
        this.addExpandButton(expandButtonX, y, config, expansion);

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
        this.addButton(trigger, (button, mouseButton) -> actionHotkey.trigger());
        this.addLabel(
                x + (TRIGGER_WIDTH - this.getStringWidth(triggerText)) / 2,
                y + 7,
                TRIGGER_WIDTH,
                8,
                0xFFFFFFFF,
                triggerText);

        int hotkeyX = x + TRIGGER_WIDTH + 2;
        int hotkeyWidth = configWidth - TRIGGER_WIDTH - 2;
        this.addHotkeyConfigElements(hotkeyX, y, hotkeyWidth, config.getName(), actionHotkey);
        this.addCustomGroupControls(config, y);
    }

    //#if MC >= 1.21.1
    private void addIgnoredKeysOption(int x, int y, int labelWidth, int configWidth, IConfigBase config)
    {
        this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getConfigGuiDisplayName());
        this.addConfigComment(x, y + 5, labelWidth, 12, config.getComment());
        int controlsX = x + labelWidth + 10;
        int keyboardWidth = Math.min(86, Math.max(50, configWidth / 3));
        int valueWidth = Math.max(10, configWidth - keyboardWidth - 4);
        Runnable open = () -> GuiBase.openGui(new IgnoredKeysScreen(MinecraftClientCompat.getScreen(this.mc)));
        this.addButton(new IgnoredKeysValueButton(controlsX, y + 1, valueWidth), (button, mouseButton) -> open.run());
        this.addButton(new ButtonGeneric(controlsX + valueWidth + 4, y + 1, keyboardWidth, 20,
                StringUtils.translate("halfmasa.gui.ignored_keys.open")), (button, mouseButton) -> open.run());
        this.addButton(this.createResetButton(controlsX + configWidth + RESET_BUTTON_GAP, y + 1, Configs.KEYBIND_IGNORED_KEYS),
                (button, mouseButton) -> {
                    Configs.KEYBIND_IGNORED_KEYS.resetToDefault();
                    new Configs().save();
                    if (MinecraftClientCompat.getScreen(this.mc) instanceof GuiBase gui) gui.initGui();
                });
    }

    private static final class IgnoredKeysValueButton extends ButtonGeneric
    {
        private IgnoredKeysValueButton(int x, int y, int width)
        {
            super(x, y, width, 20, "");
        }

        @Override
        //#if MC >= 1.21.11
        public void render(GuiContext graphics, int mouseX, int mouseY, boolean selected)
        //#elseif MC >= 1.21.8
        //$$ public void render(GuiGraphics graphics, int mouseX, int mouseY, boolean selected)
        //#else
        //$$ public void render(int mouseX, int mouseY, boolean selected, GuiGraphics graphics)
        //#endif
        {
            String names = KeymapInputNames.names(Configs.KEYBIND_IGNORED_KEYS.getStringValue());
            this.setDisplayString(Minecraft.getInstance().font.plainSubstrByWidth(names, Math.max(1, this.width - 8)));
            this.setHoverStrings(names, StringUtils.translate("halfmasa.gui.ignored_keys.open"));
            //#if MC >= 1.21.8
            super.render(graphics, mouseX, mouseY, selected);
            //#else
            //$$ super.render(mouseX, mouseY, selected, graphics);
            //#endif
        }
    }
    //#endif

    private void addInlineCompanion(
            int x, int y, int labelWidth, int configWidth, IConfigBase config, IHotkey companion)
    {
        int hotkeyWidth = Math.min(230, Math.max(120, configWidth / 3));
        int resetAreaWidth = INLINE_RESET_WIDTH + 2 + INLINE_CONTROL_GAP;
        int valueWidth = Math.max(80, configWidth - hotkeyWidth - resetAreaWidth);
        this.inlineNumericLayout = true;
        //#if MC >= 1.21.10
        try
        {
            super.addConfigOption(x, y, labelWidth, valueWidth, config);
        }
        finally
        {
            this.inlineNumericLayout = false;
        }
        //#else
        //$$ try
        //$$ {
        //$$     super.addConfigOption(x, y, zLevel, labelWidth, valueWidth, config);
        //$$ }
        //$$ finally
        //$$ {
        //$$     this.inlineNumericLayout = false;
        //$$ }
        //#endif

        // MaLiLib offsets the value controls past the label inside addConfigOption.
        int controlsX = x + labelWidth + 10;
        int hotkeyX = controlsX + valueWidth + 2 + INLINE_RESET_WIDTH + INLINE_CONTROL_GAP;
        int remainingWidth = Math.max(44, configWidth - valueWidth - resetAreaWidth);
        this.addHotkeyConfigElements(hotkeyX, y + 1, remainingWidth,
                companionName(companion), companion);
    }

    private static String companionName(IHotkey companion)
    {
        return companion instanceof IConfigBase config ? config.getName() : "flyPreset";
    }

    private void addActionConfig(
            int x,
            int y,
            int labelWidth,
            int configWidth,
            ActionConfig action,
            ConfigBoolean expansion,
            int expandButtonX)
    {
        y += 1;
        this.addLabel(x, y + 7, labelWidth, 8, 0xFFFFFFFF, action.getConfigGuiDisplayName());
        this.addExpandButton(expandButtonX, y, action, expansion);

        IConfigInfoProvider infoProvider = this.host.getHoverInfoProvider();
        String comment = infoProvider != null ? infoProvider.getHoverInfo(action) : action.getComment();
        if (comment != null)
        {
            this.addConfigComment(x, y + 5, labelWidth, 12, comment);
        }

        var buttons = action.getButtons();
        int buttonX = x + labelWidth + 10;
        int buttonAreaX = buttonX;
        int gap = 2;
        int buttonWidth = buttons.size() == 1
                ? configWidth
                : Math.max(40, (configWidth - gap * (buttons.size() - 1)) / buttons.size());
        for (ActionConfig.ActionButton actionButton : buttons)
        {
            ButtonGeneric button = new ButtonGeneric(
                    buttonX,
                    y,
                    buttonWidth,
                    20,
                    StringUtils.translate(actionButton.translationKey()));
            this.addButton(button, (pressed, mouseButton) -> actionButton.trigger());
            buttonX += buttonWidth + gap;
        }

        DisabledResetButton reset = new DisabledResetButton(
                buttonAreaX + configWidth + RESET_BUTTON_GAP,
                y);
        this.addButton(reset, (button, mouseButton) -> {});
        this.addCustomGroupControls(action, y);
    }

    private void addCustomGroupControls(IConfigBase config, int y)
    {
        if (!Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue() || !isCustomGroupCandidate(config))
        {
            return;
        }

        String source = this.getExpansionProvider().getConfigSource();
        int right = this.getX() + this.getWidth() - 4;
        int downX = right - GROUP_BUTTON_WIDTH;
        int upX = downX - GROUP_BUTTON_GAP - GROUP_BUTTON_WIDTH;
        int minusX = upX - GROUP_BUTTON_GAP - GROUP_BUTTON_WIDTH;
        int plusX = minusX - GROUP_BUTTON_GAP - GROUP_BUTTON_WIDTH;
        this.addGroupButton(plusX, y, "+", "halfmasa.gui.custom_groups.add_to_group",
                () -> CustomConfigGroupScreen.openGroupPicker(this.mc, this.host instanceof net.minecraft.client.gui.screens.Screen
                        ? (net.minecraft.client.gui.screens.Screen) this.host : null, source, config.getName()));
        this.addGroupButton(minusX, y, "-", "halfmasa.gui.custom_groups.remove_from_group",
                () -> {
                    CustomConfigGroupStore.getInstance().removeConfig(source, config.getName());
                    this.actionList.refreshExpandedConfigs();
                });
        this.addGroupButton(upX, y, "\u2191", "halfmasa.gui.custom_groups.move_up",
                () -> {
                    CustomConfigGroupStore.getInstance().moveConfig(source, config.getName(), -1);
                    this.actionList.refreshExpandedConfigs();
                });
        this.addGroupButton(downX, y, "\u2193", "halfmasa.gui.custom_groups.move_down",
                () -> {
                    CustomConfigGroupStore.getInstance().moveConfig(source, config.getName(), 1);
                    this.actionList.refreshExpandedConfigs();
                });
    }

    private void addGroupButton(int x, int y, String label, String tooltipKey, Runnable action)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, GROUP_BUTTON_WIDTH, 20, label,
                StringUtils.translate(tooltipKey));
        this.addButton(button, (clicked, mouseButton) -> action.run());
    }

    private static boolean isCustomGroupCandidate(IConfigBase config)
    {
        return !(config instanceof ConfigGroupHeader) && config != null &&
                !config.getName().endsWith("Expanded") && Configs.getExpansionParent(config) == null;
    }

    private void addExpandButton(
            int x,
            int y,
            IConfigBase config,
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
            this.expansionProvider.onExpansionChanged(config, expansion.getBooleanValue());
            this.actionList.refreshExpandedConfigs();
        });
    }

    private ConfigExpansionProvider getExpansionProvider()
    {
        if (this.expansionProvider != null)
        {
            return this.expansionProvider;
        }
        if (this.parent instanceof ActionConfigListWidget list)
        {
            return list.getExpansionProvider();
        }
        return HalfMasaConfigExpansionProvider.INSTANCE;
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
