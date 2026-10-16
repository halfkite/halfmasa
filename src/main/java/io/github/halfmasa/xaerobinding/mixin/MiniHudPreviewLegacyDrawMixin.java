package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

//#if MC > 1.21.5
@Desc(id = "renderInventoryStacks", value = "renderInventoryStacks", args = {GuiGraphics.class, InventoryRenderType.class, Container.class, int.class, int.class, int.class, int.class, int.class, Set.class, Minecraft.class, double.class, double.class})
//#else
//$$ @Desc(id = "renderInventoryStacks", value = "renderInventoryStacks", args = {InventoryRenderType.class, Container.class, int.class, int.class, int.class, int.class, int.class, Set.class, Minecraft.class, GuiGraphics.class, double.class, double.class})
//#endif
//#if MC > 1.21.5
@Desc(id = "renderStackAt", value = "renderStackAt", args = {GuiGraphics.class, ItemStack.class, float.class, float.class, float.class, Minecraft.class, double.class, double.class})
//#else
//$$ @Desc(id = "renderStackAt", value = "renderStackAt", args = {ItemStack.class, float.class, float.class, float.class, Minecraft.class, GuiGraphics.class, double.class, double.class})
//#endif
@Mixin(value = InventoryOverlay.class, remap = false)
public abstract class MiniHudPreviewLegacyDrawMixin
{
    @WrapMethod(method = "@Desc(renderInventoryStacks)")
//#if MC > 1.21.5
    private static void halfmasa$gridScope(GuiGraphics ctx, InventoryRenderType type, Container inventory,
                                           int x, int y, int columns, int start, int max, Set<Integer> disabled,
                                           Minecraft client, double mouseX, double mouseY, Operation<Void> original)
//#else
//$$     private static void halfmasa$gridScope(InventoryRenderType type, Container inventory,
//$$                                            int x, int y, int columns, int start, int max, Set<Integer> disabled,
//$$                                            Minecraft client, GuiGraphics ctx, double mouseX, double mouseY, Operation<Void> original)
//#endif
    {
        ItemStack previous = PreviewCache.swapDrawingStack(null);
//#if MC > 1.21.5
        try { original.call(ctx, type, inventory, x, y, columns, start, max, disabled, client, mouseX, mouseY); }
//#else
//$$         try { original.call(type, inventory, x, y, columns, start, max, disabled, client, ctx, mouseX, mouseY); }
//#endif
        finally { PreviewCache.swapDrawingStack(previous); }
    }

    @WrapOperation(method = "@Desc(renderInventoryStacks)",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", remap = true, ordinal = 0))
    private static ItemStack halfmasa$isolateGridItem(ItemStack stack, Operation<ItemStack> original)
    {
        ItemStack isolated = PreviewCache.drawCopy(stack, () -> original.call(stack));
        if (PreviewCache.worldActive()) PreviewCache.swapDrawingStack(isolated);
        return isolated;
    }

    // The generic grid supplies a renderer-owned snapshot. Keep the hover copy upstream.
    @WrapOperation(method = "@Desc(renderStackAt)",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", remap = true, ordinal = 0))
    private static ItemStack halfmasa$drawItem(ItemStack stack, Operation<ItemStack> original)
    {
        return PreviewCache.worldActive() && PreviewCache.isDrawingStack(stack) ? stack : original.call(stack);
    }

    @WrapOperation(method = "@Desc(renderStackAt)",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", remap = true, ordinal = 1))
    private static ItemStack halfmasa$drawDecorations(ItemStack stack, Operation<ItemStack> original)
    {
        return PreviewCache.worldActive() && PreviewCache.isDrawingStack(stack) ? stack : original.call(stack);
    }
}
//#endif
