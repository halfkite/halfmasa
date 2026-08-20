package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.BoatClientFeatures;

@Mixin(ItemInHandRenderer.class)
public abstract class BoatItemViewMixin
{
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean halfmasa$showHeldItemsWhileRowing(LocalPlayer player)
    {
        if (Configs.BOAT_ITEM_VIEW.getBooleanValue() && BoatClientFeatures.isBoat(player.getVehicle()))
        {
            return false;
        }
        return player.isHandsBusy();
    }
}
