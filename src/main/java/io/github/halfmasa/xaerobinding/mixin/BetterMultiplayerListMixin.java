package io.github.halfmasa.xaerobinding.mixin;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.*;
import net.minecraft.client.multiplayer.*;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#else

//#endif
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.folder.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.privacy.CaptureDetectionManager;

@Mixin(ServerSelectionList.class)
public abstract class BetterMultiplayerListMixin extends ObjectSelectionList<ServerSelectionList.Entry> implements ServerFolderWidget {
    @Shadow @Final private JoinMultiplayerScreen screen;
    @Shadow @Final private List<ServerSelectionList.OnlineServerEntry> onlineServers;
    @Shadow private void refreshEntries() { throw new AssertionError(); }
    @Unique private double halfmasa$scroll() {
        //#if MC >= 1.21.4
        return scrollAmount();
        //#else
        //$$ return getScrollAmount();
        //#endif
    }
    @Unique private String halfmasa$query = "";
    @Unique private ServerSelectionList.Entry halfmasa$pressed;
    @Unique private double halfmasa$pressX, halfmasa$pressY;
    @Unique private boolean halfmasa$dragging;
    public BetterMultiplayerListMixin(Minecraft mc, int w, int h, int y, int row) { super(mc, w, h, y, row); }
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void halfmasa$fitRowWidth(CallbackInfoReturnable<Integer> cir) {
        if (BetterMultiplayerConfig.active())
            cir.setReturnValue(Math.max(CompactServerRow.ICON_SIZE + CompactServerRow.GAP,
                    Math.min(cir.getReturnValue(), getWidth() - CompactServerRow.GAP * 3)));
    }
    @Inject(method = "refreshEntries", at = @At("RETURN"))
    private void halfmasa$organize(CallbackInfo ci) {
        if (!BetterMultiplayerConfig.active()) return;
        var manager = ServerFolderManager.getInstance();
        manager.reconcile(onlineServers.stream().map(ServerSelectionList.OnlineServerEntry::getServerData).toList());
        Map<String, ServerSelectionList.OnlineServerEntry> entries = new HashMap<>();
        for (var entry : onlineServers) entries.put(manager.id(entry.getServerData()), entry);
        List<ServerSelectionList.Entry> rows = new ArrayList<>();
        List<ServerSelectionList.Entry> lan = children().stream().filter(e -> !(e instanceof ServerSelectionList.OnlineServerEntry)).toList();
        lan = lan.stream().filter(e -> !(e instanceof FolderEntry) && !(e instanceof CompactServerRow)).toList();
        if (BetterMultiplayerConfig.folders()) {
            for (var node : manager.visible(halfmasa$query, !CaptureDetectionManager.INSTANCE.privacy.hidden())) {
                var entry = node.folder() ? new FolderEntry(node.id(), node.depth(), this::halfmasa$rebuildFolders) : entries.get(node.id());
                if (entry != null) rows.add(entry);
                if (node.folder() && !manager.folder(node.id()).expanded && halfmasa$query.isEmpty()
                        && BetterMultiplayerConfig.COLLAPSED_ICONS.getBooleanValue()) {
                    List<String> compact = manager.compactServers(node.id());
                    int columns = CompactServerRow.columns(getRowWidth() - 4, node.depth() + 1);
                    for (int i = 0; i < compact.size(); i += columns)
                        rows.add(new CompactServerRow(node.id(), node.depth() + 1, i,
                                compact.subList(i, Math.min(i + columns, compact.size())), entries,
                                screen, (ServerSelectionList) (Object) this));
                }
            }
        } else rows.addAll(onlineServers);
        rows.addAll(lan);
        var selected = getSelected(); String selectedId = halfmasa$entryId(selected);
        if (selectedId != null)
            for (var entry : rows) if (entry instanceof CompactServerRow row && row.serverIds().contains(selectedId)) row.select(selectedId);
        for(var entry:rows) if(entry instanceof ServerDisplayEntry display) display.halfmasa$bind((ServerSelectionList)(Object)this);
        clearEntries();
        //#if MC >= 1.21.10
        for (var entry : rows) addEntry(entry, defaultEntryHeight);
        //#else
        //$$ for (var entry : rows) addEntry(entry);
        //#endif
        // refreshEntries also runs before the multiplayer screen creates its buttons.
        // Only an existing selection needs a notification; initial null is already correct.
        if (selected != null) setSelected(rows.stream().filter(entry -> selectedId != null ? selectedId.equals(halfmasa$entryId(entry)) : entry == selected).findFirst().orElse(null));
        setScrollAmount(halfmasa$scroll());
    }
    @Unique private boolean halfmasa$enhancedHeight() {
        return BetterMultiplayerConfig.active() && (BetterMultiplayerConfig.ADDRESS.getBooleanValue() || BetterMultiplayerConfig.LATENCY.getBooleanValue() || BetterMultiplayerConfig.folders());
    }
    @Override public void halfmasa$rebuildFolders() {
        halfmasa$pressed = null; halfmasa$dragging = false;
        //#if MC >= 1.21.10
        for (var entry : onlineServers) entry.setHeight(defaultEntryHeight);
        //#else

        //#endif
        refreshEntries();
    }
    @Override public void halfmasa$search(String query) { halfmasa$query = query; halfmasa$rebuildFolders(); setScrollAmount(0); }
    @Override public String halfmasa$entryId(Object entry) {
        if (entry instanceof CompactServerRow row) return row.selectedId();
        if (entry instanceof FolderEntry folder) return folder.id;
        if (entry instanceof ServerSelectionList.OnlineServerEntry server) return ServerFolderManager.getInstance().id(server.getServerData());
        return null;
    }
    //#if MC >= 1.21.10
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //#else
    //$$ @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
    //$$         var event=new ServerPointer(mouseX,mouseY,button,net.minecraft.client.gui.screens.Screen.hasShiftDown());
    //#endif
        //#if MC >= 1.21.10
        if (!BetterMultiplayerConfig.folders()) return super.mouseClicked(event, doubleClick);
        //#else
        //$$ if (!BetterMultiplayerConfig.folders()) return super.mouseClicked(mouseX,mouseY,button);
        //#endif
        var entry = getEntryAtPosition(event.x(), event.y()); String id = halfmasa$entryId(entry);
        //#if MC >= 1.21.10
        if (entry instanceof CompactServerRow) return super.mouseClicked(event, doubleClick);
        //#else
        //$$ if (entry instanceof CompactServerRow row) return button == InputConstants.MOUSE_BUTTON_RIGHT ? row.mouseClicked(mouseX,mouseY,button) : super.mouseClicked(mouseX,mouseY,button);
        //#endif
        if (id != null && event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            Minecraft.getInstance().setScreenAndShow(new ServerFolderScreen(screen, id, null)); return true;
        }
        if (id != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            halfmasa$pressed = entry; halfmasa$pressX = event.x(); halfmasa$pressY = event.y();
            // Defer folders' toggle to release so a drag never expands/collapses its source.
            if (entry instanceof FolderEntry) { setSelected(entry); return true; }
        }
        //#if MC >= 1.21.10
        return super.mouseClicked(event, doubleClick);
        //#else
        //$$ return super.mouseClicked(mouseX,mouseY,button);
        //#endif
    }
    @Override public boolean halfmasa$folderDragged(double mouseX,double mouseY,int button,boolean shift, double dx, double dy) {
        if (BetterMultiplayerConfig.folders() && halfmasa$pressed != null && halfmasa$query.isEmpty()) {
            halfmasa$dragging |= Math.hypot(mouseX - halfmasa$pressX, mouseY - halfmasa$pressY) >= 5;
            if (halfmasa$dragging) {
                if (mouseY < getY() + 12) setScrollAmount(halfmasa$scroll() - 8);
                if (mouseY > getBottom() - 12) setScrollAmount(halfmasa$scroll() + 8);
                return true;
            }
        }
        return false;
    }
    @Override public boolean halfmasa$folderReleased(double mouseX,double mouseY,int button,boolean shift) {
        var pressed = halfmasa$pressed; boolean dragged = halfmasa$dragging;
        halfmasa$pressed = null; halfmasa$dragging = false;
        if (BetterMultiplayerConfig.folders() && pressed != null) {
            if (dragged) {
                if (!isMouseOver(mouseX, mouseY)) return true;
                var target = getEntryAtPosition(mouseX, mouseY);
                String source = halfmasa$entryId(pressed), destination = halfmasa$entryId(target);
                if (!Objects.equals(source, destination)) {
                    var manager = ServerFolderManager.getInstance();
                    // Shift-drop before a folder sorts it; normal drop enters it.
                    if (target instanceof FolderEntry && !shift) manager.move(source, destination, null);
                    else manager.move(source, destination == null ? null : manager.parent(destination), destination);
                    halfmasa$rebuildFolders();
                }
                return true;
            }
            if (pressed instanceof FolderEntry && pressed == getEntryAtPosition(mouseX, mouseY)) { ((FolderEntry) pressed).join(); return true; }
        }
        return false;
    }
    @Override public String halfmasa$dragLabel() {
        if (!halfmasa$dragging || halfmasa$pressed == null) return null;
        var manager = ServerFolderManager.getInstance(); String id = halfmasa$entryId(halfmasa$pressed);
        var folder = manager.folder(id); var server = manager.server(id);
        return folder != null ? folder.name : server == null ? null : server.name;
    }
}
