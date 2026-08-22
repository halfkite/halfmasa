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
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.common.util.ImmutableRect2i;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
public abstract class JeiItemHistoryBookmarkMixin
{
    @Dynamic
    @ModifyArg(
            method = "updateBounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;Ljava/util/Set;Lmezz/jei/common/util/ImmutablePoint2i;)V"),
            index = 1,
            require = 0,
            remap = false)
    private Set<ImmutableRect2i> halfmasa_excludeHistoryFromJeiBookmarks(
            Set<ImmutableRect2i> exclusions)
    {
        return halfmasa_addHistoryExclusion(exclusions);
    }

    @Dynamic
    @ModifyArg(
            method = "updateBounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/overlay/bookmarks/history/LookupHistoryOverlay;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;Ljava/util/Set;Lmezz/jei/common/util/ImmutablePoint2i;)V"),
            index = 1,
            require = 0,
            remap = false)
    private Set<ImmutableRect2i> halfmasa_excludeHistoryFromJeiLookupHistory(
            Set<ImmutableRect2i> exclusions)
    {
        return halfmasa_addHistoryExclusion(exclusions);
    }

    @Dynamic
    @Inject(method = "updateBounds", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_updateJeiBookmarkAreas(IGuiProperties properties, CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.updateJeiBookmarkAreas(this);
    }

    @Dynamic
    @Inject(method = "drawScreen", at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa_syncJeiBookmarkAreas(
            Minecraft minecraft,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks,
            CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.updateJeiBookmarkAreas(this);
    }

    private static Set<ImmutableRect2i> halfmasa_addHistoryExclusion(
            Set<ImmutableRect2i> exclusions)
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
}
//#endif
