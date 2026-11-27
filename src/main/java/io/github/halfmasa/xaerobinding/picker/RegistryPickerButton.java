package io.github.halfmasa.xaerobinding.picker;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.network.chat.Component;

public final class RegistryPickerButton extends ButtonGeneric
{
    public RegistryPickerButton(int x, int y, int width, int height)
    {
        super(x, y, width, height, "", Component.translatable("halfmasa.picker.open").getString());
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
        //#if MC >= 1.21.8
        super.render(graphics, mouseX, mouseY, selected);
        //#else
        //$$ super.render(mouseX, mouseY, selected, graphics);
        //#endif
        int left = this.getX() + (this.getWidth() - 9) / 2;
        int top = this.getY() + (this.getHeight() - 9) / 2;
        for (int y = 0; y < 2; y++)
        {
            for (int x = 0; x < 2; x++)
            {
                int tileX = left + x * 5;
                int tileY = top + y * 5;
                //#if MC >= 1.21.8
                RenderUtils.drawRect(graphics, tileX + 1, tileY + 1, 4, 4, 0xFF373737);
                RenderUtils.drawRect(graphics, tileX, tileY, 4, 4, 0xFFF0F0F0);
                //#else
                //$$ graphics.fill(tileX + 1, tileY + 1, tileX + 5, tileY + 5, 0xFF373737);
                //$$ graphics.fill(tileX, tileY, tileX + 4, tileY + 4, 0xFFF0F0F0);
                //#endif
            }
        }
    }
}
//#endif
