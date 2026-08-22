package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DragList;
import io.github.halfmasa.xaerobinding.draggable.DragManager;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
import io.github.halfmasa.xaerobinding.draggable.duck.AbstractPackDuckProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TransferableSelectionList.class)
@Environment(EnvType.CLIENT)
//#if MC >= 1.21.10
public abstract class TransferableSelectionListMixin extends ObjectSelectionList<TransferableSelectionList.Entry> implements DragList<PackSelectionModel.Entry, TransferableSelectionList.Entry> {
    @Unique
    private final DragManager<PackSelectionModel.Entry, TransferableSelectionList.Entry> draggable_lists$dragManager = new DragManager<>(this);
//#else
//$$ public abstract class TransferableSelectionListMixin extends ObjectSelectionList<TransferableSelectionList.PackEntry> implements DragList<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> {
//$$ @Unique
//$$ private final DragManager<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> draggable_lists$dragManager = new DragManager<>(this);
//#endif

    public TransferableSelectionListMixin(Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l);
    }

    //#if MC >= 26.1
    @Override
    protected void extractListItems(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
    //#else
    //$$ @Override
    //$$ protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
    //#endif
        draggable_lists$dragManager.renderListItems(guiGraphics, mouseX, mouseY, tickDelta);
    }

    @Unique
    private boolean draggable_lists$isMouseOverScrollbar(double mouseX) {
        return false;
    }

    //#if MC >= 1.21.10
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggable_lists$dragManager.mouseReleased(event.x(), event.y(), event.button())) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if ((io.github.halfmasa.xaerobinding.config.Configs.DRAGGABLE_LISTS.getBooleanValue() && (((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.ENABLED || ((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.REQUIRES_MODIFIER && event.hasShiftDown())) && !draggable_lists$isMouseOverScrollbar(event.x()) && draggable_lists$dragManager.mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY)) return true;
        return super.mouseDragged(event, deltaX, deltaY);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //$$     if (draggable_lists$dragManager.mouseReleased(mouseX, mouseY, button)) return true;
    //$$     return super.mouseReleased(mouseX, mouseY, button);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    //$$     if ((io.github.halfmasa.xaerobinding.config.Configs.DRAGGABLE_LISTS.getBooleanValue() && (((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.ENABLED || ((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_RESOURCE_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.REQUIRES_MODIFIER && net.minecraft.client.gui.screens.Screen.hasShiftDown())) && !draggable_lists$isMouseOverScrollbar(mouseX) && draggable_lists$dragManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
    //$$     return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    //$$ }
    //#endif

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (draggable_lists$dragManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    //#if MC >= 26.1
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta);
    //#else
    //$$ @Override
    //$$ public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    //$$     super.renderWidget(guiGraphics, mouseX, mouseY, delta);
    //#endif
        draggable_lists$dragManager.renderWidget(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    //#if MC >= 1.21.10
    public DragItem<PackSelectionModel.Entry, TransferableSelectionList.Entry> draggable_lists$getEntryAtPosition(double mouseX, double mouseY) {
        TransferableSelectionList.Entry entryAtPosition = getEntryAtPosition(mouseX, mouseY);
        return entryAtPosition instanceof DragItem<?, ?>
                ? (DragItem<PackSelectionModel.Entry, TransferableSelectionList.Entry>) entryAtPosition
                : null;
    }
    //#else
    //$$ public DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> draggable_lists$getEntryAtPosition(double mouseX, double mouseY) {
    //$$     TransferableSelectionList.PackEntry entryAtPosition = getEntryAtPosition(mouseX, mouseY);
    //$$     return (DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry>) entryAtPosition;
    //$$ }
    //#endif

    @Override
    //#if MC >= 1.21.10
    public int draggable_lists$getIndexOfEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.Entry> selectedItem) {
    //#else
    //$$ public int draggable_lists$getIndexOfEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> selectedItem) {
    //#endif
        return children().indexOf(selectedItem.draggable_lists$getUnderlyingEntry());
    }

    @Override
    public void draggable_lists$setDragging(boolean b) {
        super.setDragging(b);
    }

    @Override
    public int draggable_lists$getHeaderHeight() {
        //#if MC >= 1.21.10
        return 0;
        //#else
        //$$ return headerHeight;
        //#endif
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
        //#if MC >= 1.21.10
        return defaultEntryHeight;
        //#else
        //$$ return itemHeight;
        //#endif
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
        //#if MC >= 1.21.4
        return scrollAmount();
        //#else
        //$$ return getScrollAmount();
        //#endif
    }

    @Override
    public void draggable_lists$setScrollAmount(double v) {
        setScrollAmount(v);
    }

    @Override
    //#if MC >= 1.21.10
    public void draggable_lists$moveEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.Entry> item, int n) {
    //#else
    //$$ public void draggable_lists$moveEntry(DragItem<PackSelectionModel.Entry, TransferableSelectionList.PackEntry> item, int n) {
    //#endif
        if (item.draggable_lists$getUnderlyingData() instanceof AbstractPackDuckProvider duckProvider) {
            duckProvider.draggable_lists$moveTo(n);
        }
    }

    @Override
    public int draggable_lists$getItemCount() {
        return getItemCount();
    }

    @Override
    //#if MC >= 26.1
    public void draggable_lists$renderItem(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta, int i, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        extractItem(guiGraphics, mouseX, mouseY, tickDelta, children().get(i));
    }
    //#else
    //$$ public void draggable_lists$renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta, int i, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        //#if MC >= 1.21.10 && MC < 26.1
        //$$ renderItem(guiGraphics, mouseX, mouseY, tickDelta, children().get(i));
        //#else
        //$$ renderItem(guiGraphics, mouseX, mouseY, tickDelta, i, rowLeft, rowTop, rowWidth, rowHeight);
        //#endif
    //$$ }
    //#endif
}
