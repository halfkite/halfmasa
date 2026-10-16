package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(WitherBoss.class)
public interface WitherBossEventAccessor {
    @Accessor("bossEvent") ServerBossEvent halfmasa$bossEvent();
}
//#endif
