package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface AggregationGameRendererAccessor
{
    //#if MC < 26.0
    //#if MC >= 1.21.3
    @Invoker("getFov") float halfmasa$getFov(Camera camera, float partialTick, boolean changing);
    //#else
    //$$ @Invoker("getFov") double halfmasa$getFov(Camera camera, float partialTick, boolean changing);
    //#endif
    //#endif
}
//#endif
