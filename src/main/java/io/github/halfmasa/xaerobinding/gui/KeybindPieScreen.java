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
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;

public final class KeybindPieScreen extends GuiBase
{
    private final InputConstants.Key conflictedKey;
    private final List<KeyMapping> conflicts;
    private int centerX;
    private int centerY;
    private int selected = -1;
    private int ticks;
    private int selectionClickButton = -1;

    public KeybindPieScreen(InputConstants.Key conflictedKey, List<KeyMapping> conflicts)
    {
        this.conflictedKey = conflictedKey;
        this.conflicts = List.copyOf(conflicts);
        this.setTitle("");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.centerX = this.getScreenWidth() / 2;
        this.centerY = this.getScreenHeight() / 2;
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
        if (Configs.KEYBIND_BLUR_BACKGROUND.getBooleanValue() ||
            Configs.KEYBIND_DARKEN_BACKGROUND.getBooleanValue())
        {
            super.drawScreenBackground(graphics, mouseX, mouseY);
        }
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
        int count = this.conflicts.size();
        double radius = Math.min(this.centerX, this.centerY) * Configs.KEYBIND_SCALE.getDoubleValue();
        radius = Math.max(32.0D, radius - Configs.KEYBIND_MARGIN.getIntegerValue());
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
        int step = Math.max(1, 180 / Math.max(12, Configs.KEYBIND_CIRCLE_VERTICES.getIntegerValue()));

        for (int dy = -outerRadius; dy <= outerRadius; dy += step)
        {
            for (int dx = -outerRadius; dx <= outerRadius; dx += step)
            {
                double distance = Math.sqrt((double) dx * dx + (double) dy * dy);
                if (distance < cancelRadius * animation)
                {
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
                    continue;
                }

                int color = sectorColor(sector);
                graphics.fill(
                        this.centerX + dx,
                        this.centerY + dy,
                        this.centerX + dx + step,
                        this.centerY + dy + step,
                        color);
            }
        }

        double sectorAngle = Math.PI * 2.0D / count;
        for (int index = 0; index < count; index++)
        {
            double angle = (index + 0.5D) * sectorAngle;
            double labelRadius = radius * 0.72D;
            int x = this.centerX + (int) (Math.cos(angle) * labelRadius);
            int y = this.centerY + (int) (Math.sin(angle) * labelRadius) - 4;
            String label = KeybindCustomizationStore.getInstance().displayName(this.conflicts.get(index));
            int maxWidth = Math.max(60, (int) (radius * 0.8D));
            label = this.mc.font.plainSubstrByWidth(label, maxWidth);
            int width = this.mc.font.width(label);
            this.drawString(
                    graphics,
                    label,
                    x - width / 2,
                    y,
                    index == this.selected ? 0xFFFFFFFF : 0xFFE0E0E0);
        }
    }

    private int sectorColor(int sector)
    {
        KeybindCustomizationStore.Entry custom =
                KeybindCustomizationStore.getInstance().get(this.conflicts.get(sector));
        int rgb = custom.sectorColor == null
                ? Configs.KEYBIND_MENU_COLOR.getIntegerValue() & 0xFFFFFF
                : custom.sectorColor & 0xFFFFFF;
        if (sector == this.selected)
        {
            rgb = this.selectionClickButton >= 0
                    ? Configs.KEYBIND_HIGHLIGHT_COLOR.getIntegerValue() & 0xFFFFFF
                    : Configs.KEYBIND_SELECTED_COLOR.getIntegerValue() & 0xFFFFFF;
        }
        else if (Configs.KEYBIND_GRADATION.getBooleanValue() && (sector & 1) == 1)
        {
            rgb = lighten(rgb, Configs.KEYBIND_ALTERNATE_LIGHTEN.getIntegerValue());
        }
        int alpha = Configs.KEYBIND_BLEND.getBooleanValue()
                ? Configs.KEYBIND_ALPHA.getIntegerValue()
                : 255;
        return (alpha << 24) | rgb;
    }

    private static int lighten(int color, int amount)
    {
        int red = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int green = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int blue = Math.min(255, (color & 0xFF) + amount);
        return (red << 16) | (green << 8) | blue;
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
        KeyMapping mapping = this.selected >= 0 && this.selected < this.conflicts.size()
                ? this.conflicts.get(this.selected)
                : null;
        KeybindPieManager.getInstance().completeSelection(mapping, clickHold);
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
