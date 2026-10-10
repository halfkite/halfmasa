package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.4
import net.minecraft.client.renderer.item.ItemStackRenderState;
//#if MC >= 26.0
import net.minecraft.client.resources.model.cuboid.ItemTransform;
//#else
//$$ import net.minecraft.client.renderer.block.model.ItemTransform;
//#endif
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface SpawnerItemLayerAccessor
{
    //#if MC >= 26.0
    @Accessor("itemTransform") ItemTransform halfmasa$getItemTransform();
    @Accessor("localTransform") Matrix4f halfmasa$getLocalTransform();
    //#elseif MC >= 1.21.5
    //$$ @Accessor("transform") ItemTransform halfmasa$getItemTransform();
    //#else
    //$$ @Accessor("model") net.minecraft.client.resources.model.BakedModel halfmasa$getModel();
    //#endif
}
//#endif
