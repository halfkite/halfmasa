package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.components.*;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.xaerobinding.better_multiplayer.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.folder.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.privacy.CaptureDetectionManager;

@Mixin(JoinMultiplayerScreen.class)
public abstract class BetterMultiplayerScreenMixin extends Screen {
    @Shadow protected ServerSelectionList serverSelectionList;
    @Unique private EditBox halfmasa$search;
    @Unique private Button halfmasa$newFolder;
    @Unique private boolean halfmasa$active, halfmasa$folders, halfmasa$address, halfmasa$latency, halfmasa$hidden;
    @Unique private long halfmasa$revision;
    @Unique private String halfmasa$query = "";
    @Unique private boolean halfmasa$initialized;
    @Unique private boolean halfmasa$compact;
    protected BetterMultiplayerScreenMixin(Component title) { super(title); }
    /** Native edit/delete/join code must see the server represented by the selected tile. */
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;getSelected()Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;"))
    @Coerce
    private Object halfmasa$selectedServer(ServerSelectionList list) {
        var selected = list.getSelected();
        return selected instanceof CompactServerRow row ? row.selectedEntry() : selected;
    }
    @Inject(method = "init", at = @At("HEAD"))
    private void halfmasa$clearControls(CallbackInfo ci) { halfmasa$initialized = false; halfmasa$search = null; halfmasa$newFolder = null; }
    @Inject(method = "init", at = @At("TAIL"))
    private void halfmasa$init(CallbackInfo ci) {
        CaptureDetectionManager.INSTANCE.update();
        halfmasa$active = BetterMultiplayerConfig.active(); halfmasa$folders = BetterMultiplayerConfig.folders();
        halfmasa$address = BetterMultiplayerConfig.ADDRESS.getBooleanValue(); halfmasa$latency = BetterMultiplayerConfig.LATENCY.getBooleanValue();
        halfmasa$hidden = CaptureDetectionManager.INSTANCE.privacy.hidden();
        halfmasa$compact = BetterMultiplayerConfig.COLLAPSED_ICONS.getBooleanValue();
        halfmasa$revision = ServerFolderManager.getInstance().revision();
        if (halfmasa$folders) {
            int left = width / 2 - Math.min(150, width / 2 - 8);
            int space = Math.min(300, width - 16);
            halfmasa$search = addRenderableWidget(new EditBox(font, left, 32, Math.max(70, space - 106), 20, Component.translatable("halfmasa.multiplayer.search")));
            halfmasa$search.setHint(Component.translatable("halfmasa.multiplayer.search")); halfmasa$search.setValue(halfmasa$query);
            halfmasa$search.setResponder(value -> { halfmasa$query = value; ((ServerFolderWidget) serverSelectionList).halfmasa$search(value); });
            halfmasa$newFolder = addRenderableWidget(Button.builder(Component.translatable("halfmasa.multiplayer.new_folder"), b -> {
                String selected = ((ServerFolderWidget) serverSelectionList).halfmasa$entryId(serverSelectionList.getSelected());
                var manager = ServerFolderManager.getInstance();
                minecraft.setScreenAndShow(new ServerFolderScreen(this, null, manager.folder(selected) == null ? null : selected));
            }).bounds(left + space - 102, 32, 102, 20).build());
            ((ServerFolderWidget) serverSelectionList).halfmasa$search(halfmasa$query);
        }
        halfmasa$layout();
        ((ServerFolderWidget) serverSelectionList).halfmasa$rebuildFolders();
        halfmasa$initialized = true;
    }
    //#if MC >= 1.21.10
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void halfmasa$resize(CallbackInfo ci) {
        if (halfmasa$initialized) {
            halfmasa$layout();
            ((ServerFolderWidget) serverSelectionList).halfmasa$rebuildFolders();
        }
    }
    //#endif
    @Unique private void halfmasa$layout() {
        if (!BetterMultiplayerConfig.active()) return;
        int top = serverSelectionList.getY() + (BetterMultiplayerConfig.folders() ? 26 : 0);
        int available = Math.max(36, serverSelectionList.getHeight() - (BetterMultiplayerConfig.folders() ? 26 : 0) - 12);
        //#if MC >= 1.21.10
        serverSelectionList.updateSizeAndPosition(serverSelectionList.getWidth(), available, serverSelectionList.getX(), top);
        //#else
        //$$ serverSelectionList.updateSizeAndPosition(serverSelectionList.getWidth(), available, top);
        //#endif
        for (var child : children()) if (child instanceof Button button && button != halfmasa$newFolder && button.getY() >= height - 64) button.setY(button.getY() - 12);
        if (halfmasa$search != null) {
            int left = width / 2 - Math.min(150, width / 2 - 8), space = Math.min(300, width - 16);
            halfmasa$search.setX(left); halfmasa$search.setWidth(Math.max(70, space - 106));
            halfmasa$newFolder.setX(left + space - 102);
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void halfmasa$tick(CallbackInfo ci) {
        CaptureDetectionManager.INSTANCE.update();
        if (halfmasa$active != BetterMultiplayerConfig.active() || halfmasa$folders != BetterMultiplayerConfig.folders()) { rebuildWidgets(); return; }
        boolean hidden = CaptureDetectionManager.INSTANCE.privacy.hidden();
        if (halfmasa$compact != BetterMultiplayerConfig.COLLAPSED_ICONS.getBooleanValue()) {
            halfmasa$compact = BetterMultiplayerConfig.COLLAPSED_ICONS.getBooleanValue();
            ((ServerFolderWidget) serverSelectionList).halfmasa$rebuildFolders();
        }
        if (halfmasa$revision != ServerFolderManager.getInstance().revision() || halfmasa$address != BetterMultiplayerConfig.ADDRESS.getBooleanValue()
                || halfmasa$latency != BetterMultiplayerConfig.LATENCY.getBooleanValue() || halfmasa$hidden != hidden) {
            halfmasa$revision = ServerFolderManager.getInstance().revision();
            halfmasa$address = BetterMultiplayerConfig.ADDRESS.getBooleanValue(); halfmasa$latency = BetterMultiplayerConfig.LATENCY.getBooleanValue(); halfmasa$hidden = hidden;
            ((ServerFolderWidget) serverSelectionList).halfmasa$rebuildFolders();
        }
    }
    @Inject(method = "removed", at = @At("TAIL"))
    private void halfmasa$removed(CallbackInfo ci) { CaptureDetectionManager.INSTANCE.stop(); }
    //#if MC >= 26.1
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta) {
        super.extractRenderState(g,x,y,delta);
    //#elseif MC >= 1.21.10
    //$$ @Override public void render(GuiGraphics g,int x,int y,float delta) {
    //$$     super.render(g,x,y,delta);
    //#else
    //$$ @Inject(method = "render", at = @At("TAIL"))
    //$$ private void halfmasa$overlay(GuiGraphics g,int x,int y,float delta,CallbackInfo ci) {
    //#endif
        if (BetterMultiplayerConfig.active()) {
            String dragging = ((ServerFolderWidget) serverSelectionList).halfmasa$dragLabel();
            //#if MC >= 26.1
            if (dragging != null) g.text(font, "→ " + font.plainSubstrByWidth(dragging, 180), Math.min(x + 10, width - 190), Math.min(y + 8, height - 20), 0xffffd080);
            //#else
            //$$ if (dragging != null) g.drawString(font, "→ " + font.plainSubstrByWidth(dragging, 180), Math.min(x + 10, width - 190), Math.min(y + 8, height - 20), 0xffffd080);
            //#endif
            var privacy = CaptureDetectionManager.INSTANCE.privacy;
            String key = BetterMultiplayerConfig.MANUAL_PRIVACY.getBooleanValue() ? "manual" : !BetterMultiplayerConfig.AUTO_HIDE.getBooleanValue() ? "off" : privacy.state().name().toLowerCase(java.util.Locale.ROOT);
            if (key.equals("idle") && privacy.hidden()) key = "latched";
            //#if MC >= 26.1
            g.text(font, Component.translatable("halfmasa.multiplayer.privacy." + key), 6, height - 10, privacy.hidden() ? 0xffffcc55 : 0xffaaaaaa);
            //#else
            //$$ g.drawString(font, Component.translatable("halfmasa.multiplayer.privacy." + key), 6, height - 10, privacy.hidden() ? 0xffffcc55 : 0xffaaaaaa);
            //#endif
            //#if MC >= 26.1
            if (!ServerFolderManager.getInstance().error().isEmpty()) g.text(font, Component.translatable(ServerFolderManager.getInstance().error()), 6, 55, 0xffff5555);
            //#else
            //$$ if (!ServerFolderManager.getInstance().error().isEmpty()) g.drawString(font, Component.translatable(ServerFolderManager.getInstance().error()), 6, 55, 0xffff5555);
            //#endif
        }
    }
}
