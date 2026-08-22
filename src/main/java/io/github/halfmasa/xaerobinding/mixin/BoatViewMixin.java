package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
//#if MC >= 1.21.11
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
//#elseif MC >= 1.21.2
//$$ import net.minecraft.world.entity.vehicle.AbstractBoat;
//#else
//$$ import net.minecraft.world.entity.vehicle.Boat;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;

//#if MC >= 1.21.2
@Mixin(AbstractBoat.class)
//#else
//$$ @Mixin(Boat.class)
//#endif
public abstract class BoatViewMixin
{
    @Redirect(
            method = "clampRotation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    private float halfmasa$allowFullRotation(float value, float minimum, float maximum, Entity passenger)
    {
        float clamped = Mth.clamp(value, minimum, maximum);
        if (!Configs.BOAT_VIEW_360.getBooleanValue() || !(passenger instanceof LocalPlayer))
        {
            return clamped;
        }
        if (clamped != value)
        {
            passenger.setYBodyRot(passenger.getViewYRot(1.0F) - clamped);
        }
        return value;
    }
}
