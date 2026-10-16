package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

@Desc(id = "renderBundlePreview", value = "renderBundlePreview", args = {fi.dy.masa.malilib.render.GuiContext.class, ItemStack.class, int.class, int.class, int.class, boolean.class})
@Mixin(value = RenderUtils.class, remap = false)
public abstract class MiniHudPreviewRenderMixin
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

    @WrapOperation(method = {"renderShulkerBoxPreview", "@Desc(renderBundlePreview)", "renderDataItemsPreview"}, at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getAsInventory", args = {NonNullList.class}, ret = Container.class)))
    private static Container halfmasa$inventory(NonNullList<ItemStack> items, Operation<Container> original)
    {
        return PreviewCache.inventory(items, () -> original.call(items));
    }

    //#if MC >= 26.1
    @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getStoredBlockEntityDataTag", args = {ItemStack.class}, ret = CompoundData.class)))
    private static CompoundData halfmasa$blockData(ItemStack stack, Operation<CompoundData> original)
    {
        return PreviewCache.blockData(stack, () -> original.call(stack));
    }
    //#else
    //$$ @WrapOperation(method = "renderShulkerBoxPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getStoredBlockEntityNbt", args = {ItemStack.class}, ret = CompoundTag.class)))
    //$$ private static CompoundTag halfmasa$blockTag(ItemStack stack, Operation<CompoundTag> original)
    //$$ {
    //$$     return PreviewCache.blockTag(stack, () -> original.call(stack));
    //$$ }
    //#endif

    @WrapOperation(method = "renderDataItemsPreview", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getDataItems", args = {CompoundData.class, int.class, RegistryAccess.class}, ret = NonNullList.class)))
    private static NonNullList<ItemStack> halfmasa$dataItems(CompoundData data, int slots, RegistryAccess registry, Operation<NonNullList<ItemStack>> original)
    {
        return PreviewCache.data(slots, registry, data, () -> original.call(data, slots, registry));
    }
}
//#endif
