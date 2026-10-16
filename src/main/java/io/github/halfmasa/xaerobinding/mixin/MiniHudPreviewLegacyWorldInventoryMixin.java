package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.nbt.CompoundTag;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
//#if MC > 1.21.5
import net.minecraft.core.RegistryAccess;
//#else
//$$ import net.minecraft.core.HolderLookup.Provider;
//#endif
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Desc;

//#if MC > 1.21.5
@Desc(id = "getNbtInventory", value = "getNbtInventory", args = {CompoundTag.class, int.class, RegistryAccess.class}, ret = Container.class)
//#else
//$$ @Desc(id = "getNbtInventory", value = "getNbtInventory", args = {CompoundTag.class, int.class, Provider.class}, ret = Container.class)
//#endif
@Mixin(value = InventoryUtils.class, remap = false)
public abstract class MiniHudPreviewLegacyWorldInventoryMixin
{
    @WrapMethod(method = "@Desc(getNbtInventory)")
//#if MC > 1.21.5
    private static Container halfmasa$worldInventory(CompoundTag data, int slots, RegistryAccess registry,
//#else
//$$     private static Container halfmasa$worldInventory(CompoundTag data, int slots, Provider registry,
//#endif
                                                     Operation<Container> original)
    {
        return io.github.halfmasa.xaerobinding.feature.minihud.LegacyNbtCache.data(slots, registry, data, () -> original.call(data, slots, registry));
    }
}
//#endif
