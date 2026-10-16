package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
//#if MC > 1.21.5
import fi.dy.masa.malilib.util.nbt.NbtInventory;
//#endif
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
//#if MC > 1.21.5
import net.minecraft.core.RegistryAccess;
//#else
//$$ import net.minecraft.core.HolderLookup.Provider;
//#endif
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler", remap = false)
public abstract class MiniHudPreviewLegacyScopeMixin
{
    @WrapMethod(method = "onRenderTooltipLast")
    private void halfmasa$previewScope(GuiGraphics ctx, ItemStack stack, int x, int y, Operation<Void> original)
    {
        PreviewCache.enter();
        try { original.call(ctx, stack, x, y); }
        finally { PreviewCache.exit(); }
    }

//#if MC > 1.21.5
    @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getPlayerEnderItemsFromNbt", args = {CompoundTag.class, RegistryAccess.class}, ret = PlayerEnderChestContainer.class)))
//#else
//$$     @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", desc = @Desc(owner = fi.dy.masa.malilib.util.InventoryUtils.class, value = "getPlayerEnderItemsFromNbt", args = {CompoundTag.class, Provider.class}, ret = PlayerEnderChestContainer.class)))
//#endif
//#if MC > 1.21.5
    private PlayerEnderChestContainer halfmasa$enderItems(CompoundTag data, RegistryAccess registry, Operation<PlayerEnderChestContainer> original)
//#else
//$$     private PlayerEnderChestContainer halfmasa$enderItems(CompoundTag data, Provider registry, Operation<PlayerEnderChestContainer> original)
//#endif
    {
        return io.github.halfmasa.xaerobinding.feature.minihud.LegacyNbtCache.data("ender", registry, data, () -> original.call(data, registry));
    }

    //#if MC > 1.21.5
//#if MC > 1.21.5
    @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", desc = @Desc(owner = NbtInventory.class, value = "toNbtList", args = {RegistryAccess.class}, ret = ListTag.class)))
//#else
//$$     @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", desc = @Desc(owner = NbtInventory.class, value = "toNbtList", args = {Provider.class}, ret = ListTag.class)))
//#endif
//#if MC > 1.21.5
    private ListTag halfmasa$serializeEnder(NbtInventory inventory, RegistryAccess registry, Operation<ListTag> original)
//#else
//$$     private ListTag halfmasa$serializeEnder(NbtInventory inventory, Provider registry, Operation<ListTag> original)
//#endif
    {
        return io.github.halfmasa.xaerobinding.feature.minihud.LegacyNbtCache.serialize(inventory, registry, () -> original.call(inventory, registry));
    }
    //#endif
}
//#endif
