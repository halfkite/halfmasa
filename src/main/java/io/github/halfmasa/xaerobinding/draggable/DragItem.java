package io.github.halfmasa.xaerobinding.draggable;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;

public interface DragItem<T, E extends ObjectSelectionList.Entry<?>> {
    T draggable_lists$getUnderlyingData();

    E draggable_lists$getUnderlyingEntry();

    //#if MC >= 26.1
    void draggable_lists$render(GuiGraphicsExtractor guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
    //#else
    //$$ void draggable_lists$render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
    //#endif

    void draggable_lists$setBeingDragged(boolean v);
}
