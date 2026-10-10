//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.feature.LitematicaBlockFilters;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand", remap = false)
public abstract class LitematicaPasteCommandBlockFilterMixin
{
    // Skip before setblock, special block handling, NBT modification or clone preparation.
    @Inject(method = "shouldSetBlock",
            at = @At("HEAD"), cancellable = true)
    private void halfmasa$filterSingleBlock(BlockState schematic, BlockState actual, CallbackInfoReturnable<Boolean> cir)
    {
        if (!LitematicaBlockFilters.allowsPaste(schematic)) cir.setReturnValue(false);
    }

    // Native fill volumes contain a single schematic state. Reject the entire volume before queuing commands.
    @Inject(method = "pasteVolume",
            at = @At("HEAD"), cancellable = true)
    private void halfmasa$filterVolume(int x1, int y1, int z1, int x2, int y2, int z2, BlockState schematic, CallbackInfo ci)
    {
        if (!LitematicaBlockFilters.allowsPaste(schematic)) ci.cancel();
    }
}
//#endif
