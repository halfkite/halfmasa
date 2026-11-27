//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.api.view.ViewGroup;

@Pseudo
@Mixin(targets = "snownee.jade.addon.universal.ItemStorageProvider", remap = false)
public interface JadeBarrelStorageAccessor
{
    @Accessor("STREAM_CODEC")
    //#if MC >= 1.21.11
    static StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<ItemStack>>>> halfmasa$codec()
    //#else
    //$$ static StreamCodec<RegistryFriendlyByteBuf, Map.Entry<ResourceLocation, List<ViewGroup<ItemStack>>>> halfmasa$codec()
    //#endif
    {
        throw new AssertionError("Mixin accessor");
    }
}
//#endif
