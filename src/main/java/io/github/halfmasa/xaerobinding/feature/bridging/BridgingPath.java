package io.github.halfmasa.xaerobinding.feature.bridging;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import io.github.halfmasa.xaerobinding.config.BridgingAdjacencyMode;

final class BridgingPath
{
    private static final double EPSILON = 1.0E-7D;

    private BridgingPath() {}

    static List<BlockPos> trace(Vec3 start, Vec3 end, BridgingAdjacencyMode adjacency, double snapStrength)
    {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        int sampleCount = Math.max(1, (int) Math.ceil(length * 16.0D));
        Set<BlockPos> basePath = new LinkedHashSet<>();
        for (int index = 0; index <= sampleCount; index++)
        {
            double progress = index / (double) sampleCount;
            basePath.add(BlockPos.containing(start.add(delta.scale(progress))));
        }

        Set<BlockPos> candidates = new LinkedHashSet<>(basePath);
        int maximumChangedAxes = adjacency.getMaximumChangedAxes();
        double halfExtent = Math.max(0.0D, Math.min(1.0D, snapStrength)) * 0.5D;
        if (maximumChangedAxes > 0 && halfExtent > EPSILON)
        {
            for (BlockPos base : basePath)
            {
                for (int offsetX = -1; offsetX <= 1; offsetX++)
                {
                    for (int offsetY = -1; offsetY <= 1; offsetY++)
                    {
                        for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
                        {
                            int changedAxes = (offsetX != 0 ? 1 : 0) + (offsetY != 0 ? 1 : 0) + (offsetZ != 0 ? 1 : 0);
                            if (changedAxes == 0 || changedAxes > maximumChangedAxes)
                            {
                                continue;
                            }
                            BlockPos neighbor = base.offset(offsetX, offsetY, offsetZ);
                            if (lineIntersectsBox(start, end, neighbor, halfExtent))
                            {
                                candidates.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        List<BlockPos> ordered = new ArrayList<>(candidates);
        double deltaLengthSquared = Math.max(EPSILON, delta.lengthSqr());
        ordered.sort(Comparator
                .comparingDouble((BlockPos pos) -> projection(start, delta, deltaLengthSquared, pos))
                .thenComparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(start)));
        return ordered;
    }

    private static double projection(Vec3 start, Vec3 delta, double deltaLengthSquared, BlockPos position)
    {
        Vec3 relative = Vec3.atCenterOf(position).subtract(start);
        return (relative.x * delta.x + relative.y * delta.y + relative.z * delta.z) / deltaLengthSquared;
    }

    private static boolean lineIntersectsBox(Vec3 start, Vec3 end, BlockPos position, double halfExtent)
    {
        Vec3 delta = end.subtract(start);
        double centerX = position.getX() + 0.5D;
        double centerY = position.getY() + 0.5D;
        double centerZ = position.getZ() + 0.5D;
        double[] interval = {0.0D, 1.0D};
        return clipAxis(start.x, delta.x, centerX - halfExtent, centerX + halfExtent, interval) &&
                clipAxis(start.y, delta.y, centerY - halfExtent, centerY + halfExtent, interval) &&
                clipAxis(start.z, delta.z, centerZ - halfExtent, centerZ + halfExtent, interval);
    }

    private static boolean clipAxis(double start, double delta, double minimum, double maximum, double[] interval)
    {
        if (Math.abs(delta) < EPSILON)
        {
            return start >= minimum && start <= maximum;
        }
        double first = (minimum - start) / delta;
        double second = (maximum - start) / delta;
        if (first > second)
        {
            double swap = first;
            first = second;
            second = swap;
        }
        interval[0] = Math.max(interval[0], first);
        interval[1] = Math.min(interval[1], second);
        return interval[0] <= interval[1];
    }
}
