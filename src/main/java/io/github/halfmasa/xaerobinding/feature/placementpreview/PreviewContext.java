package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
/** Virtual yaw: no player mutation, no movement packet. */
final class PreviewContext extends BlockPlaceContext {
    private final Direction horizontal;
    private final float pitch;
    PreviewContext(Player player, InteractionHand hand, BlockHitResult hit, Direction horizontal) {
        super(player, hand, player.getItemInHand(hand), hit);
        this.horizontal = horizontal;
        this.pitch = player.getXRot();
    }
    @Override public Direction getHorizontalDirection() { return horizontal == null ? super.getHorizontalDirection() : horizontal; }
    @Override public float getRotation() { return horizontal == null ? super.getRotation() : horizontal.toYRot(); }
    private Direction[] ordered() {
        if (horizontal == null) return Direction.orderedByNearest(getPlayer());
        double yaw = Math.toRadians(horizontal.toYRot());
        double p = Math.toRadians(pitch);
        Vec3 v = new Vec3(-Math.sin(yaw)*Math.cos(p), -Math.sin(p), Math.cos(yaw)*Math.cos(p));
        Direction[] ds = Direction.values().clone();
        Arrays.sort(ds, Comparator.comparingDouble((Direction d) -> -(d.getStepX()*v.x+d.getStepY()*v.y+d.getStepZ()*v.z)));
        return ds;
    }
    @Override public Direction getNearestLookingDirection() { return ordered()[0]; }
    @Override public Direction[] getNearestLookingDirections() {
        Direction[] ds = ordered();
        if (!replacingClickedOnBlock()) {
            Direction first = getClickedFace().getOpposite();
            for (int i=0;i<ds.length;i++) if (ds[i]==first) {
                System.arraycopy(ds,0,ds,1,i); ds[0]=first; break;
            }
        }
        return ds;
    }
}
//#endif
