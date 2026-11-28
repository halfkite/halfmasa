package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DragList;
import io.github.halfmasa.xaerobinding.draggable.DragManager;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.DragMode;
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
        if (draggable_lists$dragManager.isDragging()) {
            draggable_lists$dragManager.renderListItems(guiGraphics, mouseX, mouseY, tickDelta);
        } else {
            //#if MC >= 26.1
            super.extractListItems(guiGraphics, mouseX, mouseY, tickDelta);
            //#else
            //$$ super.renderListItems(guiGraphics, mouseX, mouseY, tickDelta);
            //#endif
        }
    }

    @Unique
    private boolean draggable_lists$drag(double mouseX, double mouseY, int button, boolean shiftDown) {
        if (button != draggable_lists$primaryButton()) return false;
        if (draggable_lists$dragManager.isDragging()) return true;
        if (!Configs.DRAGGABLE_LISTS.getBooleanValue()) return false;
        DragMode mode = (DragMode) Configs.DRAG_RESOURCE_MODE.getOptionListValue();
        if (mode != DragMode.ENABLED && !(mode == DragMode.REQUIRES_MODIFIER && shiftDown)) return false;

        //#if MC >= 26.3
        if (isOverScrollbar(mouseX, mouseY)) return false;
        //#endif
        var item = draggable_lists$getEntryAtPosition(mouseX, mouseY);
        if (item == null || item.draggable_lists$getUnderlyingData().isFixedPosition()) return false;
        //#if MC >= 1.21.10
        if (mouseX < item.draggable_lists$getUnderlyingEntry().getContentX() + 32) return false;
        //#endif
        return draggable_lists$dragManager.mouseDragged(mouseX, mouseY, button, 0, 0);
    }

    @Unique
    private static int draggable_lists$primaryButton() {
        //#if MC >= 26.3
        return org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
        //#else
        //$$ return org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
        //#endif
    }

    //#if MC >= 1.21.10
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == draggable_lists$primaryButton() && draggable_lists$dragManager.isDragging()) {
            draggable_lists$dragManager.mouseReleased(event.x(), event.y(), event.button());
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggable_lists$drag(event.x() - deltaX, event.y() - deltaY, event.button(), event.hasShiftDown())) return true;
        return super.mouseDragged(event, deltaX, deltaY);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //$$     if (button == draggable_lists$primaryButton() && draggable_lists$dragManager.isDragging()) {
    //$$         draggable_lists$dragManager.mouseReleased(mouseX, mouseY, button);
    //$$         return true;
    //$$     }
    //$$     return super.mouseReleased(mouseX, mouseY, button);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    //$$     if (draggable_lists$drag(mouseX - deltaX, mouseY - deltaY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown())) return true;
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
        if (n < 0 || n >= getItemCount() || draggable_lists$getIndexOfEntry(item) < 0) return;
        if (item.draggable_lists$getUnderlyingData() instanceof AbstractPackDuckProvider duckProvider
                && children().get(n) instanceof DragItem<?, ?> target
                && target.draggable_lists$getUnderlyingData() instanceof PackSelectionModel.Entry pack) {
            duckProvider.draggable_lists$moveTo(pack.getId());
        }
    }

    //#if MC >= 1.21.10
    @Override
    public int draggable_lists$getIndexAtY(double mouseY) {
        // The title is a separate, shorter entry at index zero.
        int count = getItemCount();
        for (int index = 1; index < count; index++) {
            if (mouseY < getRowBottom(index)) return index;
        }
        return count - 1;
    }
    //#endif

    @Override
    public int draggable_lists$getItemCount() {
        return getItemCount();
    }

    @Override
    //#if MC >= 26.1
    public void draggable_lists$renderItem(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta, int i, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        TransferableSelectionList.Entry entry = children().get(i);
        int offsetY = rowTop - entry.getY();
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(0, offsetY);
        extractItem(guiGraphics, mouseX, mouseY - offsetY, tickDelta, entry);
        pose.popMatrix();
    }
    //#else
    //$$ public void draggable_lists$renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta, int i, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        //#if MC >= 1.21.10 && MC < 26.1
        //$$ TransferableSelectionList.Entry entry = children().get(i);
        //$$ int offsetY = rowTop - entry.getY();
        //$$ var pose = guiGraphics.pose();
        //$$ pose.pushMatrix();
        //$$ pose.translate(0, offsetY);
        //$$ renderItem(guiGraphics, mouseX, mouseY - offsetY, tickDelta, entry);
        //$$ pose.popMatrix();
        //#else
        //$$ renderItem(guiGraphics, mouseX, mouseY, tickDelta, i, rowLeft, rowTop, rowWidth, rowHeight);
        //#endif
    //$$ }
    //#endif
}
