package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.nbt.CompoundTag;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.NonNullList;
//#if MC > 1.21.5
import net.minecraft.core.RegistryAccess;
//#else
//$$ import net.minecraft.core.HolderLookup.Provider;
//#endif
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

//#if MC > 1.21.5
@Desc(id = "renderBundlePreview", value = "renderBundlePreview", args = {net.minecraft.client.gui.GuiGraphics.class, ItemStack.class, int.class, int.class, int.class, boolean.class})
//#else
//$$ @Desc(id = "renderBundlePreview", value = "renderBundlePreview", args = {ItemStack.class, int.class, int.class, int.class, boolean.class, net.minecraft.client.gui.GuiGraphics.class})
//#endif
@Mixin(value = RenderUtils.class, remap = false)
public abstract class MiniHudPreviewLegacyRenderMixin
{
    @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getStoredItems", args = {ItemStack.class, int.class}, ret = NonNullList.class)))
    private static NonNullList<ItemStack> halfmasa$container(ItemStack stack, int slots, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.items(stack, slots, false, () -> original.call(stack, slots));
    }

    @WrapOperation(method = "@Desc(renderBundlePreview)", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getBundleItems", args = {ItemStack.class, int.class}, ret = NonNullList.class)))
    private static NonNullList<ItemStack> halfmasa$bundle(ItemStack stack, int slots, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.items(stack, slots, true, () -> original.call(stack, slots));
    }

    @WrapOperation(method = {"renderShulkerBoxPreview", "@Desc(renderBundlePreview)", "renderNbtItemsPreview"}, at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getAsInventory", args = {NonNullList.class}, ret = Container.class)))
    private static Container halfmasa$inventory(NonNullList<ItemStack> items, Operation<Container> original)
    {
        return PreviewCache.inventory(items, () -> original.call(items));
    }

    @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getStoredBlockEntityNbt", args = {ItemStack.class}, ret = CompoundTag.class)))
    private static CompoundTag halfmasa$blockTag(ItemStack stack, Operation<CompoundTag> original)
    {
        return PreviewCache.blockTag(stack, () -> original.call(stack));
    }

//#if MC > 1.21.5
    @WrapOperation(method = "renderNbtItemsPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getNbtItems", args = {CompoundTag.class, int.class, RegistryAccess.class}, ret = NonNullList.class)))
//#else
//$$     @WrapOperation(method = "renderNbtItemsPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getNbtItems", args = {CompoundTag.class, int.class, Provider.class}, ret = NonNullList.class)))
//#endif
//#if MC > 1.21.5
    private static NonNullList<ItemStack> halfmasa$dataItems(CompoundTag data, int slots, RegistryAccess registry, Operation<NonNullList<ItemStack>> original)
//#else
//$$     private static NonNullList<ItemStack> halfmasa$dataItems(CompoundTag data, int slots, Provider registry, Operation<NonNullList<ItemStack>> original)
//#endif
    {
        return io.github.halfmasa.xaerobinding.feature.minihud.LegacyNbtCache.data(slots, registry, data, () -> original.call(data, slots, registry));
    }
}
//#endif
