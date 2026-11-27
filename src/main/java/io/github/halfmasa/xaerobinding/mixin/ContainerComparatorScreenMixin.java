package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.comparator.ContainerComparator;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Locale;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerComparatorScreenMixin extends Screen
{
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow @Final protected int imageWidth;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int titleLabelX;
    @Shadow protected int titleLabelY;
    @Unique private ItemStack halfmasa$comparatorIcon = ItemStack.EMPTY;

    protected ContainerComparatorScreenMixin(Component title) { super(title); }

    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void halfmasa$extractComparator(GuiGraphicsExtractor graphics, int mouseX,
                                            int mouseY, float delta, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("TAIL"))
    //$$ private void halfmasa$extractComparator(GuiGraphics graphics, int mouseX,
    //$$                                         int mouseY, float delta, CallbackInfo ci)
    //#endif
    {
        if (!Configs.CONTAINER_COMPARATOR_OUTPUT.getBooleanValue()) return;
        var result = ContainerComparator.calculate(this.menu, this.getTitle());
        if (result == null) return;

        boolean icon = Configs.CONTAINER_COMPARATOR_ICON.getBooleanValue();
        String value = Integer.toString(result.signal());
        int textWidth = this.font.width(value);
        int indicatorWidth = (icon ? 14 : 0) + textWidth;
        int x = this.leftPos + this.imageWidth - 8 - indicatorWidth;
        int y = this.topPos + this.titleLabelY;
        int titleEnd = this.leftPos + this.titleLabelX + this.font.width(this.getTitle());
        boolean outside = titleEnd + 5 > x;
        if (outside)
        {
            // Long or centered titles: move above the GUI rather than covering text.
            y = this.topPos - 13;
            if (y < 2) return;
        }
        if (x < 0 || x + indicatorWidth > this.width || y < 1 || y + 11 > this.height) return;

        if (outside) graphics.fill(x - 3, y - 2, x + indicatorWidth + 3, y + 11, 0xC0100010);
        if (icon)
        {
            if (halfmasa$comparatorIcon.isEmpty()) halfmasa$comparatorIcon = Items.COMPARATOR.getDefaultInstance();
            var pose = graphics.pose();
            //#if MC >= 1.21.8
            pose.pushMatrix();
            //#else
            //$$ pose.pushPose();
            //#endif
            try
            {
                //#if MC >= 1.21.8
                pose.translate(x, y - 2);
                pose.scale(0.75F, 0.75F);
                //#else
                //$$ pose.translate(x, y - 2, 0F);
                //$$ pose.scale(0.75F, 0.75F, 1F);
                //#endif
                //#if MC >= 26.1
                graphics.item(halfmasa$comparatorIcon, 0, 0);
                //#else
                //$$ graphics.renderItem(halfmasa$comparatorIcon, 0, 0);
                //#endif
            }
            //#if MC >= 1.21.8
            finally { pose.popMatrix(); }
            //#else
            //$$ finally { pose.popPose(); }
            //#endif
        }
        int color = outside ? 0xFFFFFFFF : 0xFF404040;
        if (Configs.CONTAINER_COMPARATOR_COLOR.getBooleanValue())
        {
            int red = 100 + (155 * result.signal()) / result.maximum();
            color = 0xFF000000 | (red << 16) | (outside ? 0x6060 : 0x2020);
        }
        //#if MC >= 26.1
        graphics.text(this.font, value, x + (icon ? 14 : 0), y, color, false);
        //#else
        //$$ graphics.drawString(this.font, value, x + (icon ? 14 : 0), y, color, false);
        //#endif

        if (Configs.CONTAINER_COMPARATOR_TOOLTIP.getBooleanValue() &&
                mouseX >= x && mouseX < x + indicatorWidth && mouseY >= y - 2 && mouseY < y + 11)
        {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("halfmasa.comparator.signal", result.signal(), result.maximum()));
            lines.add(Component.translatable("halfmasa.comparator.occupied", result.occupied(), result.slots()));
            lines.add(Component.translatable("halfmasa.comparator.fullness", String.format(Locale.ROOT, "%.2f", result.fullness() * 100)));
            if (result.maximum() == 9)
            {
                lines.add(Component.translatable("halfmasa.comparator.disabled", result.disabled()));
                lines.add(Component.translatable("halfmasa.comparator.effective", result.effective()));
            }
            else if (result.nextItems() >= 0)
                lines.add(Component.translatable("halfmasa.comparator.next", result.nextItems()));
            else lines.add(Component.translatable("halfmasa.comparator.max"));
            lines.add(Component.translatable("halfmasa.comparator.estimate"));
            //#if MC >= 1.21.11
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            //#elseif MC >= 1.21.8
            //$$ graphics.setTooltipForNextFrame(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            //#else
            //$$ graphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            //#endif
        }
    }
}
//#else
//$$ @Mixin(AbstractContainerScreen.class)
//$$ public abstract class ContainerComparatorScreenMixin {}
//#endif
