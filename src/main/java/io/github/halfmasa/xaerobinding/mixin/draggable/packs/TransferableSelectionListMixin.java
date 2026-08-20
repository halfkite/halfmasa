package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DragList;
import io.github.halfmasa.xaerobinding.draggable.DragManager;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
import io.github.halfmasa.xaerobinding.draggable.duck.AbstractPackDuckProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TransferableSelectionList.class)
@Environment(EnvType.CLIENT)
public abstract class TransferableSelectionListMixin extends ObjectSelectionList<TransferableSelectionList.PackEntry> implements DragList<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> {
    @Unique
    private final DragManager<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> draggable_lists$dragManager = new DragManager<>(this);

    public TransferableSelectionListMixin(Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l);
    }

    @Override
    protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        draggable_lists$dragManager.renderListItems(guiGraphics, mouseX, mouseY, tickDelta);
    }

    @Unique
    private boolean draggable_lists$isMouseOverScrollbar(double mouseX) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggable_lists$dragManager.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if ((io.github.halfmasa.xaerobinding.config.Configs.DRAGGABLE_LISTS.getBooleanValue() && (((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.ENABLED || ((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.REQUIRES_MODIFIER && net.minecraft.client.gui.screens.Screen.hasShiftDown())) && !draggable_lists$isMouseOverScrollbar(mouseX) && draggable_lists$dragManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (draggable_lists$dragManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);
        draggable_lists$dragManager.renderWidget(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> draggable_lists$getEntryAtPosition(double mouseX, double mouseY) {
        TransferableSelectionList.PackEntry entryAtPosition = getEntryAtPosition(mouseX, mouseY);
        return (DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry>) entryAtPosition;
    }

    @Override
    public int draggable_lists$getIndexOfEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> selectedItem) {
        return children().indexOf(selectedItem.draggable_lists$getUnderlyingEntry());
    }

    @Override
    public void draggable_lists$setDragging(boolean b) {
        super.setDragging(b);
    }

    @Override
    public int draggable_lists$getHeaderHeight() {
        return headerHeight;
    }

    @Override
    public int draggable_lists$getY() {
        return getY();
    }

    @Override
    public int draggable_lists$getBottom() {
        return getBottom();
    }

    @Override
    public int draggable_lists$getItemHeight() {
        return itemHeight;
    }

    @Override
    public int draggable_lists$getRowTop(int i) {
        return getRowTop(i);
    }

    @Override
    public int draggable_lists$getRowBottom(int i) {
        return getRowBottom(i);
    }

    @Override
    public double draggable_lists$getRowLeft() {
        return getRowLeft();
    }

    @Override
    public int draggable_lists$getRowWidth() {
        return getRowWidth();
    }

    @Override
    public double draggable_lists$getScrollAmount() {
        return getScrollAmount();
    }

    @Override
    public void draggable_lists$setScrollAmount(double v) {
        setScrollAmount(v);
    }

    @Override
    public void draggable_lists$moveEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> item, int n) {
        if (item.draggable_lists$getUnderlyingData() instanceof AbstractPackDuckProvider duckProvider) {
            duckProvider.draggable_lists$moveTo(n);
        }
    }

    @Override
    public int draggable_lists$getItemCount() {
        return getItemCount();
    }

    @Override
    public void draggable_lists$renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta, int i, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        renderItem(guiGraphics, mouseX, mouseY, tickDelta, i, rowLeft, rowTop, rowWidth, rowHeight);
    }
}
