package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
//#if MC >= 1.21.5
import fi.dy.masa.malilib.util.position.PositionUtils;
//#else
//$$ import fi.dy.masa.malilib.util.PositionUtils;
//#endif
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewTweakerooAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
/** Read-only adapter for the supported Tweakeroo versions. Unsupported combinations fail closed. */
final class TweakerooPreviewCompat {
    private TweakerooPreviewCompat() {}
    record Target(BlockHitResult hit, Direction virtualYaw, Direction protocolFacing) {}

    static BlockHitResult angelHit(LocalPlayer player, HitResult original) {
        if (original == null || original.getType() != HitResult.Type.MISS || !player.isCreative()
                || !FeatureToggle.TWEAK_ANGEL_BLOCK.getBooleanValue()) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F)
                .scale(Configs.Generic.ANGEL_BLOCK_PLACEMENT_DISTANCE.getDoubleValue()));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
    }

    static Target resolve(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        boolean flexible = FeatureToggle.TWEAK_FLEXIBLE_BLOCK_PLACEMENT.getBooleanValue();
        boolean remember = Configs.Generic.REMEMBER_FLEXIBLE.getBooleanValue();
        boolean rotate = flexible && (Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_ROTATION.getKeybind().isKeybindHeld()
                || remember && PlacementPreviewTweakerooAccessor.halfmasa$firstRotation());
        boolean offset = flexible && (Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_OFFSET.getKeybind().isKeybindHeld()
                || remember && PlacementPreviewTweakerooAccessor.halfmasa$firstOffset());
        boolean adjacent = flexible && Hotkeys.FLEXIBLE_BLOCK_PLACEMENT_ADJACENT.getKeybind().isKeybindHeld();
        // Do not display a misleading target for paths not modeled by this adapter.
        if (FeatureToggle.TWEAK_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() &&
                (Hotkeys.ACCURATE_BLOCK_PLACEMENT_IN.getKeybind().isKeybindHeld() || Hotkeys.ACCURATE_BLOCK_PLACEMENT_REVERSE.getKeybind().isKeybindHeld())
                || FeatureToggle.TWEAK_AFTER_CLICKER.getBooleanValue()
                || FeatureToggle.TWEAK_PICK_BEFORE_PLACE.getBooleanValue()
                || FeatureToggle.TWEAK_FAKE_SNEAK_PLACEMENT.getBooleanValue()
                //#if MC >= 1.21.5 || MC == 1.21.3
                || FeatureToggle.TWEAK_SCAFFOLD_PLACE.getBooleanValue() && !(rotate || offset || adjacent)
                //#endif
                || FeatureToggle.TWEAK_Y_MIRROR.getBooleanValue() && Hotkeys.PLACEMENT_Y_MIRROR.getKeybind().isKeybindHeld()) return null;
        if (!(rotate || offset || adjacent)) return new Target(hit, null, null);
        BlockPos original = hit.getBlockPos();
        Direction face = hit.getDirection(), yaw = player.getDirection();
        Vec3 point = hit.getLocation();
        var part = PositionUtils.getHitPart(face, yaw, original, point);
        Direction rotated = PlacementPreviewTweakerooAccessor.halfmasa$rotate(face, yaw, part);
        BlockPlaceContext initial = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
        var old = player.level().getBlockState(original);
        BlockPos target = old.canBeReplaced(initial) || old.liquid() || old.canBeReplaced() ? original : original.relative(face);
        boolean changed = rotate || offset || (adjacent && part != PositionUtils.HitPart.CENTER);
        if (!changed) return new Target(hit,null,null);
        if (adjacent && part != PositionUtils.HitPart.CENTER) {
            Direction a=rotated.getOpposite(), b=face.getOpposite();
            target=target.relative(a).relative(b);
            point=point.add(a.getStepX()+b.getStepX(),a.getStepY()+b.getStepY(),a.getStepZ()+b.getStepZ());
        }
        Direction clickFace=rotate ? rotated : face;
        if (offset) {
            Direction d=rotated.getOpposite(); target=target.relative(d);
            point=point.add(d.getStepX(),d.getStepY(),d.getStepZ());
        }
        Direction virtual = null;
        if (rotate) virtual = switch(part) {
            case CENTER -> yaw.getOpposite(); case LEFT -> yaw.getCounterClockWise();
            case RIGHT -> yaw.getClockWise(); default -> yaw;
        };
        Direction requested = rotate && Configs.Generic.ACCURATE_PLACEMENT_PROTOCOL.getBooleanValue() ? clickFace.getOpposite() : null;
        BlockHitResult adjusted = new BlockHitResult(point,clickFace,target,false);
        var context = new BlockPlaceContext(player,hand,player.getItemInHand(hand),adjusted);
        var state = player.level().getBlockState(target);
        if (!(state.canBeReplaced(context)||state.liquid()||state.canBeReplaced())) return null;
        return new Target(adjusted, virtual, requested);
    }
}
//#endif
