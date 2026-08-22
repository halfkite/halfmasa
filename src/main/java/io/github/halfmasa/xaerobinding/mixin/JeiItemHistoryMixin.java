//#if MC <= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;
import mezz.jei.common.util.ImmutableRect2i;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false)
public abstract class JeiItemHistoryMixin
{
    @Dynamic
    @ModifyArg(
            method = "updateBounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;Ljava/util/Set;Lmezz/jei/common/util/ImmutablePoint2i;)V"),
            index = 0,
            require = 0,
            remap = false)
    private ImmutableRect2i halfmasa_reserveJeiHistoryRows(ImmutableRect2i area)
    {
        return (ImmutableRect2i) ItemManagerHistoryOverlay.reserveJeiArea(this, area);
    }

    @Dynamic
    @ModifyArg(
            method = "updateBounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;Ljava/util/Set;Lmezz/jei/common/util/ImmutablePoint2i;)V"),
            index = 1,
            require = 0,
            remap = false)
    private Set<ImmutableRect2i> halfmasa_excludeJeiHistoryArea(Set<ImmutableRect2i> exclusions)
    {
        int[] area = ItemManagerHistoryOverlay.getJeiHistoryExclusionArea();
        if (area == null)
        {
            return exclusions;
        }
        Set<ImmutableRect2i> combined = new HashSet<>(exclusions);
        combined.add(new ImmutableRect2i(area[0], area[1], area[2], area[3]));
        return combined;
    }

    @Dynamic
    @ModifyArg(
            method = "updateBounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/input/GuiTextFieldFilter;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;)V"),
            index = 0,
            require = 0,
            remap = false)
    private ImmutableRect2i halfmasa_captureJeiSearchArea(ImmutableRect2i area)
    {
        ItemManagerHistoryOverlay.updateJeiSearchArea(this, area);
        return area;
    }

    @Dynamic
    @Inject(method = "drawForeground", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_renderJeiHistory(
            Minecraft minecraft,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks,
            CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.renderJei(this, graphics, mouseX, mouseY);
    }

    @Dynamic
    @Inject(method = "drawScreen", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_renderJeiHistoryLegacy(
            Minecraft minecraft,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks,
            CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.renderJei(this, graphics, mouseX, mouseY);
    }
}
//#endif
