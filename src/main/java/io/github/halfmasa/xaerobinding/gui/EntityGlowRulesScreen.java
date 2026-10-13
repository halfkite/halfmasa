package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiColorEditorHSV;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetColorIndicator;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#endif

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 1.21.10
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.BlockGlowService;
import io.github.halfmasa.xaerobinding.feature.EntityGlowRule;
import io.github.halfmasa.xaerobinding.feature.EntityGlowService;
import io.github.halfmasa.xaerobinding.feature.GlowRuleHotkeys;

/** One editable list for entity, dropped-item and through-wall block highlights. */
public final class EntityGlowRulesScreen extends GuiBase implements IKeybindConfigGui
{
    private static final int WIDE_ROW_HEIGHT = 30;
    private static final int COMPACT_ROW_HEIGHT = 52;
    private static final int TINY_ROW_HEIGHT = 76;
    private final Screen parent;
    private final List<Draft> drafts = new ArrayList<>();
    private final Set<String> savedKeys = new HashSet<>();
    private final ButtonPressDirtyListenerSimple buttonListener = new ButtonPressDirtyListenerSimple();
    private final List<Runnable> keybindChangeListeners = new ArrayList<>();
    private ConfigButtonKeybind activeKeybindButton;
    private int scroll;
    private Component error;
    private boolean needsRebuild;

    public EntityGlowRulesScreen(Screen parent)
    {
        this.setTitle(Component.translatable("halfmasa.entity_glow.rules_title").getString());
        this.setParent(parent);
        this.parent = parent;
        for (EntityGlowRule rule : EntityGlowRule.loadAll())
        {
            this.drafts.add(new Draft(rule));
            this.savedKeys.add(rule.key());
        }
    }

    private boolean compact() { return this.width < 700; }
    private boolean tiny() { return this.contentWidth() < 320; }
    private int rowHeight() { return this.tiny() ? TINY_ROW_HEIGHT
            : this.compact() ? COMPACT_ROW_HEIGHT : WIDE_ROW_HEIGHT; }
    private int contentWidth() { return Math.min(960, this.width - 20); }
    private int contentX() { return (this.width - this.contentWidth()) / 2; }
    private int visibleRows() { return Math.max(1, (this.height - 112) / this.rowHeight()); }

    @Override
    public void initGui()
    {
        this.clearOptions();
        super.initGui();
        int x = this.contentX();
        int visible = this.visibleRows();
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.drafts.size() - visible)));
        this.addButton(new ButtonGeneric(x, 30, 90, 20,
                Component.translatable("halfmasa.entity_glow.add").getString()), (button, mouseButton) -> {
                    this.drafts.add(new Draft(new EntityGlowRule(EntityGlowRule.TargetType.ENTITY,
                            EntityGlowRule.normalizeId("minecraft:pig"), EntityGlowRule.DEFAULT_COLOR)));
                    Draft draft = this.drafts.get(this.drafts.size() - 1);
                    draft.idText = "";
                    this.scroll = Math.max(0, this.drafts.size() - this.visibleRows());
                    this.needsRebuild = true;
                });

        for (int slot = 0; slot < visible && this.scroll + slot < this.drafts.size(); slot++)
        {
            this.addRow(this.drafts.get(this.scroll + slot), x, 60 + slot * this.rowHeight());
        }
        this.addButton(new ButtonGeneric(this.width / 2 - 105, this.height - 28, 100, 20,
                Component.translatable("halfmasa.entity_glow.save").getString()),
                (button, mouseButton) -> this.save());
        this.addButton(new ButtonGeneric(this.width / 2 + 5, this.height - 28, 100, 20,
                Component.translatable("halfmasa.entity_glow.cancel").getString()),
                (button, mouseButton) -> this.onClose());
    }

    private void addRow(Draft draft, int x, int y)
    {
        int contentWidth = this.contentWidth();
        int toggleWidth = 54;
        int hotkeyWidth = this.tiny() ? Math.max(60, contentWidth / 4) : this.compact() ? 104 : 118;
        int typeWidth = this.tiny() ? 64 : this.compact() ? 76 : 80;
        int pickerWidth = this.compact() ? 44 : 48;
        int hexWidth = this.tiny() ? contentWidth - 68 : this.compact() ? 108 : 112;
        int indicatorWidth = 20;
        int colorResetWidth = 42;
        int deleteWidth = 24;
        int gap = 3;

        this.addButton(new ButtonGeneric(x, y, toggleWidth, 20,
                Component.translatable(draft.enabled ? "halfmasa.entity_glow.enabled"
                        : "halfmasa.entity_glow.disabled").getString()), (button, mouseButton) -> {
                    draft.enabled = !draft.enabled;
                    button.setDisplayString(Component.translatable(draft.enabled
                            ? "halfmasa.entity_glow.enabled" : "halfmasa.entity_glow.disabled").getString());
                });
        int hotkeyX = x + toggleWidth + gap;
        ConfigButtonKeybind keybindButton = new ConfigButtonKeybind(hotkeyX, y, hotkeyWidth, 20,
                draft.hotkey.getKeybind(), this);
        this.addButton(keybindButton, this.buttonListener);
        int typeX = hotkeyX + hotkeyWidth + gap;
        this.addButton(new ButtonGeneric(typeX, y, typeWidth, 20,
                this.typeLabel(draft.type).getString()), (button, mouseButton) -> {
                    draft.type = draft.type.next();
                    draft.idText = "";
                    this.needsRebuild = true;
                });

        int fieldsY = this.compact() ? y + 23 : y;
        int idX = this.compact() ? x : typeX + typeWidth + gap;
        int remaining = this.compact() ? contentWidth : x + contentWidth - idX;
        int idWidth = this.tiny() ? contentWidth - pickerWidth - gap
                : remaining - pickerWidth - hexWidth - indicatorWidth
                - colorResetWidth - (this.compact() ? 0 : deleteWidth) - 5 * gap;
        idWidth = Math.max(70, idWidth);
        GuiTextFieldGeneric idBox = new GuiTextFieldGeneric(idX, fieldsY, idWidth, 20, this.font);
        // Register before narrowing any field: MaLiLib shares TextFieldType.STRING's max length.
        this.addTextField(idBox, field -> {
            draft.idText = fieldText(field);
            return true;
        });
        idBox.setMaxLengthWrapper(256);
        setFieldText(idBox, draft.idText);
        idBox.setCursorPosition(0);
        idBox.setHighlightPos(0);
        //#if MC >= 1.21.8
        idBox.setHoverTooltip(draft.idText);
        //#endif
        int pickerX = idX + idWidth + gap;
        this.addButton(new ButtonGeneric(pickerX, fieldsY, pickerWidth, 20,
                Component.translatable("halfmasa.entity_glow.search").getString()),
                (button, mouseButton) -> MinecraftClientCompat.setScreen(this.minecraft,
                        new EntityGlowSearchScreen(this, draft.type, id -> {
                            draft.idText = id.toString();
                            this.error = null;
                            this.needsRebuild = true;
                        })));

        int hexX = this.tiny() ? x : pickerX + pickerWidth + gap;
        int colorY = this.tiny() ? y + 46 : fieldsY;
        GuiTextFieldGeneric hexBox = new GuiTextFieldGeneric(hexX, colorY, hexWidth, 20, this.font);
        this.addTextField(hexBox, field -> {
            draft.colorText = fieldText(field);
            Integer color = EntityGlowRule.parseColor(draft.colorText);
            if (color != null) draft.color.setIntegerValue(color);
            return true;
        });
        hexBox.setMaxLengthWrapper(9);
        setFieldText(hexBox, draft.colorText);
        int indicatorX = hexX + hexWidth + gap;
        this.addWidget(new WidgetColorIndicator(indicatorX, colorY, indicatorWidth, 20, draft.color)
        {
            @Override
            //#if MC >= 1.21.10
            protected boolean onMouseClickedImpl(MouseButtonEvent event, boolean doubleClick)
            //#else
            //$$ protected boolean onMouseClickedImpl(int mouseX, int mouseY, int button)
            //#endif
            {
                EntityGlowRulesScreen.this.openColorEditor(draft);
                return true;
            }
        });
        int resetX = indicatorX + indicatorWidth + gap;
        this.addButton(new ButtonGeneric(resetX, colorY, colorResetWidth, 20,
                Component.translatable("halfmasa.entity_glow.reset_color").getString()),
                (button, mouseButton) -> {
                    draft.color.resetToDefault();
                    draft.colorText = draft.color.getStringValue();
                    this.needsRebuild = true;
                });
        int deleteX = this.compact() ? x + contentWidth - deleteWidth : resetX + colorResetWidth + gap;
        this.addButton(new ButtonGeneric(deleteX, y, deleteWidth, 20, "−"), (button, mouseButton) -> {
            this.drafts.removeIf(row -> row.key.equals(draft.key));
            this.needsRebuild = true;
        });
    }

    private Component typeLabel(EntityGlowRule.TargetType type)
    {
        return Component.translatable(type == EntityGlowRule.TargetType.ENTITY
                ? "halfmasa.entity_glow.entity" : type == EntityGlowRule.TargetType.ITEM
                ? "halfmasa.entity_glow.item" : "halfmasa.entity_glow.block");
    }

    private void openColorEditor(Draft draft)
    {
        GuiColorEditorHSV editor = new GuiColorEditorHSV(draft.color, null, this)
        {
            @Override
            public void removed()
            {
                super.removed();
                draft.colorText = draft.color.getStringValue();
                EntityGlowRulesScreen.this.needsRebuild = true;
            }
        };
        MinecraftClientCompat.setScreen(this.minecraft, editor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (mouseY >= 54 && mouseY < this.height - 38 && verticalAmount != 0)
        {
            int max = Math.max(0, this.drafts.size() - this.visibleRows());
            this.scroll = Math.max(0, Math.min(max, this.scroll + (verticalAmount > 0 ? -1 : 1)));
            this.needsRebuild = true;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    //#if MC >= 26.1
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    //#elseif MC >= 1.21.11
    //$$ protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        if (this.needsRebuild)
        {
            this.initGui();
            this.needsRebuild = false;
        }
        //#if MC >= 26.1
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        //#else
        //$$ super.drawContents(graphics, mouseX, mouseY, partialTick);
        //#endif
        //#if MC >= 26.1
        graphics.text(this.font, Component.translatable("halfmasa.entity_glow.count", this.drafts.size()).getString(),
                this.contentX() + 100, 36, 0xFFAAAAAA, false);
        //#else
        //$$ this.drawString(graphics, Component.translatable("halfmasa.entity_glow.count", this.drafts.size()).getString(),
        //$$         this.contentX() + 100, 36, 0xFFAAAAAA);
        //#endif
        if (this.drafts.isEmpty())
        {
            //#if MC >= 26.1
            graphics.centeredText(this.font, Component.translatable("halfmasa.entity_glow.empty_hint"),
                    this.width / 2, 83, 0xFFDDDDDD);
            //#else
            //$$ this.drawString(graphics, Component.translatable("halfmasa.entity_glow.empty_hint").getString(),
            //$$         this.width / 2 - 70, 83, 0xFFDDDDDD);
            //#endif
        }
        for (int slot = 0; slot < this.visibleRows() && this.scroll + slot < this.drafts.size(); slot++)
        {
            Draft draft = this.drafts.get(this.scroll + slot);
            //#if MC >= 1.21.11
            Identifier id = EntityGlowRule.normalizeId(draft.idText);
            //#else
            //$$ ResourceLocation id = EntityGlowRule.normalizeId(draft.idText);
            //#endif
            if (id != null && !this.exists(draft.type, id))
            {
                //#if MC >= 26.1
                graphics.text(this.font, Component.translatable("halfmasa.entity_glow.missing").getString(),
                        this.contentX() + 4, 81 + slot * this.rowHeight(), 0xFFFFAA55, false);
                //#else
                //$$ this.drawString(graphics, Component.translatable("halfmasa.entity_glow.missing").getString(),
                //$$         this.contentX() + 4, 81 + slot * this.rowHeight(), 0xFFFFAA55);
                //#endif
            }
        }
        if (this.error != null)
        {
            //#if MC >= 26.1
            graphics.centeredText(this.font, this.error, this.width / 2, this.height - 44, 0xFFFF5555);
            //#else
            //$$ this.drawString(graphics, this.error.getString(), this.width / 2 - 80, this.height - 44, 0xFFFF5555);
            //#endif
        }
    }

    //#if MC >= 1.21.11
    private boolean exists(EntityGlowRule.TargetType type, Identifier id)
    //#else
    //$$ private boolean exists(EntityGlowRule.TargetType type, ResourceLocation id)
    //#endif
    {
        return switch (type)
        {
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
            case ITEM -> BuiltInRegistries.ITEM.containsKey(id);
            case BLOCK -> BuiltInRegistries.BLOCK.containsKey(id);
        };
    }

    private void save()
    {
        List<String> encoded = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < this.drafts.size(); index++)
        {
            Draft draft = this.drafts.get(index);
            //#if MC >= 1.21.11
            Identifier id = EntityGlowRule.normalizeId(draft.idText);
            //#else
            //$$ ResourceLocation id = EntityGlowRule.normalizeId(draft.idText);
            //#endif
            if (id == null)
            {
                this.error = Component.translatable("halfmasa.entity_glow.invalid_id", index + 1);
                return;
            }
            Integer color = EntityGlowRule.parseColor(draft.colorText);
            if (color == null)
            {
                this.error = Component.translatable("halfmasa.entity_glow.invalid_color");
                return;
            }
            EntityGlowRule rule = new EntityGlowRule(draft.type, id, color, draft.enabled,
                    hotkeyText(draft.hotkey));
            String key = rule.key();
            if (!seen.add(key))
            {
                this.error = Component.translatable("halfmasa.entity_glow.duplicate", index + 1);
                return;
            }
            if (!this.exists(draft.type, id) && !this.savedKeys.contains(key))
            {
                this.error = Component.translatable("halfmasa.entity_glow.unknown_id", index + 1);
                return;
            }
            encoded.add(rule.encode());
        }
        Configs.ENTITY_GLOW_RULES.setStrings(encoded);
        Configs.BLOCK_GLOW_RULES.setStrings(List.of());
        EntityGlowService.reload();
        BlockGlowService.getInstance().reload();
        GlowRuleHotkeys.reload();
        new Configs().save();
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    @Override
    public void onClose()
    {
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    @Override public String getModId() { return "halfmasa"; }
    @Override public List<ConfigOptionWrapper> getConfigs() { return List.of(); }
    @Override public IConfigInfoProvider getHoverInfoProvider() { return null; }
    @Override public ButtonPressDirtyListenerSimple getButtonPressListener() { return this.buttonListener; }
    @Override public void addKeybindChangeListener(Runnable listener) { this.keybindChangeListeners.add(listener); }
    @Override public void clearOptions()
    {
        this.setActiveKeybindButton(null);
        this.keybindChangeListeners.clear();
    }
    @Override public void setActiveKeybindButton(ConfigButtonKeybind button)
    {
        if (this.activeKeybindButton != null) this.activeKeybindButton.onClearSelection();
        this.activeKeybindButton = button;
        if (button != null) button.onSelected();
        for (Runnable listener : this.keybindChangeListeners) listener.run();
    }
    @Override
    //#if MC >= 1.21.10
    public boolean onKeyTyped(KeyEvent event)
    //#else
    //$$ public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    //#endif
    {
        if (this.activeKeybindButton != null)
        {
            //#if MC >= 1.21.10
            this.activeKeybindButton.onKeyPressed(event.key());
            //#else
            //$$ this.activeKeybindButton.onKeyPressed(keyCode);
            //#endif
            return true;
        }
        //#if MC >= 1.21.10
        return super.onKeyTyped(event);
        //#else
        //$$ return super.onKeyTyped(keyCode, scanCode, modifiers);
        //#endif
    }
    @Override
    //#if MC >= 1.21.10
    public boolean onCharTyped(CharacterEvent event)
    //#else
    //$$ public boolean onCharTyped(char character, int modifiers)
    //#endif
    {
        //#if MC >= 1.21.10
        return this.activeKeybindButton != null || super.onCharTyped(event);
        //#else
        //$$ return this.activeKeybindButton != null || super.onCharTyped(character, modifiers);
        //#endif
    }
    @Override
    //#if MC >= 1.21.10
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    //#else
    //$$ public boolean onMouseClicked(int mouseX, int mouseY, int button)
    //#endif
    {
        //#if MC >= 1.21.10
        if (super.onMouseClicked(event, doubleClick)) return true;
        //#else
        //$$ if (super.onMouseClicked(mouseX, mouseY, button)) return true;
        //#endif
        if (this.activeKeybindButton != null)
        {
            this.setActiveKeybindButton(null);
            return true;
        }
        return false;
    }

    private static final class Draft
    {
        private final UUID key = UUID.randomUUID();
        private final ConfigHotkey hotkey = new ConfigHotkey("glowRuleDraft", "");
        private final ConfigColor color = new ConfigColor("glowRuleColor", "#FFFFFFFF");
        private EntityGlowRule.TargetType type;
        private String idText;
        private String colorText;
        private boolean enabled;

        private Draft(EntityGlowRule rule)
        {
            this.type = rule.type();
            this.idText = rule.id().toString();
            this.color.setIntegerValue(rule.color());
            this.colorText = this.color.getStringValue();
            setHotkeyText(this.hotkey, rule.hotkey());
            this.enabled = rule.enabled();
        }
    }

    private static String fieldText(GuiTextFieldGeneric field)
    {
        //#if MC >= 1.21.11
        return field.getValueWrapper();
        //#else
        //$$ return field.getTextWrapper();
        //#endif
    }

    private static void setFieldText(GuiTextFieldGeneric field, String value)
    {
        //#if MC >= 1.21.11
        field.setValueWrapper(value);
        //#else
        //$$ field.setTextWrapper(value);
        //#endif
    }

    private static String hotkeyText(ConfigHotkey hotkey)
    {
        //#if MC >= 1.21.11
        return hotkey.getHotkeyStringValue();
        //#else
        //$$ return hotkey.getStringValue();
        //#endif
    }

    private static void setHotkeyText(ConfigHotkey hotkey, String value)
    {
        //#if MC >= 1.21.11
        hotkey.setHotkeyStringValue(value);
        //#else
        //$$ hotkey.setValueFromString(value);
        //#endif
    }
}
//#endif
