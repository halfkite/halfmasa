package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import java.util.UUID;
import net.minecraft.server.level.ServerBossEvent;
//#if MC >= 26.0
import net.minecraft.world.level.dimension.end.EnderDragonFight;
//#else
//$$ import net.minecraft.world.level.dimension.end.EndDragonFight;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//#if MC >= 26.0
@Mixin(EnderDragonFight.class)
//#else
//$$ @Mixin(EndDragonFight.class)
//#endif
public interface DragonFightBossEventAccessor {
    @Accessor("dragonEvent") ServerBossEvent halfmasa$bossEvent();
    @Accessor("dragonUUID") UUID halfmasa$dragonUUID();
}
//#endif
