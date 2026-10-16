package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler", remap = false)
public abstract class MiniHudPreviewScopeMixin
{
    @WrapMethod(method = "onRenderTooltipLast")
    private void halfmasa$previewScope(GuiContext ctx, ItemStack stack, int x, int y, Operation<Void> original)
    {
        PreviewCache.enter();
        try { original.call(ctx, stack, x, y); }
        finally { PreviewCache.exit(); }
    }

    @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InventoryUtils;getPlayerEnderItemsFromData(Lfi/dy/masa/malilib/util/data/tag/CompoundData;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/inventory/PlayerEnderChestContainer;"))
    private PlayerEnderChestContainer halfmasa$enderItems(CompoundData data, RegistryAccess registry, Operation<PlayerEnderChestContainer> original)
    {
        return PreviewCache.data("ender", registry, data.getList(NbtKeys.ENDER_ITEMS), () -> original.call(data, registry));
    }

    @WrapOperation(method = "onRenderTooltipLast", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/nbt/NbtInventory;toDataList(Lnet/minecraft/core/RegistryAccess;)Lfi/dy/masa/malilib/util/data/tag/ListData;"))
    private ListData halfmasa$serializeEnder(NbtInventory inventory, RegistryAccess registry, Operation<ListData> original)
    {
        return PreviewCache.serialize(inventory, registry, () -> original.call(inventory, registry));
    }
}
//#endif
