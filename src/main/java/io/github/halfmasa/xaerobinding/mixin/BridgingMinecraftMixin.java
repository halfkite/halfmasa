package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.bridging.BridgingAssist;

@Mixin(Minecraft.class)
public abstract class BridgingMinecraftMixin
{
    @Shadow private int rightClickDelay;

    @Redirect(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult halfmasa$useBridgingTarget(
            MultiPlayerGameMode gameMode,
            Player player,
            InteractionHand hand)
    {
        InteractionResult result = player instanceof LocalPlayer localPlayer
                ? BridgingAssist.getInstance().tryPlace(gameMode, localPlayer, hand)
                : null;
        if (result == null)
        {
            return gameMode.useItem(player, hand);
        }
        if (result.consumesAction())
        {
            this.rightClickDelay = Configs.BRIDGING_PLACEMENT_DELAY.getIntegerValue();
        }
        return result;
    }
}
