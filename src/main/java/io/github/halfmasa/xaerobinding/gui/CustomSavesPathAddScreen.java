package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

public final class CustomSavesPathAddScreen extends Screen
{
    private final CustomSavesPathScreen parent;
    private final CustomSavesPath.PathOption editing;
    private EditBox pathBox;
    private Component error;

    public CustomSavesPathAddScreen(CustomSavesPathScreen parent)
    {
        this(parent, null);
    }

    public CustomSavesPathAddScreen(CustomSavesPathScreen parent, CustomSavesPath.PathOption editing)
    {
        super(Component.translatable(editing == null ?
                "halfmasa.custom_saves.add_title" : "halfmasa.custom_saves.edit_title"));
        this.parent = parent;
        this.editing = editing;
    }

    @Override
    protected void init()
    {
        this.pathBox = this.addRenderableWidget(new EditBox(
                this.font,
                this.width / 2 - 150,
                54,
                300,
                20,
                Component.translatable("halfmasa.custom_saves.path_hint")));
        this.pathBox.setMaxLength(1024);
        if (this.editing != null)
        {
            this.pathBox.setValue(this.editing.configuredValue());
        }
        this.addRenderableWidget(Button.builder(Component.translatable(this.editing == null ?
                        "halfmasa.custom_saves.add" : "halfmasa.custom_saves.save"), button -> this.savePath())
                .bounds(this.width / 2 - 156, this.height - 36, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(this.width / 2 + 56, this.height - 36, 100, 20)
                .build());
    }

    @Override
    protected void setInitialFocus()
    {
        this.setInitialFocus(this.pathBox);
    }

    @Override
    public void onClose()
    {
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    //#if MC >= 26.1
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.centeredText(this.font,
    //#else
    //$$ @Override
    //$$ public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //$$ {
    //$$     super.render(graphics, mouseX, mouseY, partialTick);
    //$$     graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    //$$     graphics.drawCenteredString(this.font,
    //#endif
                Component.translatable("halfmasa.custom_saves.path_description"), this.width / 2, 38, 0xAAAAAA);
        if (this.error != null)
        {
            //#if MC >= 26.1
            graphics.centeredText(this.font, this.error, this.width / 2, 88, 0xFF5555);
            //#else
            //$$ graphics.drawCenteredString(this.font, this.error, this.width / 2, 88, 0xFF5555);
            //#endif
        }
    }

    private void savePath()
    {
        boolean saved = this.editing == null
                ? CustomSavesPath.addConfiguredPath(this.pathBox.getValue())
                : CustomSavesPath.editConfiguredPath(this.editing, this.pathBox.getValue());
        if (!saved)
        {
            this.error = Component.translatable(this.editing == null ?
                    "halfmasa.custom_saves.add_failed" : "halfmasa.custom_saves.edit_failed");
            return;
        }

        this.parent.reloadPathOptions();
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }
}
