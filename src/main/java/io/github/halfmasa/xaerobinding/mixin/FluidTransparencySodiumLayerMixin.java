package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
//#if MC >= 26.1
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
//$$ import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
//$$ import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
//$$ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Choose the translucent material before retrieving the matching Sodium mesh builder. */
@Mixin(value = FluidRendererImpl.class, remap = false)
public abstract class FluidTransparencySodiumLayerMixin {
    //#if MC >= 26.1
    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target =
        "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forChunkLayer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"), require = 1)
    private void halfmasa$material(Args args, LevelSlice level, BlockState block, FluidState fluid,
                                   BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector,
                                   ChunkBuildBuffers buffers) {
        if (FluidTransparency.factor(fluid) != 10000) args.set(0, ChunkSectionLayer.TRANSLUCENT);
    }
    //#else
    //$$ @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target =
    //$$         "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forFluidState"), require = 1)
    //$$ private Material halfmasa$material(Material original, LevelSlice level, BlockState block, FluidState fluid) {
    //$$     return FluidTransparency.factor(fluid) == 10000 ? original : DefaultMaterials.TRANSLUCENT;
    //$$ }
    //#endif
}
//#endif
