package io.github.halfmasa.xaerobinding.gui;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#if MC >= 1.21.10
//$$ import net.minecraft.client.input.KeyEvent;
//$$ import net.minecraft.client.input.MouseButtonEvent;
//#endif
//#endif

import fi.dy.masa.malilib.gui.GuiBase;

import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC >= 26.3
import io.github.halfmasa.xaerobinding.config.KeybindSelectionLayout;
//#endif
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;

public final class KeybindPieScreen extends GuiBase
{
    private InputConstants.Key conflictedKey;
    //#if MC >= 26.3
    private List<KeybindPieManager.PieAction> conflicts;
    //#else
    //$$ private final List<KeyMapping> conflicts;
    //#endif
    private int centerX;
    private int centerY;
    private int selected = -1;
    private int ticks;
    private int selectionClickButton = -1;
    //#if MC >= 26.3
    private int listScroll;
    private ConflictSelectionAppearance.ListLayout renderedListLayout;
    //#endif

    //#if MC >= 26.3
    public KeybindPieScreen(InputConstants.Key conflictedKey, List<KeybindPieManager.PieAction> conflicts)
    //#else
    //$$ public KeybindPieScreen(InputConstants.Key conflictedKey, List<KeyMapping> conflicts)
    //#endif
    {
        this.conflictedKey = conflictedKey;
        this.conflicts = List.copyOf(conflicts);
        this.setTitle("");
    }

    //#if MC >= 26.3
    public void updateSelection(InputConstants.Key key, List<KeybindPieManager.PieAction> actions)
    {
        this.conflictedKey = key;
        this.conflicts = List.copyOf(actions);
        this.selected = -1;
        this.selectionClickButton = -1;
        this.listScroll = 0;
        this.renderedListLayout = null;
        this.ticks = 0;
    }
    //#endif

    @Override
    public void initGui()
    {
        super.initGui();
        this.centerX = this.getScreenWidth() / 2;
        this.centerY = this.getScreenHeight() / 2;
        //#if MC >= 26.3
        this.renderedListLayout = null;
        //#endif
    }

    @Override
    public void tick()
    {
        this.ticks++;
    }

    //#if MC >= 1.21.11
    @Override
    protected void drawScreenBackground(GuiContext graphics, int mouseX, int mouseY)
    //#else
    //$$ @Override
    //$$ protected void drawScreenBackground(GuiGraphics graphics, int mouseX, int mouseY)
    //#endif
    {
        //#if MC < 26.3
        //$$ if (Configs.KEYBIND_BLUR_BACKGROUND.getBooleanValue() ||
        //$$     Configs.KEYBIND_DARKEN_BACKGROUND.getBooleanValue())
        //$$ {
        //$$     super.drawScreenBackground(graphics, mouseX, mouseY);
        //$$ }
        //#endif
        if (Configs.KEYBIND_DARKEN_BACKGROUND.getBooleanValue())
        {
            graphics.fill(0, 0, this.getScreenWidth(), this.getScreenHeight(), 0x60000000);
        }
    }

    //#if MC >= 1.21.11
    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        //#if MC >= 26.3
        if (this.listMode())
        {
            this.drawList(graphics, mouseX, mouseY, partialTick);
            return;
        }
        //#endif
        int count = this.conflicts.size();
        double radius = Math.min(this.centerX, this.centerY) * Configs.KEYBIND_SCALE.getDoubleValue();
        //#if MC >= 26.3
        radius = Math.max(32.0D, radius);
        //#else
        //$$ radius = Math.max(32.0D, radius - Configs.KEYBIND_MARGIN.getIntegerValue());
        //#endif
        double cancelRadius = radius * Configs.KEYBIND_CANCEL_ZONE.getDoubleValue();
        double mouseDx = mouseX - this.centerX;
        double mouseDy = mouseY - this.centerY;
        double mouseDistance = Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
        double mouseAngle = normalizedAngle(Math.atan2(mouseDy, mouseDx));
        this.selected = mouseDistance <= cancelRadius || mouseDistance > radius * Configs.KEYBIND_EXPANSION.getDoubleValue()
                ? -1
                : Math.min(count - 1, (int) (mouseAngle / (Math.PI * 2.0D / count)));

        double animation = Configs.KEYBIND_ANIMATE.getBooleanValue()
                ? Math.min(1.0D, (this.ticks + partialTick) / 6.0D)
                : 1.0D;
        int outerRadius = (int) Math.ceil(radius * Configs.KEYBIND_EXPANSION.getDoubleValue() * animation);
        int step = Math.max(1, 60 / Math.max(12, Configs.KEYBIND_CIRCLE_VERTICES.getIntegerValue()));
        int[] colors = new int[count];
        for (int index = 0; index < count; index++)
        {
            colors[index] = sectorColor(index);
        }

        for (int dy = -outerRadius; dy <= outerRadius; dy += step)
        {
            int runStart = -outerRadius;
            int runColor = 0;
            for (int dx = -outerRadius; dx <= outerRadius; dx += step)
            {
                double distance = Math.sqrt((dx + 0.5D) * (dx + 0.5D) + (dy + 0.5D) * (dy + 0.5D));
                int color = 0;
                if (distance < cancelRadius * animation)
                {
                    if (color != runColor)
                    {
                        fillRun(graphics, runStart, dx, dy, step, runColor);
                        runStart = dx;
                        runColor = color;
                    }
                    continue;
                }
                int sector = Math.min(count - 1,
                        (int) (normalizedAngle(Math.atan2(dy, dx)) / (Math.PI * 2.0D / count)));
                double sectorRadius = radius * animation;
                if (sector == this.selected)
                {
                    sectorRadius *= Configs.KEYBIND_EXPANSION.getDoubleValue();
                }
                if (distance > sectorRadius)
                {
                    if (color != runColor)
                    {
                        fillRun(graphics, runStart, dx, dy, step, runColor);
                        runStart = dx;
                        runColor = color;
                    }
                    continue;
                }

                color = colors[sector];
                if (step == 1)
                {
                    double coverage = Math.min(
                            distance - cancelRadius * animation + 0.5D,
                            sectorRadius - distance + 0.5D);
                    color = applyCoverage(color, Math.max(0.0D, Math.min(1.0D, coverage)));
                }
                if (color != runColor)
                {
                    fillRun(graphics, runStart, dx, dy, step, runColor);
                    runStart = dx;
                    runColor = color;
                }
            }
            fillRun(graphics, runStart, outerRadius + step, dy, step, runColor);
        }

        double sectorAngle = Math.PI * 2.0D / count;
        for (int index = 0; index < count; index++)
        {
            double angle = (index + 0.5D) * sectorAngle;
            double labelRadius = radius * 0.72D;
            int x = this.centerX + (int) (Math.cos(angle) * labelRadius);
            int y = this.centerY + (int) (Math.sin(angle) * labelRadius) - 4;
            //#if MC >= 26.3
            String label = this.conflicts.get(index).displayName();
            //#else
            //$$ String label = KeybindCustomizationStore.getInstance().displayName(this.conflicts.get(index));
            //#endif
            int maxWidth = Math.max(60, (int) (radius * 0.8D));
            label = this.mc.font.plainSubstrByWidth(label, maxWidth);
            int width = this.mc.font.width(label);
            //#if MC >= 26.3
            graphics.drawString(this.mc.font, label, x - width / 2, y,
                    ConflictSelectionAppearance.Palette.textColor(colors[index]), false);
            //#else
            //$$ this.drawString(
            //$$         graphics,
            //$$         label,
            //$$         x - width / 2,
            //$$         y,
            //$$         index == this.selected ? 0xFFFFFFFF : 0xFFE0E0E0);
            //#endif
        }
    }

    //#if MC >= 26.3
    private boolean listMode()
    {
        return Configs.KEYBIND_SELECTION_LAYOUT.getOptionListValue() == KeybindSelectionLayout.LIST;
    }

    private ConflictSelectionAppearance.Palette palette()
    {
        return new ConflictSelectionAppearance.Palette(
                Configs.KEYBIND_MENU_COLOR.getIntegerValue(),
                Configs.KEYBIND_SELECTED_COLOR.getIntegerValue(),
                Configs.KEYBIND_HIGHLIGHT_COLOR.getIntegerValue(),
                Configs.KEYBIND_ALPHA.getIntegerValue(),
                Configs.KEYBIND_GRADATION.getBooleanValue(),
                Configs.KEYBIND_ALTERNATE_LIGHTEN.getIntegerValue());
    }

    private double openingProgress(float partialTick)
    {
        return Configs.KEYBIND_ANIMATE.getBooleanValue()
                ? Math.min(1.0D, (this.ticks + partialTick) / 6.0D) : 1.0D;
    }

    private ConflictSelectionAppearance.ListLayout listLayout(double animation)
    {
        return ConflictSelectionAppearance.ListLayout.create(this.getScreenWidth(), this.getScreenHeight(),
                this.conflicts.size(), Configs.KEYBIND_SCALE.getDoubleValue(),
                Configs.KEYBIND_EXPANSION.getDoubleValue(), animation);
    }

    private int visibleListRows()
    {
        return this.listLayout(1.0D).visibleRows();
    }

    private void drawList(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    {
        double progress = this.openingProgress(partialTick);
        if (progress <= 0.0D)
        {
            this.selected = -1;
            this.renderedListLayout = null;
            return;
        }
        var layout = this.listLayout(progress);
        this.renderedListLayout = layout;
        int visibleRows = layout.visibleRows();
        this.listScroll = Math.clamp(this.listScroll, 0, this.conflicts.size() - visibleRows);
        this.selected = layout.indexAt(mouseX, mouseY, this.listScroll, this.selected);
        int left = layout.left();
        int top = layout.top();
        int right = left + layout.width();
        int bottom = top + layout.height();
        var palette = this.palette();
        int border = palette.withAlpha(palette.base());
        // Keep the center empty: a fixed opaque backing would override the opacity slider.
        graphics.fill(left - 2, top - 2, right + 2, top, border);
        graphics.fill(left - 2, bottom, right + 2, bottom + 2, border);
        graphics.fill(left - 2, top, left, bottom, border);
        graphics.fill(right, top, right + 2, bottom, border);
        int inset = Math.max(1, (int) Math.round(10 * layout.textScale()));
        int scrollWidth = this.conflicts.size() > visibleRows ? Math.max(2, (int) Math.round(8 * layout.textScale())) : 0;
        int textWidth = Math.max(1, (int) ((layout.width() - 2 * inset - scrollWidth) / layout.textScale()));
        for (int row = 0; row < visibleRows; row++)
        {
            int index = this.listScroll + row;
            int y = layout.rowTop(row);
            int extra = index == this.selected ? layout.expansion() : 0;
            int color = this.sectorColor(index);
            graphics.fill(left - extra, y, right + extra - scrollWidth, y + layout.rowHeight(), color);
            String label = this.mc.font.plainSubstrByWidth(this.conflicts.get(index).displayName(), textWidth);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) (left + inset), (float) (y + layout.rowHeight() / 2));
            graphics.pose().scale(layout.textScale(), layout.textScale());
            graphics.drawString(this.mc.font, label, 0, -4,
                    ConflictSelectionAppearance.Palette.textColor(color), false);
            graphics.pose().popMatrix();
        }
        if (this.conflicts.size() > visibleRows)
        {
            int trackTop = layout.rowTop(0);
            int trackHeight = layout.height() - 2 * layout.padding();
            int thumbHeight = Math.min(trackHeight, Math.max(3, trackHeight * visibleRows / this.conflicts.size()));
            int thumbY = trackTop + (trackHeight - thumbHeight) * this.listScroll /
                    (this.conflicts.size() - visibleRows);
            graphics.fill(right - scrollWidth + 1, trackTop, right, trackTop + trackHeight, border);
            graphics.fill(right - scrollWidth + 1, thumbY, right, thumbY + thumbHeight,
                    palette.withAlpha(palette.highlight()));
        }
    }

    private int listIndexAt(double mouseX, double mouseY)
    {
        var layout = this.renderedListLayout;
        if (layout == null)
        {
            double progress = this.openingProgress(0.0F);
            if (progress <= 0.0D) return -1;
            layout = this.listLayout(progress);
        }
        return layout.indexAt(mouseX, mouseY, this.listScroll, this.selected);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (this.listMode() && verticalAmount != 0.0D)
        {
            int visibleRows = this.visibleListRows();
            int step = Math.max(1, (int) Math.abs(verticalAmount));
            this.listScroll = Math.max(0, Math.min(this.conflicts.size() - visibleRows,
                    this.listScroll + (verticalAmount > 0 ? -step : step)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    //#endif

    private int sectorColor(int sector)
    {
        //#if MC >= 26.3
        var action = this.conflicts.get(sector);
        var custom = KeybindCustomizationStore.getInstance().get(action.mapping() == null
                ? action.hotkey().getName() : action.mapping().getName());
        return this.palette().color(custom.sectorColor, sector, sector == this.selected,
                this.selectionClickButton >= 0);
        //#else
        //$$ KeybindCustomizationStore.Entry custom =
        //$$         KeybindCustomizationStore.getInstance().get(this.conflicts.get(sector));
        //$$ int rgb = custom.sectorColor == null
    //$$             ? Configs.KEYBIND_MENU_COLOR.getIntegerValue() & 0xFFFFFF
    //$$             : custom.sectorColor & 0xFFFFFF;
    //$$     if (sector == this.selected)
    //$$     {
    //$$         rgb = this.selectionClickButton >= 0
    //$$                 ? Configs.KEYBIND_HIGHLIGHT_COLOR.getIntegerValue() & 0xFFFFFF
    //$$                 : Configs.KEYBIND_SELECTED_COLOR.getIntegerValue() & 0xFFFFFF;
    //$$     }
    //$$     else if (Configs.KEYBIND_GRADATION.getBooleanValue() && (sector & 1) == 1)
    //$$     {
    //$$         rgb = lighten(rgb, Configs.KEYBIND_ALTERNATE_LIGHTEN.getIntegerValue());
    //$$     }
    //$$     int alpha = Configs.KEYBIND_BLEND.getBooleanValue()
    //$$             ? Configs.KEYBIND_ALPHA.getIntegerValue()
    //$$             : 255;
    //$$     return (alpha << 24) | rgb;
        //#endif
    }

    private static int lighten(int color, int amount)
    {
        int red = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int green = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int blue = Math.min(255, (color & 0xFF) + amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static int applyCoverage(int color, double coverage)
    {
        int alpha = (color >>> 24) & 0xFF;
        return ((int) Math.round(alpha * coverage) << 24) | (color & 0xFFFFFF);
    }

    //#if MC >= 1.21.11
    private void fillRun(GuiContext graphics, int startX, int endX, int y, int step, int color)
    //#else
    //$$ private void fillRun(GuiGraphics graphics, int startX, int endX, int y, int step, int color)
    //#endif
    {
        if ((color >>> 24) != 0 && endX > startX)
        {
            graphics.fill(
                    this.centerX + startX,
                    this.centerY + y,
                    this.centerX + endX,
                    this.centerY + y + step,
                    color);
        }
    }

    private static double normalizedAngle(double angle)
    {
        return (angle + Math.PI * 2.0D) % (Math.PI * 2.0D);
    }

    //#if MC >= 26.2
    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (InputConstants.getKey(event).equals(this.conflictedKey))
        {
            this.finish(false);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        //#if MC >= 26.3
        if (this.listMode()) this.selected = this.listIndexAt(event.x(), event.y());
        //#endif
        if (this.selected >= 0 &&
            !(this.conflictedKey.getType() == InputConstants.Type.MOUSE && event.button() == this.conflictedKey.getValue()))
        {
            this.selectionClickButton = event.button();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        //#if MC >= 26.3
        if (this.listMode()) this.selected = this.listIndexAt(event.x(), event.y());
        //#endif
        if (this.conflictedKey.getType() == InputConstants.Type.MOUSE && event.button() == this.conflictedKey.getValue())
        {
            this.finish(false);
            return true;
        }
        if (event.button() == this.selectionClickButton)
        {
            this.finish(true);
            return true;
        }
        return super.mouseReleased(event);
    }
    //#else
    //#if MC >= 1.21.10
    //$$ @Override
    //$$ public boolean keyReleased(KeyEvent event)
    //$$ {
    //$$     if (InputConstants.getKey(event).equals(this.conflictedKey))
    //$$     {
    //$$         this.finish(false);
    //$$         return true;
    //$$     }
    //$$     return super.keyReleased(event);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    //$$ {
    //$$     if (this.selected >= 0 &&
    //$$         !(this.conflictedKey.getType() == InputConstants.Type.MOUSE && event.button() == this.conflictedKey.getValue()))
    //$$     {
    //$$         this.selectionClickButton = event.button();
    //$$         return true;
    //$$     }
    //$$     return super.mouseClicked(event, doubleClick);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseReleased(MouseButtonEvent event)
    //$$ {
    //$$     if (this.conflictedKey.getType() == InputConstants.Type.MOUSE && event.button() == this.conflictedKey.getValue())
    //$$     {
    //$$         this.finish(false);
    //$$         return true;
    //$$     }
    //$$     if (event.button() == this.selectionClickButton)
    //$$     {
    //$$         this.finish(true);
    //$$         return true;
    //$$     }
    //$$     return super.mouseReleased(event);
    //$$ }
    //#else
    //$$ @Override
    //$$ public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    //$$ {
    //$$     if (this.conflictedKey.getType() == InputConstants.Type.KEYSYM && keyCode == this.conflictedKey.getValue())
    //$$     {
    //$$         this.finish(false);
    //$$         return true;
    //$$     }
    //$$     return super.keyReleased(keyCode, scanCode, modifiers);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
    //$$ {
    //$$     if (this.selected >= 0 &&
    //$$         !(this.conflictedKey.getType() == InputConstants.Type.MOUSE && button == this.conflictedKey.getValue()))
    //$$     {
    //$$         this.selectionClickButton = button;
    //$$         return true;
    //$$     }
    //$$     return super.mouseClicked(mouseX, mouseY, button);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseReleased(double mouseX, double mouseY, int button)
    //$$ {
    //$$     if (this.conflictedKey.getType() == InputConstants.Type.MOUSE && button == this.conflictedKey.getValue())
    //$$     {
    //$$         this.finish(false);
    //$$         return true;
    //$$     }
    //$$     if (button == this.selectionClickButton)
    //$$     {
    //$$         this.finish(true);
    //$$         return true;
    //$$     }
    //$$     return super.mouseReleased(mouseX, mouseY, button);
    //$$ }
    //#endif
    //#endif

    private void finish(boolean clickHold)
    {
        //#if MC >= 26.3
        KeybindPieManager.PieAction action = this.selected >= 0 && this.selected < this.conflicts.size()
                ? this.conflicts.get(this.selected) : null;
        KeybindPieManager.getInstance().completeSelection(action, clickHold);
        //#else
        //$$ KeyMapping mapping = this.selected >= 0 && this.selected < this.conflicts.size()
        //$$         ? this.conflicts.get(this.selected) : null;
        //$$ KeybindPieManager.getInstance().completeSelection(mapping, clickHold);
        //#endif
    }

    @Override
    public void onClose()
    {
        KeybindPieManager.getInstance().cancel(this);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
