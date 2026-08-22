package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.Minecraft;
//#if MC >= 1.21.10
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
//#if MC >= 1.21.11
import net.minecraft.util.Util;
//#else
//$$ import net.minecraft.Util;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.gui.CustomSavesPathScreen;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import fi.dy.masa.malilib.config.ConfigManager;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin
{
    @Inject(method = "init", at = @At("TAIL"))
    private void halfmasa_addSavesPathButton(CallbackInfo ci)
    {
        Screen screen = (Screen) (Object) this;
        int defaultX = screen.width / 2 - 124;
        int defaultY = 23;
        int configuredX = Configs.CUSTOM_SAVES_BUTTON_X.getIntegerValue();
        int configuredY = Configs.CUSTOM_SAVES_BUTTON_Y.getIntegerValue();
        Button button = new FolderButton(
                configuredX < 0 ? defaultX : configuredX,
                configuredY < 0 ? defaultY : configuredY,
                screen.width,
                screen.height,
                pressed -> MinecraftClientCompat.setScreen(Minecraft.getInstance(), new CustomSavesPathScreen((SelectWorldScreen) (Object) this)),
                Component.translatable("halfmasa.custom_saves.label"));
        button.setTooltip(Tooltip.create(Component.translatable(
                "halfmasa.custom_saves.tooltip", CustomSavesPath.getCurrentPath())));
        ((ScreenAccessor) (Object) this).halfmasa$addRenderableWidget(button);
    }

    private static final class FolderButton extends Button
    {
        private static final long DRAG_DELAY_MS = 1000L;
        private final int screenWidth;
        private final int screenHeight;
        private boolean mousePressed;
        private boolean draggingPosition;
        private long pressStarted;
        private double dragOffsetX;
        private double dragOffsetY;

        private FolderButton(int x, int y, int screenWidth, int screenHeight, OnPress onPress, Component narration)
        {
            super(Mth.clamp(x, 0, screenWidth - 20), Mth.clamp(y, 0, screenHeight - 18),
                    20, 18, narration, onPress, DEFAULT_NARRATION);
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }

        //#if MC >= 1.21.10
        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick)
        {
            this.halfmasa$mouseClicked(event.x(), event.y(), event.button());
        }
        //#else
        //$$ @Override
        //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
        //$$ {
        //$$     return this.halfmasa$mouseClicked(mouseX, mouseY, button);
        //$$ }
        //#endif

        private boolean halfmasa$mouseClicked(double mouseX, double mouseY, int button)
        {
            if (button != 0 || !this.active || !this.visible || !this.isMouseOver(mouseX, mouseY))
            {
                return false;
            }

            this.mousePressed = true;
            this.draggingPosition = false;
            this.pressStarted = Util.getMillis();
            this.dragOffsetX = mouseX - this.getX();
            this.dragOffsetY = mouseY - this.getY();
            return true;
        }

        //#if MC >= 1.21.10
        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)
        {
            return this.halfmasa$mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
        }
        //#else
        //$$ @Override
        //$$ public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
        //$$ {
        //$$     return this.halfmasa$mouseDragged(mouseX, mouseY, button, dragX, dragY);
        //$$ }
        //#endif

        private boolean halfmasa$mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
        {
            if (!this.mousePressed || button != 0)
            {
                return false;
            }

            if (Util.getMillis() - this.pressStarted >= DRAG_DELAY_MS)
            {
                this.draggingPosition = true;
                this.setX(Mth.clamp((int) Math.round(mouseX - this.dragOffsetX), 0, this.screenWidth - this.getWidth()));
                this.setY(Mth.clamp((int) Math.round(mouseY - this.dragOffsetY), 0, this.screenHeight - this.getHeight()));
            }
            return true;
        }

        //#if MC >= 1.21.10
        @Override
        public void onRelease(MouseButtonEvent event)
        {
            this.halfmasa$mouseReleased(event.x(), event.y(), event);
        }
        //#else
        //$$ @Override
        //$$ public boolean mouseReleased(double mouseX, double mouseY, int button)
        //$$ {
        //$$     return this.halfmasa$mouseReleased(mouseX, mouseY, button);
        //$$ }
        //#endif

        //#if MC >= 1.21.10
        private boolean halfmasa$mouseReleased(double mouseX, double mouseY, InputWithModifiers input)
        //#else
        //$$ private boolean halfmasa$mouseReleased(double mouseX, double mouseY, int input)
        //#endif
        {
            //#if MC >= 1.21.10
            int button = input.input();
            //#else
            //$$ int button = input;
            //#endif
            if (!this.mousePressed || button != 0)
            {
                return false;
            }

            boolean heldLongEnough = Util.getMillis() - this.pressStarted >= DRAG_DELAY_MS;
            this.mousePressed = false;
            if (this.draggingPosition || heldLongEnough)
            {
                Configs.CUSTOM_SAVES_BUTTON_X.setIntegerValue(this.getX());
                Configs.CUSTOM_SAVES_BUTTON_Y.setIntegerValue(this.getY());
                ConfigManager.getInstance().onConfigsChanged(XaeroWorldBinding.MOD_ID);
                this.draggingPosition = false;
                return true;
            }

            if (this.isMouseOver(mouseX, mouseY))
            {
                //#if MC >= 1.21.10
                this.onPress(input);
                //#else
                //$$ this.onPress();
                //#endif
            }
            return true;
        }

        //#if MC >= 26.1
        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
        {
            this.extractDefaultSprite(graphics);
            this.halfmasa$renderFolder(graphics);
        }
        //#elseif MC >= 1.21.11
        //$$ @Override
        //$$ protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        //$$ {
        //$$     this.renderDefaultSprite(graphics);
        //$$     this.halfmasa$renderFolder(graphics);
        //$$ }
        //#else
        //$$ @Override
        //$$ public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        //$$ {
        //$$     super.renderWidget(graphics, mouseX, mouseY, partialTick);
        //$$     this.halfmasa$renderFolder(graphics);
        //$$ }
        //#endif

        //#if MC >= 26.1
        private void halfmasa$renderFolder(GuiGraphicsExtractor graphics)
        //#else
        //$$ private void halfmasa$renderFolder(GuiGraphics graphics)
        //#endif
        {
            int color = this.active ? 0xFFF2C94C : 0xFF777777;
            int shadow = this.active ? 0xFFB88724 : 0xFF4A4A4A;
            int x = this.getX() + 4;
            int y = this.getY() + 5;
            graphics.fill(x + 1, y, x + 7, y + 3, color);
            graphics.fill(x, y + 2, x + 13, y + 10, shadow);
            graphics.fill(x + 1, y + 3, x + 12, y + 9, color);
        }

        //#if MC < 1.21.11
        //$$ @Override
        //$$ public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int color) {}
        //#endif
    }
}
