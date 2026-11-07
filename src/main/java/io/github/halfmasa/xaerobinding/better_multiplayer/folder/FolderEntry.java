package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.*;
//#else

//#endif
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

public final class FolderEntry extends ServerDisplayEntry {
    public final String id;
    private final int depth;
    private final Runnable changed;
    public FolderEntry(String id, int depth, Runnable changed) { this.id = id; this.depth = depth; this.changed = changed; }
    @Override protected boolean matches(ServerSelectionList.Entry other) { return other instanceof FolderEntry f && id.equals(f.id); }
    @Override public void join() { ServerFolderManager.getInstance().toggle(id); changed.run(); }
    //#if MC >= 26.1
    @Override public void extractContent(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, float delta) {
    //#else
    //#if MC >= 1.21.10
    //$$ @Override public void renderContent(GuiGraphics graphics, int x, int y, boolean hovered, float delta) {
    //#else
    //$$ @Override public void halfmasa$renderContent(GuiGraphics graphics, int x, int y, boolean hovered, float delta) {
    //#endif
    //#endif
        var manager = ServerFolderManager.getInstance(); var folder = manager.folder(id);
        if (folder == null) return;
        var font = Minecraft.getInstance().font;
        int indent = Math.min(depth, 12) * 12;
        String label = (folder.expanded ? "▾ " : "▸ ") + folder.name;
        //#if MC >= 26.1
        graphics.text(font, font.plainSubstrByWidth(label, Math.max(30, getContentWidth() - indent - 90)), getContentX() + 6 + indent, getContentY() + 7, 0xffffd080);
        //#else
        //$$ graphics.drawString(font, font.plainSubstrByWidth(label, Math.max(30, getContentWidth() - indent - 90)), getContentX() + 6 + indent, getContentY() + 7, 0xffffd080);
        //#endif
        //#if MC >= 26.1
        graphics.text(font, Component.translatable("halfmasa.multiplayer.server_count", manager.count(id)), getContentX() + 20 + indent, getContentY() + 19, 0xffaaaaaa);
        //#else
        //$$ graphics.drawString(font, Component.translatable("halfmasa.multiplayer.server_count", manager.count(id)), getContentX() + 20 + indent, getContentY() + 19, 0xffaaaaaa);
        //#endif
    }
    //#if MC >= 1.21.10
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //#else
    //$$ @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
    //$$         var event=new ServerPointer(mouseX,mouseY,button,net.minecraft.client.gui.screens.Screen.hasShiftDown());
    //#endif
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        ServerFolderManager.getInstance().toggle(id); changed.run(); return true;
    }
    //#if MC >= 1.21.10
    @Override public boolean keyPressed(KeyEvent event) {
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
    //$$         var event=new ServerKey(keyCode,modifiers);
    //#endif
        if (event.isConfirmation()) { ServerFolderManager.getInstance().toggle(id); changed.run(); return true; }
        return false;
    }
    @Override public Component getNarration() {
        var manager = ServerFolderManager.getInstance(); var folder = manager.folder(id);
        return Component.literal(folder == null ? "" : folder.name).append(" ").append(Component.translatable("halfmasa.multiplayer.server_count", manager.count(id)));
    }
}
