package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#else

//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#else

//#endif
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
//#if MC >= 26.3
import fi.dy.masa.malilib.util.input.ScanCodes;
//#else
//$$ import org.lwjgl.glfw.GLFW;
//#endif
import io.github.halfmasa.xaerobinding.better_multiplayer.BetterMultiplayerConfig;
import io.github.halfmasa.xaerobinding.better_multiplayer.ServerLatencyOverlay;
import io.github.halfmasa.xaerobinding.better_multiplayer.privacy.CaptureDetectionManager;

/** A display row references live native entries; it owns no server or Ping data. */
public final class CompactServerRow extends ServerDisplayEntry {
    public static final int ICON_SIZE = 32;
    public static final int GAP = 4;
    private static final int STRIDE = ICON_SIZE + GAP;
    private static final int CONTROL_SIZE = 10;
    private static final int CONTROL_TOP = ICON_SIZE - CONTROL_SIZE - 1;
    public final String folderId;
    private final int depth, offset;
    private final List<String> ids;
    private final Map<String, ServerSelectionList.OnlineServerEntry> servers;
    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;
    private int selected;
    private String lastClickedId;
    private long halfmasa$lastClick;

    public record Tile(int x, int y, int size) {
        public boolean contains(double mouseX, double mouseY) { return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size; }
        /** 2 selects; -1/0/1 are the small bottom-left/join/right controls. */
        public int action(double mouseX, double mouseY) {
            if (mouseY < y + CONTROL_TOP) return 2;
            return mouseX < x + 1 + CONTROL_SIZE ? -1 : mouseX >= x + size - CONTROL_SIZE - 1 ? 1 : 0;
        }
    }

    public CompactServerRow(String folderId, int depth, int offset, List<String> ids,
            Map<String, ServerSelectionList.OnlineServerEntry> servers, JoinMultiplayerScreen screen, ServerSelectionList list) {
        this.folderId = folderId; this.depth = depth; this.offset = offset; this.ids = List.copyOf(ids);
        this.servers = servers; this.screen = screen; this.list = list;
    }
    public static int columns(int contentWidth, int depth) {
        return Math.max(1, (contentWidth - indent(contentWidth, depth) + GAP) / STRIDE);
    }
    private static int indent(int width, int depth) { return Math.min(Math.min(depth, 12) * 12 + 6, Math.max(0, width - ICON_SIZE)); }
    public List<String> serverIds() { return ids; }
    public String selectedId() { return ids.isEmpty() ? null : ids.get(selected); }
    public ServerSelectionList.OnlineServerEntry selectedEntry() { return servers.get(selectedId()); }
    public void select(String id) { int index = ids.indexOf(id); if (index >= 0) selected = index; }
    public Tile tile(int index) { return new Tile(getContentX() + indent(getContentWidth(), depth) + index * STRIDE, getContentY(), ICON_SIZE); }
    @Override protected boolean matches(ServerSelectionList.Entry other) { return other instanceof CompactServerRow row && folderId.equals(row.folderId) && offset == row.offset; }
    //#if MC >= 26.1
    @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
    //#else
    //#if MC >= 1.21.10
    //$$ @Override public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
    //#else
    //$$ @Override public void halfmasa$renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
    //#endif
    //#endif
        for (int i = 0; i < ids.size(); i++) {
            var entry = servers.get(ids.get(i)); if (entry == null) continue;
            Tile tile = tile(i); boolean over = hovered && tile.contains(mouseX, mouseY);
            renderNativeIcon(graphics, entry, tile, delta);
            if (list.getSelected() == this && i == selected) {
                graphics.fill(tile.x - 1, tile.y - 1, tile.x + ICON_SIZE + 1, tile.y, 0xffffffff);
                graphics.fill(tile.x - 1, tile.y + ICON_SIZE, tile.x + ICON_SIZE + 1, tile.y + ICON_SIZE + 1, 0xffffffff);
            }
            if (over) renderControls(graphics, tile, ids.get(i), mouseX, mouseY);
            else renderCaption(graphics, tile, entry.getServerData());
            //#if MC >= 26.1
            if (over) graphics.setTooltipForNextFrame(Minecraft.getInstance().font,
                    tooltip(entry.getServerData(), tile.action(mouseX, mouseY), ids.get(i)).stream().map(Component::getVisualOrderText).toList(),
                    Optional.empty(), (screenWidth, screenHeight, cursorX, cursorY, width, height) ->
                            new org.joml.Vector2i(Math.max(4, Math.min(tile.x, screenWidth - width - 4)),
                                    tile.y + tile.size + height + 8 <= screenHeight ? tile.y + tile.size + 4 : Math.max(4, tile.y - height - 4)),
                    mouseX, mouseY, false, null);
            //#elseif MC >= 1.21.8
            //$$ if (over) graphics.setTooltipForNextFrame(Minecraft.getInstance().font,
            //$$         tooltip(entry.getServerData(), tile.action(mouseX, mouseY), ids.get(i)).stream().map(Component::getVisualOrderText).toList(),
            //$$         (screenWidth, screenHeight, cursorX, cursorY, width, height) ->
            //$$                 new org.joml.Vector2i(Math.max(4, Math.min(tile.x, screenWidth - width - 4)),
            //$$                         tile.y + tile.size + height + 8 <= screenHeight ? tile.y + tile.size + 4 : Math.max(4, tile.y - height - 4)),
            //$$         mouseX, mouseY, false);
            //#else
            //$$ if(over) screen.setTooltipForNextRenderPass(tooltip(entry.getServerData(),tile.action(mouseX,mouseY),ids.get(i)).stream().map(Component::getVisualOrderText).toList(),
            //$$     (screenWidth,screenHeight,cursorX,cursorY,width,height)->new org.joml.Vector2i(Math.max(4,Math.min(tile.x,screenWidth-width-4)),tile.y+tile.size+height+8<=screenHeight?tile.y+tile.size+4:Math.max(4,tile.y-height-4)),false);
            //#endif
        }
    }
    //#if MC >= 26.1
    private static void renderNativeIcon(GuiGraphicsExtractor graphics, ServerSelectionList.OnlineServerEntry entry, Tile tile, float delta) {
    //#else
    //$$ private static void renderNativeIcon(GuiGraphics graphics, ServerSelectionList.OnlineServerEntry entry, Tile tile, float delta) {
    //#endif
//#if MC >= 1.21.10
        int x = entry.getX(), y = entry.getY(), width = entry.getWidth(), height = entry.getHeight();
        int padX = entry.getContentX() - x, padY = entry.getContentY() - y;
        graphics.enableScissor(tile.x, tile.y, tile.x + ICON_SIZE, tile.y + ICON_SIZE);
        try {
            entry.setX(tile.x - padX); entry.setY(tile.y - padY); entry.setWidth(Math.max(160, width)); entry.setHeight(36);
            // This is the native render path that schedules each Ping once and updates its favicon.
            //#if MC >= 26.1
            entry.extractContent(graphics, -1, -1, false, delta);
            //#else
            //$$ entry.renderContent(graphics, -1, -1, false, delta);
            //#endif
        } finally {
            entry.setX(x); entry.setY(y); entry.setWidth(width); entry.setHeight(height); graphics.disableScissor();
        }
//#else
//$$         graphics.enableScissor(tile.x, tile.y, tile.x+ICON_SIZE, tile.y+ICON_SIZE);
//$$         try { entry.render(graphics,0,tile.y,tile.x,Math.max(160,ICON_SIZE),ICON_SIZE,-1,-1,false,delta); }
//$$         finally { graphics.disableScissor(); }
//#endif

    }
    //#if MC >= 26.1
    private static void renderCaption(GuiGraphicsExtractor g, Tile tile, ServerData data) {
    //#else
    //$$ private static void renderCaption(GuiGraphics g, Tile tile, ServerData data) {
    //#endif
        var font = Minecraft.getInstance().font;
        var lines = font.split(Component.literal(data.name), 42);
        int count = Math.min(2, lines.size()); float scale = 0.75F;
        g.fill(tile.x, tile.y + ICON_SIZE - count * 7 - 1, tile.x + ICON_SIZE, tile.y + ICON_SIZE, 0xb0000000);
        //#if MC >= 1.21.8
        g.pose().pushMatrix();
        //#else
        //$$ g.pose().pushPose();
        //#endif
        //#if MC >= 1.21.8
        g.pose().translate(tile.x + 1, tile.y + ICON_SIZE - count * 7); g.pose().scale(scale, scale);
        //#else
        //$$ g.pose().translate(tile.x+1,tile.y+ICON_SIZE-count*7,0); g.pose().scale(scale,scale,1);
        //#endif
        //#if MC >= 26.1
        for (int i = 0; i < count; i++) g.text(font, lines.get(i), 0, i * 9, 0xffffffff);
        //#else
        //$$ for (int i = 0; i < count; i++) g.drawString(font, lines.get(i), 0, i * 9, 0xffffffff);
        //#endif
        //#if MC >= 1.21.8
        g.pose().popMatrix();
        //#else
        //$$ g.pose().popPose();
        //#endif
        int color = switch (data.state()) { case SUCCESSFUL -> 0xff55ff55; case UNREACHABLE, INCOMPATIBLE -> 0xffff5555; default -> 0xffffff55; };
        String state = switch (data.state()) { case SUCCESSFUL -> "+"; case UNREACHABLE, INCOMPATIBLE -> "×"; default -> "…"; };
        g.fill(tile.x + 23, tile.y, tile.x + ICON_SIZE, tile.y + 9, 0xb0000000);
        //#if MC >= 26.1
        g.text(font, state, tile.x + 24, tile.y, color);
        //#else
        //$$ g.drawString(font, state, tile.x + 24, tile.y, color);
        //#endif
    }
    //#if MC >= 26.1
    private static void renderControls(GuiGraphicsExtractor g, Tile tile, String id, int mouseX, int mouseY) {
    //#else
    //$$ private static void renderControls(GuiGraphics g, Tile tile, String id, int mouseX, int mouseY) {
    //#endif
        g.fill(tile.x, tile.y, tile.x + ICON_SIZE, tile.y + ICON_SIZE, 0xa0000000);
        int action = tile.action(mouseX, mouseY);
        var manager = ServerFolderManager.getInstance();
        int controlY = tile.y + CONTROL_TOP;
        glyph(g, "◅", tile.x + 6, controlY, manager.canMoveServer(id, false), action == -1);
        glyph(g, "▻", tile.x + 26, controlY, manager.canMoveServer(id, true), action == 1);
        glyph(g, "▷", tile.x + 16, controlY, true, action == 0);
    }
    //#if MC >= 26.1
    private static void glyph(GuiGraphicsExtractor g, String symbol, int centerX, int y, boolean enabled, boolean hovered) {
    //#else
    //$$ private static void glyph(GuiGraphics g, String symbol, int centerX, int y, boolean enabled, boolean hovered) {
    //#endif
        var font = Minecraft.getInstance().font;
        int color = !enabled ? 0xff555555 : hovered ? 0xffffff55 : 0xffffffff;
        //#if MC >= 26.1
        g.text(font, symbol, centerX - font.width(symbol) / 2, y, color);
        //#else
        //$$ g.drawString(font, symbol, centerX - font.width(symbol) / 2, y, color);
        //#endif
    }
    public static List<Component> tooltip(ServerData data, int action, String id) {
        List<Component> lines = new ArrayList<>(); lines.add(Component.literal(data.name));
        if (BetterMultiplayerConfig.ADDRESS.getBooleanValue() && !CaptureDetectionManager.INSTANCE.privacy.hidden()) lines.add(Component.literal(data.ip));
        if (data.motd != null) lines.add(data.motd);
        //#if MC >= 26.1
        if (BetterMultiplayerConfig.LATENCY.getBooleanValue()) lines.add(ServerLatencyOverlay.label(data).text());
        //#else
        //$$ if (BetterMultiplayerConfig.LATENCY.getBooleanValue()) lines.add(ServerLatencyOverlay.label(data).text());
        //#endif
        String path = ServerFolderManager.getInstance().path(id); if (!path.isEmpty()) lines.add(Component.literal(path));
        lines.add(Component.translatable(action == -1 ? "halfmasa.multiplayer.compact.move_left" : action == 1 ? "halfmasa.multiplayer.compact.move_right" : action == 2 ? "halfmasa.multiplayer.compact.select" : "selectServer.select"));
        return lines;
    }
    //#if MC >= 1.21.10
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //#else
    //$$ @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
    //$$         var event=new ServerPointer(mouseX,mouseY,button,net.minecraft.client.gui.screens.Screen.hasShiftDown());
    //$$         long now=net.minecraft.Util.getMillis();
    //$$         boolean doubleClick=now-halfmasa$lastClick<net.minecraft.client.gui.components.events.GuiEventListener.DOUBLE_CLICK_THRESHOLD_MS; halfmasa$lastClick=now;
    //#endif
        for (int i = 0; i < ids.size(); i++) if (tile(i).contains(event.x(), event.y())) {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT && event.button() != InputConstants.MOUSE_BUTTON_RIGHT) return false;
            boolean sameServer = ids.get(i).equals(lastClickedId);
            selected = i; list.setSelected(this);
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                lastClickedId = null;
                Minecraft.getInstance().setScreenAndShow(new ServerFolderScreen(screen, ids.get(i), null)); return true;
            }
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            int action = tile(i).action(event.x(), event.y());
            if (action == 2) {
                lastClickedId = ids.get(i);
                if (doubleClick && sameServer) join();
            } else {
                lastClickedId = null;
                if (action != 0) move(action > 0); else join();
            }
            return true;
        }
        return false;
    }
    private void move(boolean right) {
        String id = selectedId();
        if (ServerFolderManager.getInstance().moveServer(id, right)) {
            ((ServerFolderWidget) list).halfmasa$rebuildFolders();
            for (var entry : list.children()) if (entry instanceof CompactServerRow row && row.ids.contains(id)) { row.select(id); list.setSelected(row); break; }
        }
    }
    //#if MC >= 1.21.10
    @Override public void join() { var entry = servers.get(selectedId()); if (entry != null) entry.join(); }
    //#else
    //$$ @Override public void join() { if(selectedEntry()!=null){list.setSelected(this);screen.joinSelectedServer();} }
    //#endif
    //#if MC >= 1.21.10
    @Override public boolean keyPressed(KeyEvent event) {
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
    //$$         var event=new ServerKey(keyCode,modifiers);
    //#endif
        if (event.isConfirmation()) { join(); return true; }
        //#if MC >= 26.3
        if (event.input() == ScanCodes.SCAN_LEFT || event.input() == ScanCodes.SCAN_RIGHT) {
            boolean right = event.input() == ScanCodes.SCAN_RIGHT;
        //#else
        //$$ if (event.input() == GLFW.GLFW_KEY_LEFT || event.input() == GLFW.GLFW_KEY_RIGHT) {
        //$$     boolean right = event.input() == GLFW.GLFW_KEY_RIGHT;
        //#endif
            if (event.hasShiftDown()) move(right); else selected = Math.max(0, Math.min(ids.size() - 1, selected + (right ? 1 : -1)));
            return true;
        }
        return false;
    }
    @Override public Component getNarration() { var entry = servers.get(selectedId()); return entry == null ? Component.empty() : entry.getNarration(); }
}
