package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import net.minecraft.client.gui.components.Button;
//#if MC >= 26.0
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.InputWithModifiers;
//#endif

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
    //#if MC >= 26.0
    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
    //#elseif MC >= 1.21.11
    //$$ @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //$$     this.renderDefaultSprite(graphics);
    //$$     this.renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    //$$ }
    //#endif
    //#if MC >= 1.21.10
    @Override public void onPress(InputWithModifiers input) { original.onPress(input); }
    //#else
    //$$ @Override public void onPress() { original.onPress(); }
    //#endif
}
//#endif
