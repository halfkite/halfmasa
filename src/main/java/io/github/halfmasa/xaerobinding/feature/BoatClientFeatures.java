package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.world.entity.Entity;
//#if MC >= 1.21.11
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
//#elseif MC >= 1.21.2
//$$ import net.minecraft.world.entity.vehicle.AbstractBoat;
//#else
//$$ import net.minecraft.world.entity.vehicle.Boat;
//#endif

public final class BoatClientFeatures
{
    private BoatClientFeatures()
    {
    }

    public static boolean isBoat(Entity entity)
    {
        //#if MC >= 1.21.2
        return entity instanceof AbstractBoat;
        //#else
        //$$ return entity instanceof Boat;
        //#endif
    }
}
