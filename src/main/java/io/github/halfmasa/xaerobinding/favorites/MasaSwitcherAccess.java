//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

public interface MasaSwitcherAccess
{
    void halfmasa$rebuildSwitcher();
    //#if MC >= 1.21.11
    void halfmasa$renderSwitcherOverlay(fi.dy.masa.malilib.render.GuiContext context, int mouseX, int mouseY);
    //#else
    //$$ void halfmasa$renderSwitcherOverlay(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY);
    //#endif
}
//#endif
