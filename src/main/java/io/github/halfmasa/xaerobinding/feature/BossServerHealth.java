package io.github.halfmasa.xaerobinding.feature;
//#if MC >= 1.21.1
import java.util.UUID;
import io.github.halfmasa.xaerobinding.mixin.WitherBossEventAccessor;
import io.github.halfmasa.xaerobinding.mixin.DragonFightBossEventAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
public final class BossServerHealth {
    private BossServerHealth() {}
    public static HalfMasaSupportPayloads.HealthResult find(ServerPlayer player, UUID bossId) {
        if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Boss health requires server thread");
        for (var entity : player.level().getAllEntities())
            if (entity instanceof WitherBoss wither) {
                var event = ((WitherBossEventAccessor)wither).halfmasa$bossEvent();
                if (event.getId().equals(bossId) && event.getPlayers().contains(player))
                    return new HalfMasaSupportPayloads.HealthResult(bossId, wither.getHealth(), wither.getMaxHealth());
            }
        var fight = player.level().getDragonFight();
        if (fight != null) {
            var access = (DragonFightBossEventAccessor)fight;
            if (access.halfmasa$bossEvent().getId().equals(bossId) && access.halfmasa$bossEvent().getPlayers().contains(player))
                for (var dragon : player.level().getDragons())
                    if (dragon.getUUID().equals(access.halfmasa$dragonUUID()))
                        return new HalfMasaSupportPayloads.HealthResult(bossId, dragon.getHealth(), dragon.getMaxHealth());
        }
        return new HalfMasaSupportPayloads.HealthResult(bossId, 0, 0);
    }
}
//#endif
