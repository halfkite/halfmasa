package io.github.halfmasa.xaerobinding.mixin.draggable.server;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements DragItem<ServerData, ServerSelectionList.OnlineServerEntry> {
    @Shadow
    @Final
    private ServerData serverData;

    //#if MC >= 26.1
    @Shadow
    public abstract void extractContent(GuiGraphicsExtractor guiGraphics, int x, int y, boolean hovered, float tickDelta);
    //#elseif MC >= 1.21.10
    //$$ @Shadow
    //$$ public abstract void renderContent(GuiGraphics guiGraphics, int x, int y, boolean hovered, float tickDelta);
    //#else
    //$$ @Shadow
    //$$ public abstract void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f);
    //#endif

    @Shadow
    @Final
    ServerSelectionList field_19117;

    //#if MC < 1.21.10
    //$$ @Shadow
    //$$ public abstract boolean mouseClicked(double d, double e, int i);
    //#endif

    @Unique
    private boolean draggable_lists$isBeingDragged;

    //#if MC < 1.21.10
    //$$ @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    //$$ public void removeSwapEntries(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
    //$$     double l = field_19117.getRowLeft();
    //$$     double f = d - l;
    //$$
    //$$     // don't click buttons left of 16 pixels
    //$$     if (f <= 16) {
    //$$         mouseClicked(l + 32, e, i);
    //$$         cir.setReturnValue(true);
    //$$         cir.cancel();
    //$$     }
    //$$ }
    //#endif

    @Override
    public ServerData draggable_lists$getUnderlyingData() {
        return serverData;
    }

    @Override
    public ServerSelectionList.OnlineServerEntry draggable_lists$getUnderlyingEntry() {
        return (ServerSelectionList.OnlineServerEntry) (Object) this;
    }

    @Override
    //#if MC >= 26.1
    public void draggable_lists$render(GuiGraphicsExtractor guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
    //#else
    //$$ public void draggable_lists$render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
    //#endif
        if (!draggable_lists$isBeingDragged) return;
        //#if MC >= 26.1
        extractContent(guiGraphics, x, y, hovered, tickDelta);
        //#elseif MC >= 1.21.10
        //$$ renderContent(guiGraphics, x, y, hovered, tickDelta);
        //#else
        //$$ render(guiGraphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        //#endif
    }

    @Override
    public void draggable_lists$setBeingDragged(boolean v) {
        draggable_lists$isBeingDragged = v;
    }
}
