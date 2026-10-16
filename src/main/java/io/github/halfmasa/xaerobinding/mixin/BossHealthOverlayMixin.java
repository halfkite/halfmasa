package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.halfmasa.xaerobinding.feature.HoldToShow;
import io.github.halfmasa.xaerobinding.feature.BossBarGrouper;
import io.github.halfmasa.xaerobinding.feature.BossBarHealth;
import io.github.halfmasa.xaerobinding.config.Configs;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
//#if MC >= 26.0
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin
{
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private java.util.Map<java.util.UUID, LerpingBossEvent> events;

    //#if MC >= 26.0
    private float halfmasa$scale(GuiGraphicsExtractor graphics)
    //#else
    //$$ private float halfmasa$scale(GuiGraphics graphics)
    //#endif
    {
        if (!Configs.BETTER_BOSS_BAR.getBooleanValue()) return 1;
        // Keep the bar inside narrow windows at large GUI scales.
        return Math.min(Configs.BOSS_BAR_SCALE.getIntegerValue() / 100.0F,
                Math.max(0.01F, (graphics.guiWidth() - 16) / 186.0F));
    }

    //#if MC >= 26.0
    @WrapMethod(method = "extractRenderState")
    //#else
    //$$ @WrapMethod(method = "render")
    //#endif
    //#if MC >= 26.0
    private void halfmasa$visibilityAndScale(GuiGraphicsExtractor graphics, Operation<Void> original)
    //#else
    //$$ private void halfmasa$visibilityAndScale(GuiGraphics graphics, Operation<Void> original)
    //#endif
    {
        if (!HoldToShow.bossVisible()) return;
        float scale = halfmasa$scale(graphics);
        if (scale == 1) { original.call(graphics); return; }
        var pose = graphics.pose();
        //#if MC >= 1.21.8
        pose.pushMatrix();
        float center = graphics.guiWidth() / 2.0F;
        pose.translate(center, 0).scale(scale).translate(-center, 0);
        try { original.call(graphics); } finally { pose.popMatrix(); }
        //#else
        //$$ pose.pushPose();
        //$$ float center = graphics.guiWidth() / 2.0F;
        //$$ pose.translate(center, 0, 0); pose.scale(scale, scale, 1); pose.translate(-center, 0, 0);
        //$$ try { original.call(graphics); } finally { pose.popPose(); }
        //#endif
    }

    //#if MC >= 26.0
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
    //#else
    //$$ @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
    //#endif
            //#if MC >= 26.0
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"), require = 1)
            //#else
            //$$ target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"), require = 1)
            //#endif
    //#if MC >= 26.0
    private int halfmasa$scaledHeight(int height, GuiGraphicsExtractor graphics)
    //#else
    //$$ private int halfmasa$scaledHeight(int height, GuiGraphics graphics)
    //#endif
    { return (int)(height / halfmasa$scale(graphics)); }

    //#if MC >= 26.0
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
    //#else
    //$$ @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
    //#endif
            target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"), require = 1)
    private Component halfmasa$healthLabel(Component name, @Local LerpingBossEvent event)
    { return BossBarHealth.label(event); }

    //#if MC >= 26.0
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //#endif
    //#if MC >= 26.0
    private void halfmasa$compactBossBars(GuiGraphicsExtractor graphics, CallbackInfo ci)
    //#else
    //$$ private void halfmasa$compactBossBars(GuiGraphics graphics, CallbackInfo ci)
    //#endif
    {
        if (!Configs.BETTER_BOSS_BAR.getBooleanValue() || !Configs.BOSS_BAR_COMPACT.getBooleanValue() || this.events.isEmpty()) return;
        //#if MC >= 1.21.8
        graphics.nextStratum();
        //#endif
        int width = Math.min(182, Math.max(1, graphics.guiWidth() - 16));
        int x = (graphics.guiWidth() - width) / 2, y = 4;
        for (var bar : BossBarGrouper.merge(this.events.values()))
        {
            if (y + this.minecraft.font.lineHeight + 4 > graphics.guiHeight() / (3 * halfmasa$scale(graphics))) break;
            String health = BossBarHealth.value(bar);
            String suffix = health.isEmpty() ? "" : "  " + health;
            var label = Component.empty();
            int nameWidth = Math.max(0, width - this.minecraft.font.width(suffix));
            var name = bar.getName();
            boolean clipped = this.minecraft.font.width(name) > nameWidth;
            this.minecraft.font.substrByWidth(name, Math.max(0, nameWidth - (clipped ? this.minecraft.font.width("…") : 0)))
                    .visit((style,text) -> { label.append(Component.literal(text).withStyle(style)); return java.util.Optional.empty(); }, net.minecraft.network.chat.Style.EMPTY);
            if (clipped) label.append("…");
            label.append(suffix);
            graphics.fill(x - 2, y - 1, x + width + 2, y + this.minecraft.font.lineHeight + 4, 0x88000000);
            //#if MC >= 26.0
            graphics.text(this.minecraft.font, label, x, y, 0xFFFFFFFF, true);
            //#else
            //$$ graphics.drawString(this.minecraft.font, label, x, y, 0xFFFFFFFF, true);
            //#endif
            int barY = y + this.minecraft.font.lineHeight;
            graphics.fill(x, barY, x + width, barY + 2, 0xAA333333);
            int color = switch (bar.getColor()) {
                case PINK -> 0xFFFF55FF; case BLUE -> 0xFF5555FF; case RED -> 0xFFFF5555;
                case GREEN -> 0xFF55FF55; case YELLOW -> 0xFFFFFF55; case PURPLE -> 0xFFAA00AA; case WHITE -> 0xFFFFFFFF;
            };
            float progress = bar.getProgress();
            if (Float.isFinite(progress)) graphics.fill(x, barY, x + Math.round(width * Math.max(0, Math.min(1, progress))), barY + 2, color);
            y += this.minecraft.font.lineHeight + 5;
        }
        ci.cancel();
    }
    @ModifyExpressionValue(
            //#if MC >= 26.0
            method = "extractRenderState",
            //#else
            //$$ method = "render",
            //#endif
            at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"),
            require = 1)
    private Collection<LerpingBossEvent> halfmasa$mergeBossBars(Collection<LerpingBossEvent> original)
    {
        return BossBarGrouper.merge(original);
    }
}
//#endif
