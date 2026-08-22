package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TransferableSelectionList.PackEntry.class)
//#if MC >= 1.21.10
public abstract class TransferableSelectionList_PackEntryMixin extends TransferableSelectionList.Entry implements DragItem<PackSelectionModel.Entry, TransferableSelectionList.Entry> {
    protected TransferableSelectionList_PackEntryMixin(TransferableSelectionList parent) { parent.super(); }
//#else
//$$ public abstract class TransferableSelectionList_PackEntryMixin extends ObjectSelectionList.Entry<TransferableSelectionList.PackEntry> implements DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> {
//#endif
    @Shadow
    @Final
    private TransferableSelectionList parent;

    @Shadow
    @Final
    private PackSelectionModel.Entry pack;

    @Unique
    private boolean draggable_lists$isBeingDragged = false;

    @Override
    public PackSelectionModel.Entry draggable_lists$getUnderlyingData() {
        return pack;
    }

    @Override
    //#if MC >= 1.21.10
    public TransferableSelectionList.Entry draggable_lists$getUnderlyingEntry() {
        return (TransferableSelectionList.Entry) (Object) this;
    }
    //#else
    //$$ public TransferableSelectionList.PackEntry draggable_lists$getUnderlyingEntry() {
    //$$     return (TransferableSelectionList.PackEntry) (Object) this;
    //$$ }
    //#endif

    @Override
    //#if MC >= 26.1
    public void draggable_lists$render(GuiGraphicsExtractor guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
    //#else
    //$$ public void draggable_lists$render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
    //#endif
        if (!draggable_lists$isBeingDragged) return;
        //#if MC >= 26.1
        extractContent(guiGraphics, mouseX, mouseY, hovered, tickDelta);
        //#elseif MC >= 1.21.10
        //$$ renderContent(guiGraphics, mouseX, mouseY, hovered, tickDelta);
        //#else
        //$$ render(guiGraphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        //#endif
    }

    @Override
    public void draggable_lists$setBeingDragged(boolean v) {
        draggable_lists$isBeingDragged = v;
    }
}
