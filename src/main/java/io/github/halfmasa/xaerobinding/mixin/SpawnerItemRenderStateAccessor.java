package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface SpawnerItemRenderStateAccessor
{
    @Accessor("activeLayerCount") int halfmasa$getActiveLayerCount();
    @Accessor("layers") ItemStackRenderState.LayerRenderState[] halfmasa$getLayers();
}
//#endif
