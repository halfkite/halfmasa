//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer", remap = false)
public interface LitematicaFilteredContainerAccessor
{
    @Invoker("get") BlockState halfmasa$getState(int x, int y, int z);
    @Invoker("getSize") Vec3i halfmasa$getSize();
}
//#endif
