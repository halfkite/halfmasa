package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.10
import net.minecraft.client.renderer.blockentity.TrialSpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TrialSpawnerRenderer.class)
public interface TrialSpawnerRendererAccessor
{
    @Invoker("extractSpawnerData")
    static void halfmasa$extractSpawnerData(SpawnerRenderState state, float partialTick, Entity entity,
                                           EntityRenderDispatcher dispatcher, double previousSpin, double spin)
    {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
//#endif
