package io.github.halfmasa.xaerobinding.mixin.draggable.server;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.DraggableLists;
import net.minecraft.client.gui.GuiGraphics;
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

    @Shadow
    public abstract void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f);

    @Shadow
    @Final
    ServerSelectionList field_19117;

    @Shadow
    public abstract boolean mouseClicked(double d, double e, int i);

    @Unique
    private boolean draggable_lists$isBeingDragged;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void removeSwapEntries(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        double l = field_19117.getRowLeft();
        double f = d - l;

        // don't click buttons left of 16 pixels
        if (f <= 16) {
            mouseClicked(l + 32, e, i);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Override
    public ServerData draggable_lists$getUnderlyingData() {
        return serverData;
    }

    @Override
    public ServerSelectionList.OnlineServerEntry draggable_lists$getUnderlyingEntry() {
        return (ServerSelectionList.OnlineServerEntry) (Object) this;
    }

    @Override
    public void draggable_lists$render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        if (!draggable_lists$isBeingDragged) return;
        render(guiGraphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
    }

    @Override
    public void draggable_lists$setBeingDragged(boolean v) {
        draggable_lists$isBeingDragged = v;
    }
}
