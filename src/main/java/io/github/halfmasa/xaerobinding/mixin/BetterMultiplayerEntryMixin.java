package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.better_multiplayer.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.folder.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.privacy.CaptureDetectionManager;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class BetterMultiplayerEntryMixin extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements ServerEntryLayout {
    //#if MC < 1.21.10
    //$$ @Unique private int halfmasa$x, halfmasa$y, halfmasa$width, halfmasa$height;
    //$$ public int getX(){return this$0.children().contains(this) ? this$0.getRowLeft() : halfmasa$x;} public int getY(){return this$0.children().contains(this) ? ((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)(Object)this$0).draggable_lists$getRowTop(this$0.children().indexOf(this)) : halfmasa$y;}
    //$$ public int getWidth(){return this$0.children().contains(this) ? this$0.getRowWidth() : halfmasa$width;} public int getHeight(){return ((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)(Object)this$0).draggable_lists$getItemHeight();}
    //$$ public int getContentX(){return getX();} public int getContentY(){return getY();}
    //$$ public int getContentWidth(){return getWidth();} public int getContentRight(){return getX()+getWidth();}
    //#endif
    @Shadow @Final private ServerData serverData;
    //#if MC >= 26.1
    @Shadow @Final private ServerSelectionList this$0;
    //#else
    //$$ @Shadow(aliases = {"field_19117"}, remap = false) @Final private ServerSelectionList this$0;
    //#endif
    //#if MC >= 26.1
    @Inject(method = "extractContent", at = @At("HEAD"))
    //#else
    //#if MC >= 1.21.10
    //$$ @Inject(method = "renderContent", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"))
    //#endif
    //#endif
    //#if MC >= 26.1
    private void halfmasa$prepareRow(GuiGraphicsExtractor g, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#else
    //#if MC >= 1.21.10
    //$$ private void halfmasa$prepareRow(GuiGraphics g, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#else
    //$$ private void halfmasa$prepareRow(GuiGraphics g, int index, int rowY, int rowX, int rowWidth, int rowHeight, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#endif
    //#endif
        //#if MC < 1.21.10
        //$$ halfmasa$x=rowX; halfmasa$y=rowY; halfmasa$width=rowWidth; halfmasa$height=rowHeight+4;
        //#endif
        if (BetterMultiplayerConfig.active() && serverData.state() == ServerData.State.INITIAL) ServerLatencyOverlay.invalidate(serverData);
    }
    //#if MC >= 26.1
    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 3)
    //#elseif MC >= 1.21.10
    //$$ @ModifyArg(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 3)
    //#elseif MC >= 1.21.8
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 3)
    //#elseif MC >= 1.21.4
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"), index = 3)
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"), index = 3)
    //#endif
    private int halfmasa$motdOffset(int y) {
        if (halfmasa$address()) return getContentY() + (BetterMultiplayerConfig.ADDRESS_POSITION.getOptionListValue() == BetterMultiplayerConfig.Position.BELOW_NAME ? 22 : 12);
        return y;
    }
    //#if MC >= 26.1
    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 2)
    //#elseif MC >= 1.21.10
    //$$ @ModifyArg(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 2)
    //#elseif MC >= 1.21.8
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 2)
    //#elseif MC >= 1.21.4
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"), index = 2)
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"), index = 2)
    //#endif
    private int halfmasa$nameIndent(int x) { return x + halfmasa$indent(); }
    //#if MC >= 26.1
    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 2)
    //#elseif MC >= 1.21.10
    //$$ @ModifyArg(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 2)
    //#elseif MC >= 1.21.8
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"), index = 2)
    //#elseif MC >= 1.21.4
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"), index = 2)
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"), index = 2)
    //#endif
    private int halfmasa$motdIndent(int x) { return x + halfmasa$indent(); }
    //#if MC >= 26.1
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    //#else
    //#if MC >= 1.21.10
    //$$ @WrapOperation(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    //#else
    //$$ @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    //#endif
    //#endif
    private java.util.List<FormattedCharSequence> halfmasa$fitMotd(Font font, net.minecraft.network.chat.FormattedText text, int width,
                                                                    Operation<java.util.List<FormattedCharSequence>> original) {
        var lines = original.call(font, text, Math.max(1, width - halfmasa$indent() - halfmasa$statusWidth()));
        return halfmasa$address() && lines.size() > 1 ? lines.subList(0, 1) : lines;
    }
    //#if MC >= 26.1
    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 1)
    //#elseif MC >= 1.21.10
    //$$ @ModifyArg(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 1)
    //#elseif MC >= 1.21.8
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 1)
    //#elseif MC >= 1.21.4
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"), index = 1)
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"), index = 1)
    //#endif
    private String halfmasa$fitName(String text) {
        if (!BetterMultiplayerConfig.active()) return text;
        String path = BetterMultiplayerConfig.folders() ? ServerFolderManager.getInstance().path(ServerFolderManager.getInstance().id(serverData)) : "";
        String label = text + (path.isEmpty() ? "" : " [" + path + "]");
        int count = Minecraft.getInstance().font.width(serverData.status == null ? "" : serverData.status.getString());
        return Minecraft.getInstance().font.plainSubstrByWidth(label, Math.max(0, getContentWidth() - 35 - halfmasa$indent() - count - 24));
    }
    @Unique private int halfmasa$statusWidth() {
        if (!BetterMultiplayerConfig.active() || !BetterMultiplayerConfig.LATENCY.getBooleanValue()) return 0;
        //#if MC >= 26.1
        return Minecraft.getInstance().font.width(ServerLatencyOverlay.label(serverData).text()) + 22;
        //#else
        //$$ return Minecraft.getInstance().font.width(ServerLatencyOverlay.label(serverData).text()) + 22;
        //#endif
    }
    //#if MC >= 26.1
    @Inject(method = "extractContent", at = @At("TAIL"))
    //#else
    //#if MC >= 1.21.10
    //$$ @Inject(method = "renderContent", at = @At("TAIL"))
    //#else
    //$$ @Inject(method = "render", at = @At("TAIL"))
    //#endif
    //#endif
    //#if MC >= 26.1
    private void halfmasa$details(GuiGraphicsExtractor g, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#else
    //#if MC >= 1.21.10
    //$$ private void halfmasa$details(GuiGraphics g, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#else
    //$$ private void halfmasa$details(GuiGraphics g, int index, int rowY, int rowX, int rowWidth, int rowHeight, int x, int y, boolean hover, float delta, CallbackInfo ci) {
    //#endif
    //#endif
        if (!BetterMultiplayerConfig.active()) return;
        Font font = Minecraft.getInstance().font;
        int left = getContentX() + 35 + halfmasa$indent();
        int available = Math.max(10, getContentWidth() - 40 - halfmasa$indent()
                - halfmasa$statusWidth()
                );
        if (halfmasa$address()) {
            boolean belowName = BetterMultiplayerConfig.ADDRESS_POSITION.getOptionListValue() == BetterMultiplayerConfig.Position.BELOW_NAME;
            int addressY = getContentY() + (belowName ? 12 : 22);
            //#if MC >= 26.1
            g.text(font, font.plainSubstrByWidth(serverData.ip, available), left, addressY, 0xff999999);
            //#else
            //$$ g.drawString(font, font.plainSubstrByWidth(serverData.ip, available), left, addressY, 0xff999999);
            //#endif
        }
        if (BetterMultiplayerConfig.LATENCY.getBooleanValue()) {
            var label = ServerLatencyOverlay.label(serverData);
            //#if MC >= 26.1
            g.text(font, label.text(), getContentRight() - font.width(label.text()) - 20, getContentY() + 12, label.color());
            //#else
            //$$ g.drawString(font, label.text(), getContentRight() - font.width(label.text()) - 20, getContentY() + 12, label.color());
            //#endif
        }
        if (hover && x >= left && x < getContentRight() - 17) {
            java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(net.minecraft.network.chat.Component.literal(serverData.name));
            if (halfmasa$address()) tooltip.add(net.minecraft.network.chat.Component.literal(serverData.ip));
            if (serverData.motd != null) tooltip.add(serverData.motd);
            String path = ServerFolderManager.getInstance().path(ServerFolderManager.getInstance().id(serverData));
            if (BetterMultiplayerConfig.folders() && !path.isEmpty()) tooltip.add(net.minecraft.network.chat.Component.literal(path));
            //#if MC >= 1.21.8
            g.setComponentTooltipForNextFrame(font, tooltip, x, y);
            //#else
            //$$ var screen=io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(Minecraft.getInstance());
            //$$ if(screen!=null)screen.setTooltipForNextRenderPass(tooltip.stream().map(net.minecraft.network.chat.Component::getVisualOrderText).toList());
            //#endif
        }
    }
    @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
    private void halfmasa$folderSwap(int from, int to, CallbackInfo ci) {
        if (!BetterMultiplayerConfig.folders()) return;
        ci.cancel(); var manager = ServerFolderManager.getInstance();
        String id = manager.id(serverData);
        if (to >= 0 && to < this$0.children().size()) {
            String target = ((ServerFolderWidget) this$0).halfmasa$entryId(this$0.children().get(to));
            if (target != null) manager.move(id, manager.parent(target), to < from ? target : halfmasa$nextAfter(target));
        }
        ((ServerFolderWidget) this$0).halfmasa$rebuildFolders();
    }
    @Unique private String halfmasa$nextAfter(String target) {
        var manager = ServerFolderManager.getInstance(); boolean found = false;
        for (var entry : this$0.children()) {
            String id = ((ServerFolderWidget) this$0).halfmasa$entryId(entry);
            if (found && java.util.Objects.equals(manager.parent(id), manager.parent(target))) return id;
            if (target.equals(id)) found = true;
        }
        return null;
    }
    @Unique private boolean halfmasa$address() {
        return BetterMultiplayerConfig.active() && BetterMultiplayerConfig.ADDRESS.getBooleanValue() && !CaptureDetectionManager.INSTANCE.privacy.hidden();
    }
    @Unique private int halfmasa$indent() {
        if (!BetterMultiplayerConfig.folders()) return 0;
        var manager = ServerFolderManager.getInstance(); int depth = 0;
        String parent = manager.parent(manager.id(serverData));
        while (parent != null && depth < 12) { depth++; parent = manager.parent(parent); }
        return depth * 12;
    }
}
