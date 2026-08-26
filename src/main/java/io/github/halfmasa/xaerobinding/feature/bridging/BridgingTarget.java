package io.github.halfmasa.xaerobinding.feature.bridging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record BridgingTarget(BlockPos placementPosition, Direction supportDirection)
{
    public BlockPos supportPosition()
    {
        return this.placementPosition.relative(this.supportDirection);
    }

    public Direction clickedFace()
    {
        return this.supportDirection.getOpposite();
    }

    public Vec3 faceCenter()
    {
        //#if MC >= 1.21.2
        Vec3 normal = this.clickedFace().getUnitVec3().scale(0.5D);
        //#else
        //$$ Vec3 normal = Vec3.atLowerCornerOf(this.clickedFace().getNormal()).scale(0.5D);
        //#endif
        return Vec3.atCenterOf(this.supportPosition()).add(normal);
    }
}
