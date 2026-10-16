package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = InventoryUtils.class, remap = false)
public abstract class MiniHudPreviewWorldInventoryMixin
{
    @WrapMethod(method = "getDataInventory(Lfi/dy/masa/malilib/util/data/tag/CompoundData;ILnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/Container;")
    private static Container halfmasa$worldInventory(CompoundData data, int slots, RegistryAccess registry,
                                                     Operation<Container> original)
    {
        return PreviewCache.worldInventory(data, slots, registry, () -> original.call(data, slots, registry));
    }
}
//#endif
