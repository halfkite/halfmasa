//#if MC >= 26.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
    static StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<ItemStack>>>> halfmasa$codec()
    {
        throw new AssertionError("Mixin accessor");
    }
}
//#endif
