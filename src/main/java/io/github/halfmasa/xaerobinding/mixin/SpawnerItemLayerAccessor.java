package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface SpawnerItemLayerAccessor
{
    @Accessor("itemTransform") ItemTransform halfmasa$getItemTransform();
    @Accessor("localTransform") Matrix4f halfmasa$getLocalTransform();
}
//#endif
