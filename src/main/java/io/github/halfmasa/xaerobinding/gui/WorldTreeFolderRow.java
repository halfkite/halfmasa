package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.nio.file.Path;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.Component;

public final class WorldTreeFolderRow extends WorldSelectionList.Entry
{
    private final Path root;
    private final String folder;
    private final Component name;
    private final String detail;
    private final boolean expanded;
    private final Runnable open;

    public WorldTreeFolderRow(Path root, String folder, Component name, String detail, boolean expanded, Runnable open)
    {
        this.root = root;
        this.folder = folder;
        this.name = name;
        this.detail = detail;
        this.expanded = expanded;
        this.open = open;
    }

    public Path root() { return this.root; }
    public String folder() { return this.folder; }

    //#if MC >= 26.1
    @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta)
//#elseif MC >= 1.21.10
    //$$ @Override public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta)
//#else
    //$$ @Override public void render(GuiGraphics graphics, int index, int rowY, int rowX, int rowWidth,
    //$$         int rowHeight, int mouseX, int mouseY, boolean hovered, float delta)
    //#endif
    {
        var font = Minecraft.getInstance().font;
        //#if MC >= 1.21.10
        int left = getContentX() + (this.folder == null ? 6 : 18);
        int width = Math.max(1, getContentRight() - left - 3);
        int top = getContentY();
        //#else
        //$$ int left = rowX + (this.folder == null ? 6 : 18);
        //$$ int width = Math.max(1, rowX + rowWidth - left - 3);
        //$$ int top = rowY;
        //#endif
        String heading = (this.expanded ? "▾ " : "▸ ") + this.name.getString();
        //#if MC >= 26.1
        graphics.text(font, font.plainSubstrByWidth(heading, width), left, top + 4,
        //#else
        //$$ graphics.drawString(font, font.plainSubstrByWidth(heading, width), left, top + 4,
        //#endif
                this.open == null ? 0xFFAAAAAA : 0xFFFFD080);
        if (!this.detail.isBlank())
        {
            int headingWidth = font.width(heading) + 8;
            boolean beside = width - headingWidth >= 68;
            int indent = beside ? headingWidth : 10;
            //#if MC >= 26.1
            graphics.text(font, font.plainSubstrByWidth(this.detail, Math.max(1, width - indent)),
            //#else
            //$$ graphics.drawString(font, font.plainSubstrByWidth(this.detail, Math.max(1, width - indent)),
            //#endif
                    left + indent, top + (beside ? 4 : 17), 0xFFBBBBBB);
            //#if MC >= 1.21.8
            if (hovered) graphics.setTooltipForNextFrame(Component.literal(this.detail), mouseX, mouseY);
            //#else
            //$$ if (hovered) graphics.renderTooltip(font, Component.literal(this.detail), mouseX, mouseY);
            //#endif
        }
    }

    //#if MC >= 1.21.10
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || this.open == null) return false;
    //#else
    //$$ @Override public boolean mouseClicked(double x, double y, int button) {
    //$$     if (button != InputConstants.MOUSE_BUTTON_LEFT || this.open == null) return false;
    //#endif
        this.open.run();
        return true;
    }

    //#if MC >= 1.21.10
    @Override public boolean keyPressed(KeyEvent event)
    {
        if (!event.isConfirmation() || this.open == null) return false;
    //#else
    //$$ @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
    //$$     if ((key != org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && key != org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
    //$$             || this.open == null) return false;
    //#endif
        this.open.run();
        return true;
    }

    @Override public Component getNarration() { return this.name; }
}
//#endif
