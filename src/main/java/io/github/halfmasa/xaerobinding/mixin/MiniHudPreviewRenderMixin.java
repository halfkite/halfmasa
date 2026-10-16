package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderUtils.class, remap = false)
public abstract class MiniHudPreviewRenderMixin
{
    @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getStoredItems(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/core/NonNullList;"))
    private static NonNullList<ItemStack> halfmasa$container(ItemStack stack, int slots, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.items(stack, slots, false, () -> original.call(stack, slots));
    }

    @WrapOperation(method = "renderBundlePreview(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/world/item/ItemStack;IIIZ)V", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getBundleItems(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/core/NonNullList;"))
    private static NonNullList<ItemStack> halfmasa$bundle(ItemStack stack, int slots, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.items(stack, slots, true, () -> original.call(stack, slots));
    }

    @WrapOperation(method = {"renderShulkerBoxPreview", "renderBundlePreview(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/world/item/ItemStack;IIIZ)V", "renderDataItemsPreview"}, at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getAsInventory(Lnet/minecraft/core/NonNullList;)Lnet/minecraft/world/Container;"))
    private static Container halfmasa$inventory(NonNullList<ItemStack> items, Operation<Container> original)
    {
        return PreviewCache.inventory(items, () -> original.call(items));
    }

    @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getStoredBlockEntityDataTag(Lnet/minecraft/world/item/ItemStack;)Lfi/dy/masa/malilib/util/data/tag/CompoundData;"))
    private static CompoundData halfmasa$blockData(ItemStack stack, Operation<CompoundData> original)
    {
        return PreviewCache.blockData(stack, () -> original.call(stack));
    }

    @WrapOperation(method = "renderDataItemsPreview", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getDataItems(Lfi/dy/masa/malilib/util/data/tag/CompoundData;ILnet/minecraft/core/RegistryAccess;)Lnet/minecraft/core/NonNullList;"))
    private static NonNullList<ItemStack> halfmasa$dataItems(CompoundData data, int slots, RegistryAccess registry, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.data(slots, registry, data, () -> original.call(data, slots, registry));
    }
}
//#endif
