package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.VoidTrading;

@Mixin(MultiPlayerGameMode.class)
public abstract class VoidTradeInteractionMixin
{
//#if MC >= 26.0
    @Inject(method = "interact", at = @At("HEAD"))
    private void halfmasa$rememberTradeTarget(
            Player player,
            Entity entity,
            EntityHitResult hitResult,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir)
    {
        if (player == Minecraft.getInstance().player)
        {
            VoidTrading.onVillagerInteraction(Minecraft.getInstance(), hitResult.getEntity());
        }
    }
//#else
//$$     @Inject(method = "interact", at = @At("HEAD"))
//$$     private void halfmasa$rememberTradeTarget(
//$$             Player player,
//$$             Entity entity,
//$$             InteractionHand hand,
//$$             CallbackInfoReturnable<InteractionResult> cir)
//$$     {
//$$         if (player == Minecraft.getInstance().player)
//$$         {
//$$             VoidTrading.onVillagerInteraction(Minecraft.getInstance(), entity);
//$$         }
//$$     }
//#endif
}
