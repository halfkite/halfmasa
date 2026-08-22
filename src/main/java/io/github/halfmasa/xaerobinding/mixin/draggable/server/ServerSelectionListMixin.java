package io.github.halfmasa.xaerobinding.mixin.draggable.server;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DragList;
import io.github.halfmasa.xaerobinding.draggable.DragManager;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
import io.github.halfmasa.xaerobinding.draggable.duck.ServerListDuckProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerSelectionList.class)
@Environment(EnvType.CLIENT)
public abstract class ServerSelectionListMixin extends ObjectSelectionList<ServerSelectionList.Entry> implements DragList<ServerData, ServerSelectionList.OnlineServerEntry> {
    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Shadow
    public abstract void setSelected(@Nullable ServerSelectionList.Entry entry);

    @Shadow
    public abstract void updateOnlineServers(ServerList serverList);

    @Shadow
    public abstract int getRowWidth();

    @Unique
    private final DragManager<ServerData, ServerSelectionList.OnlineServerEntry> draggable_lists$dragManager = new DragManager<>(this);

    public ServerSelectionListMixin(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
        super(minecraftClient, i, j, k, l);
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

    //#if MC >= 1.21.10
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggable_lists$dragManager.mouseReleased(event.x(), event.y(), event.button())) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if ((io.github.halfmasa.xaerobinding.config.Configs.DRAGGABLE_LISTS.getBooleanValue() && (((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_SERVER_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.ENABLED || ((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_SERVER_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.REQUIRES_MODIFIER && event.hasShiftDown())) && draggable_lists$dragManager.mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY)) return true;
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
    //$$     if ((io.github.halfmasa.xaerobinding.config.Configs.DRAGGABLE_LISTS.getBooleanValue() && (((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_SERVER_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.ENABLED || ((io.github.halfmasa.xaerobinding.config.DragMode) io.github.halfmasa.xaerobinding.config.Configs.DRAG_SERVER_MODE.getOptionListValue()) == io.github.halfmasa.xaerobinding.config.DragMode.REQUIRES_MODIFIER && net.minecraft.client.gui.screens.Screen.hasShiftDown())) && draggable_lists$dragManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
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
    public DragItem<ServerData, ServerSelectionList.OnlineServerEntry> draggable_lists$getEntryAtPosition(double mouseX, double mouseY) {
        ServerSelectionList.Entry entryAtPosition = getEntryAtPosition(mouseX, mouseY);
        if (entryAtPosition instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry) {
            return (DragItem<ServerData, ServerSelectionList.OnlineServerEntry>) onlineServerEntry;
        }
        return null;
    }

    public int draggable_lists$getIndexOfEntry(DragItem<ServerData, ServerSelectionList.OnlineServerEntry> selectedItem) {
        return children().indexOf(selectedItem.draggable_lists$getUnderlyingEntry());
    }

    public void draggable_lists$setDragging(boolean b) {
        super.setDragging(b);
    }

    public int draggable_lists$getHeaderHeight() {
        //#if MC >= 1.21.10
        return 0;
        //#else
        //$$ return headerHeight;
        //#endif
    }

    public int draggable_lists$getY() {
        return getY();
    }

    public int draggable_lists$getBottom() {
        return getBottom();
    }

    public int draggable_lists$getItemHeight() {
        //#if MC >= 1.21.10
        return defaultEntryHeight;
        //#else
        //$$ return itemHeight;
        //#endif
    }

    public int draggable_lists$getRowTop(int i) {
        return getRowTop(i);
    }

    public int draggable_lists$getRowBottom(int i) {
        return getRowBottom(i);
    }

    public double draggable_lists$getRowLeft() {
        return getRowLeft();
    }

    public int draggable_lists$getRowWidth() {
        return getRowWidth();
    }

    public double draggable_lists$getScrollAmount() {
        //#if MC >= 1.21.4
        return scrollAmount();
        //#else
        //$$ return getScrollAmount();
        //#endif
    }

    public void draggable_lists$setScrollAmount(double v) {
        setScrollAmount(v);
    }

    public void draggable_lists$moveEntry(DragItem<ServerData, ServerSelectionList.OnlineServerEntry> item, int n) {
        ServerList servers = screen.getServers();
        if (servers instanceof ServerListDuckProvider duckProvider) {
            duckProvider.draggable_lists$moveItem(item, n);
            servers.save();
            updateOnlineServers(servers);
        }
    }

    public int draggable_lists$getItemCount() {
        return getItemCount();
    }

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
