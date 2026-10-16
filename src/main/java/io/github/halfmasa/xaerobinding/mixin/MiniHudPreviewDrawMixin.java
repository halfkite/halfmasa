package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import java.util.Set;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlayType;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InventoryOverlay.class, remap = false)
public abstract class MiniHudPreviewDrawMixin
{
    @WrapMethod(method = "renderInventoryStacks(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/malilib/render/InventoryOverlayType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;DD)V")
    private static void halfmasa$gridScope(GuiContext ctx, InventoryOverlayType type, Container inventory,
                                           int x, int y, int columns, int start, int max, Set<Integer> disabled,
                                           double mouseX, double mouseY, Operation<Void> original)
    {
        ItemStack previous = PreviewCache.swapDrawingStack(null);
        try { original.call(ctx, type, inventory, x, y, columns, start, max, disabled, mouseX, mouseY); }
        finally { PreviewCache.swapDrawingStack(previous); }
    }

    @WrapOperation(method = "renderInventoryStacks(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/malilib/render/InventoryOverlayType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;DD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
    private static ItemStack halfmasa$isolateGridItem(ItemStack stack, Operation<ItemStack> original)
    {
        ItemStack isolated = PreviewCache.drawCopy(stack, () -> original.call(stack));
        if (PreviewCache.worldActive()) PreviewCache.swapDrawingStack(isolated);
        return isolated;
    }

    // The generic grid supplies a renderer-owned snapshot. Keep the hover copy upstream.
    @WrapOperation(method = "renderStackAt(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/world/item/ItemStack;FFFDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
    private static ItemStack halfmasa$drawItem(ItemStack stack, Operation<ItemStack> original)
    {
        return PreviewCache.worldActive() && PreviewCache.isDrawingStack(stack) ? stack : original.call(stack);
    }

    @WrapOperation(method = "renderStackAt(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/world/item/ItemStack;FFFDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 1))
    private static ItemStack halfmasa$drawDecorations(ItemStack stack, Operation<ItemStack> original)
    {
        return PreviewCache.worldActive() && PreviewCache.isDrawingStack(stack) ? stack : original.call(stack);
    }
}
//#endif
