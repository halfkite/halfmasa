//#if MC <= 1.21.1
//$$ package io.github.halfmasa.xaerobinding.mixin;

//$$ import java.util.HashSet;
//$$ import java.util.Set;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.GuiGraphics;

//$$ import org.spongepowered.asm.mixin.Dynamic;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//$$ import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;
//$$ import mezz.jei.common.util.ImmutablePoint2i;
//$$ import mezz.jei.common.util.ImmutableRect2i;
//$$ import mezz.jei.gui.overlay.IngredientGridWithNavigation;

//$$ @Pseudo
//$$ @Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false)
//$$ public abstract class JeiItemHistoryMixin
//$$ {
    //$$ @Dynamic
    //$$ @WrapOperation(
            //$$ method = "updateBounds",
            //$$ at = @At(
                    //$$ value = "INVOKE",
                    //$$ target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;Ljava/util/Set;Lmezz/jei/common/util/ImmutablePoint2i;)V"),
            //$$ require = 0,
            //$$ remap = false)
    //$$ private void halfmasa_layoutJeiGrid(
            //$$ IngredientGridWithNavigation grid, ImmutableRect2i bounds, Set<ImmutableRect2i> exclusions,
            //$$ ImmutablePoint2i anchor, Operation<Void> updateBounds)
    //$$ {
        //$$ ItemManagerHistoryOverlay.prepareJeiLayout(this, bounds);
        //$$ int[] area = ItemManagerHistoryOverlay.getJeiHistoryExclusionArea();
        //$$ if (area == null)
        //$$ {
            //$$ updateBounds.call(grid, bounds, exclusions, anchor);
            //$$ return;
        //$$ }
        //$$ Set<ImmutableRect2i> combined = new HashSet<>(exclusions);
        //$$ boolean added = combined.add(new ImmutableRect2i(area[0], area[1], area[2], area[3]));
        //$$ updateBounds.call(grid, bounds, combined, anchor);
        //$$ // JEI hides its search field when the ingredient grid has no room.
        //$$ if (!grid.hasRoom())
        //$$ {
            //$$ ItemManagerHistoryOverlay.hideJeiLayout();
            //$$ if (added)
            //$$ {
                //$$ updateBounds.call(grid, bounds, exclusions, anchor);
            //$$ }
        //$$ }
    //$$ }

    //$$ @Dynamic
    //$$ @ModifyArg(
            //$$ method = "updateBounds",
            //$$ at = @At(
                    //$$ value = "INVOKE",
                    //$$ target = "Lmezz/jei/gui/input/GuiTextFieldFilter;updateBounds(Lmezz/jei/common/util/ImmutableRect2i;)V"),
            //$$ index = 0,
            //$$ require = 0,
            //$$ remap = false)
    //$$ private ImmutableRect2i halfmasa_captureJeiSearchArea(ImmutableRect2i area)
    //$$ {
        //$$ ItemManagerHistoryOverlay.updateJeiSearchArea(this, area);
        //$$ return area;
    //$$ }

    //$$ @Dynamic
    //$$ @Inject(method = "drawForeground", at = @At("TAIL"), require = 0, remap = false)
    //$$ private void halfmasa_renderJeiHistory(
            //$$ Minecraft minecraft,
            //$$ GuiGraphics graphics,
            //$$ int mouseX,
            //$$ int mouseY,
            //$$ float partialTicks,
            //$$ CallbackInfo ci)
    //$$ {
        //$$ ItemManagerHistoryOverlay.renderJei(this, graphics, mouseX, mouseY);
    //$$ }

    //$$ @Dynamic
    //$$ @Inject(method = "drawScreen", at = @At("TAIL"), require = 0, remap = false)
    //$$ private void halfmasa_renderJeiHistoryLegacy(
            //$$ Minecraft minecraft,
            //$$ GuiGraphics graphics,
            //$$ int mouseX,
            //$$ int mouseY,
            //$$ float partialTicks,
            //$$ CallbackInfo ci)
    //$$ {
        //$$ ItemManagerHistoryOverlay.renderJei(this, graphics, mouseX, mouseY);
    //$$ }
//$$ }
//#endif
