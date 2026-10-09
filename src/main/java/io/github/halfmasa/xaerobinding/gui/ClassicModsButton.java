package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;

/** Promote Mod Menu's icon while retaining its original action and input modifiers. */
public final class ClassicModsButton extends Button
{
    private final Button original;
    public ClassicModsButton(Button original)
    {
        super(0, 0, 204, 20, original.getMessage(), unused -> {}, DEFAULT_NARRATION);
        this.original = original;
        this.refreshState();
    }
    public void refreshState()
    {
        this.active = original.active;
        this.visible = original.visible;
    }
    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
    @Override public void onPress(InputWithModifiers input) { original.onPress(input); }
}
//#endif
