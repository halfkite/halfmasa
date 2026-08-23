package io.github.halfmasa.xaerobinding.gui;

import java.nio.file.Path;

import net.minecraft.ChatFormatting;
//#if MC >= 1.21.11
import net.minecraft.util.Util;
//#else
//$$ import net.minecraft.Util;
//#endif
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

public final class CustomSavesPathScreen extends Screen
{
    private final Screen parent;
    private PathList list;
    private Button editButton;
    private Button deleteButton;
    private Component error;

    public CustomSavesPathScreen(Screen parent)
    {
        super(Component.translatable("halfmasa.custom_saves.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        this.list = this.addRenderableWidget(new PathList(this, this.minecraft, this.width, this.height - 96, 42, 32));
        this.addRenderableWidget(Button.builder(Component.translatable("halfmasa.custom_saves.add"), button ->
                        MinecraftClientCompat.setScreen(this.minecraft, new CustomSavesPathAddScreen(this)))
                .bounds(this.width / 2 - 158, this.height - 36, 76, 20)
                .build());
        this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("halfmasa.custom_saves.edit"), button ->
                        this.editSelected())
                .bounds(this.width / 2 - 78, this.height - 36, 76, 20)
                .build());
        this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("halfmasa.custom_saves.delete"), button ->
                        this.deleteSelected())
                .bounds(this.width / 2 + 2, this.height - 36, 76, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(this.width / 2 + 82, this.height - 36, 76, 20)
                .build());
        this.updateSelectionButtons();
    }

    @Override
    protected void setInitialFocus()
    {
        this.setInitialFocus(this.list);
    }

    @Override
    public void onClose()
    {
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    public void reloadPathOptions()
    {
        this.rebuildWidgets();
    }

    //#if MC >= 26.1
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
    //#else
    //$$ @Override
    //$$ public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //$$ {
    //$$     super.render(graphics, mouseX, mouseY, partialTick);
    //$$     graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
    //#endif
        if (this.error != null)
        {
            //#if MC >= 26.1
            graphics.centeredText(this.font, this.error, this.width / 2, this.height - 54, 0xFF5555);
            //#else
            //$$ graphics.drawCenteredString(this.font, this.error, this.width / 2, this.height - 54, 0xFF5555);
            //#endif
        }
    }

    private void select(Path path)
    {
        if (!CustomSavesPath.switchTo(path))
        {
            this.error = Component.translatable("halfmasa.custom_saves.switch_failed").withStyle(ChatFormatting.RED);
            return;
        }

        CustomSavesPath.refreshWorldList((net.minecraft.client.gui.screens.worldselection.SelectWorldScreen) this.parent);
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    private void updateSelectionButtons()
    {
        CustomSavesPath.PathOption option = this.list.getSelectedOption();
        boolean editable = option != null && !option.defaultPath();
        if (this.editButton != null)
        {
            this.editButton.active = editable;
        }
        if (this.deleteButton != null)
        {
            this.deleteButton.active = editable;
        }
    }

    private void editSelected()
    {
        CustomSavesPath.PathOption option = this.list.getSelectedOption();
        if (option != null && !option.defaultPath())
        {
            MinecraftClientCompat.setScreen(this.minecraft, new CustomSavesPathAddScreen(this, option));
        }
    }

    private void deleteSelected()
    {
        CustomSavesPath.PathOption option = this.list.getSelectedOption();
        if (option == null || option.defaultPath())
        {
            return;
        }

        String pathText = option.invalid() ? option.configuredValue() : option.path().toString();
        MinecraftClientCompat.setScreen(this.minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed)
            {
                if (CustomSavesPath.removeConfiguredPath(option))
                {
                    this.error = null;
                    this.reloadPathOptions();
                }
                else
                {
                    this.error = Component.translatable("halfmasa.custom_saves.delete_failed");
                }
            }
            MinecraftClientCompat.setScreen(this.minecraft, this);
        }, Component.translatable("halfmasa.custom_saves.delete_title"),
                Component.translatable("halfmasa.custom_saves.delete_confirm", pathText)));
    }

    private static final class PathList extends ObjectSelectionList<PathEntry>
    {
        private final CustomSavesPathScreen screen;

        private PathList(CustomSavesPathScreen screen, Minecraft minecraft, int width, int height, int top, int itemHeight)
        {
            super(minecraft, width, height, top, itemHeight);
            this.screen = screen;
            Path current = CustomSavesPath.getCurrentPath();
            PathEntry initialSelection = null;
            for (CustomSavesPath.PathOption option : CustomSavesPath.getOptions())
            {
                PathEntry entry = new PathEntry(this.screen, option, current.equals(option.path()));
                this.addEntry(entry);
                if (entry.current)
                {
                    initialSelection = entry;
                }
            }
            super.setSelected(initialSelection);
        }

        @Override
        public int getRowWidth()
        {
            return Math.min(420, Math.max(220, this.width - 36));
        }

        @Nullable
        private CustomSavesPath.PathOption getSelectedOption()
        {
            PathEntry selected = this.getSelected();
            return selected == null ? null : selected.option;
        }

        @Override
        public void setSelected(@Nullable PathEntry entry)
        {
            super.setSelected(entry);
            this.screen.updateSelectionButtons();
        }
    }

    private static final class PathEntry extends ObjectSelectionList.Entry<PathEntry>
    {
        private final CustomSavesPathScreen screen;
        private final CustomSavesPath.PathOption option;
        private final boolean current;
        private long lastClickTime;

        private PathEntry(CustomSavesPathScreen screen, CustomSavesPath.PathOption option, boolean current)
        {
            this.screen = screen;
            this.option = option;
            this.current = current;
        }

        @Override
        public Component getNarration()
        {
            return this.getLabel();
        }

        //#if MC >= 1.21.10
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClickHint)
        {
            return this.halfmasa$mouseClicked(event.x(), event.y(), event.button());
        }
        //#else
        //$$ @Override
        //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
        //$$ {
        //$$     return this.halfmasa$mouseClicked(mouseX, mouseY, button);
        //$$ }
        //#endif

        private boolean halfmasa$mouseClicked(double mouseX, double mouseY, int button)
        {
            if (button != 0)
            {
                return false;
            }

            long clickTime = Util.getMillis();
            boolean doubleClick = this.screen.list.getSelected() == this && clickTime - this.lastClickTime < 250L;
            this.screen.list.setSelected(this);
            this.lastClickTime = clickTime;
            if (doubleClick && this.option.available() && !this.option.invalid())
            {
                this.screen.select(this.option.path());
            }
            return true;
        }

        //#if MC >= 26.1
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            this.halfmasa$render(graphics, this.getContentX(), this.getContentY(), this.getContentWidth());
        }
        //#elseif MC >= 1.21.10
        //$$ @Override
        //$$ public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        //$$ {
        //$$     this.halfmasa$render(graphics, this.getContentX(), this.getContentY(), this.getContentWidth());
        //$$ }
        //#else
        //$$ @Override
        //$$ public void render(GuiGraphics graphics, int index, int y, int x, int rowWidth, int rowHeight,
        //$$                    int mouseX, int mouseY, boolean hovered, float partialTick)
        //$$ {
        //$$     this.halfmasa$render(graphics, x, y, rowWidth);
        //$$ }
        //#endif

        //#if MC >= 26.1
        private void halfmasa$render(GuiGraphicsExtractor graphics, int x, int y, int rowWidth)
        //#else
        //$$ private void halfmasa$render(GuiGraphics graphics, int x, int y, int rowWidth)
        //#endif
        {
            String label = this.getLabel().getString();
            String visible = this.screen.font.plainSubstrByWidth(label, rowWidth - 16);
            int color = this.option.available() ? 0xFFFFFF : 0xFF5555;
            //#if MC >= 26.1
            graphics.text(this.screen.font, visible, x + 8, y + 2, color, false);
            //#else
            //$$ graphics.drawString(this.screen.font, visible, x + 8, y + 2, color, false);
            //#endif
            if (this.option.invalid())
            {
                //#if MC >= 26.1
                graphics.text(this.screen.font,
                //#else
                //$$ graphics.drawString(this.screen.font,
                //#endif
                        Component.translatable("halfmasa.custom_saves.invalid").getString(), x + 8, y + 15, 0xFF5555, false);
            }
            else if (this.current)
            {
                //#if MC >= 26.1
                graphics.text(this.screen.font,
                //#else
                //$$ graphics.drawString(this.screen.font,
                //#endif
                        Component.translatable("halfmasa.custom_saves.current").getString(), x + 8, y + 15, 0x55FF55, false);
            }
        }

        private Component getLabel()
        {
            if (this.option.defaultPath())
            {
                return Component.translatable("halfmasa.custom_saves.default", this.option.path());
            }
            return Component.literal(this.option.invalid() ? this.option.configuredValue() : this.option.path().toString());
        }
    }
}
