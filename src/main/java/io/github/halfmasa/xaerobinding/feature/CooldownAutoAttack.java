package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

/** Performs the normal attack action when the attack key is held and cooldown is ready */
public final class CooldownAutoAttack implements IClientTickHandler
{
    private static final CooldownAutoAttack INSTANCE = new CooldownAutoAttack();

    private CooldownAutoAttack() {}

    public static CooldownAutoAttack getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!Configs.COOLDOWN_AUTO_ATTACK.getBooleanValue() || client.player == null ||
            client.gameMode == null || MinecraftClientCompat.getScreen(client) != null || client.level == null ||
            client.player.isSpectator() || !client.options.keyAttack.isDown() ||
            client.player.isUsingItem())
        {
            return;
        }

        if (!(client.hitResult instanceof EntityHitResult entityHit))
        {
            return;
        }

        Entity target = entityHit.getEntity();
        Player player = client.player;
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive() || !player.canAttack(livingTarget) ||
            player.getAttackStrengthScale(0.0F) < 1.0F)
        {
            return;
        }

        ((MultiPlayerGameMode) client.gameMode).attack(player, target);
        player.resetAttackStrengthTicker();
    }
}
