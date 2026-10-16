package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Desc;

@Desc(id = "getDataInventory", value = "getDataInventory", args = {CompoundData.class, int.class, RegistryAccess.class}, ret = Container.class)
@Mixin(value = InventoryUtils.class, remap = false)
public abstract class MiniHudPreviewWorldInventoryMixin
{
    @WrapMethod(method = "@Desc(getDataInventory)")
    private static Container halfmasa$worldInventory(CompoundData data, int slots, RegistryAccess registry,
                                                     Operation<Container> original)
    {
        return PreviewCache.worldInventory(data, slots, registry, () -> original.call(data, slots, registry));
    }
}
//#endif
