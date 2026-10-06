//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase", remap = false)
public interface LitematicaTaskWorldAccessor
{
    @Accessor("world") Level halfmasa$getWorld();
}
//#endif
