package io.github.halfmasa.xaerobinding.feature.bridging;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//#if MC < 1.21.11
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import org.joml.Vector3f;
//#endif

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
//#if MC < 1.21.11
//$$ import net.minecraft.client.renderer.LevelRenderer;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC >= 1.21.11
import net.minecraft.client.renderer.rendertype.RenderType;
//#else
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//#if MC >= 1.21.8
import net.minecraft.client.renderer.RenderPipelines;
//#endif
//#if MC >= 1.21.4 && MC < 1.21.11
//$$ import net.minecraft.client.renderer.ShapeRenderer;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//#if MC >= 1.21.11
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
//#endif

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.BridgingAdjacencyMode;
import io.github.halfmasa.xaerobinding.config.BridgingAxisMode;
import io.github.halfmasa.xaerobinding.config.BridgingAxisOverride;
import io.github.halfmasa.xaerobinding.config.BridgingPerspectiveMode;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

public final class BridgingAssist implements IClientTickHandler
{
    private static final BridgingAssist INSTANCE = new BridgingAssist();
    //#if MC >= 1.21.11
    private static final Identifier INDICATOR_UP = Identifier.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/up");
    private static final Identifier INDICATOR_DOWN = Identifier.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/down");
    private static final Identifier INDICATOR_HORIZONTAL = Identifier.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/horizontal");
    //#else
    //$$ private static final ResourceLocation INDICATOR_UP = ResourceLocation.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/up");
    //$$ private static final ResourceLocation INDICATOR_DOWN = ResourceLocation.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/down");
    //$$ private static final ResourceLocation INDICATOR_HORIZONTAL = ResourceLocation.fromNamespaceAndPath(XaeroWorldBinding.MOD_ID, "bridging/indicator/horizontal");
    //#endif
    private static final double DIRECTION_SIMILARITY_THRESHOLD = 0.1D;
    private static final double REACH_TOLERANCE = 0.25D;
    private static final int INDICATOR_SIZE = 32;

    private BridgingTarget currentTarget;
    private double lastKnownYFraction;

    private BridgingAssist() {}

    public static BridgingAssist getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        LocalPlayer player = client.player;
        if (player != null && player.onGround())
        {
            this.lastKnownYFraction = Mth.frac(player.getY());
        }
        this.currentTarget = this.canCalculateTarget(client, player) ? this.findTarget(client, player) : null;
    }

    public InteractionResult tryPlace(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand)
    {
        Minecraft client = Minecraft.getInstance();
        BridgingTarget target = this.currentTarget;
        ItemStack stack = player.getItemInHand(hand);
        if (target == null || !this.canCalculateTarget(client, player) || !this.isPlaceableStack(stack) ||
                !this.isPlacementPositionValid(player, target.placementPosition()) ||
                !this.canBuildOff(player, target.placementPosition(), target.supportDirection()))
        {
            return null;
        }

        BlockHitResult hitResult = this.createPlacementHit(player, stack, target);
        double reach = player.blockInteractionRange() + REACH_TOLERANCE;
        if (player.getEyePosition().distanceToSqr(hitResult.getLocation()) > reach * reach ||
                !player.mayUseItemAt(hitResult.getBlockPos(), hitResult.getDirection(), stack))
        {
            return null;
        }
        return gameMode.useItemOn(player, hand, hitResult);
    }

    //#if MC >= 26.1
    public void renderIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, boolean debugScreenVisible)
    //#else
    //$$ public void renderIndicator(GuiGraphics graphics, DeltaTracker deltaTracker, boolean debugScreenVisible)
    //#endif
    {
        Minecraft client = Minecraft.getInstance();
        BridgingTarget target = this.currentTarget;
        if (target == null || !Configs.BRIDGING_SHOW_CROSSHAIR.getBooleanValue() ||
                !client.options.getCameraType().isFirstPerson())
        {
            return;
        }

        //#if MC >= 1.21.11
        Identifier sprite = switch (target.supportDirection())
        //#else
        //$$ ResourceLocation sprite = switch (target.supportDirection())
        //#endif
        {
            case UP -> INDICATOR_DOWN;
            case DOWN -> INDICATOR_UP;
            default -> INDICATOR_HORIZONTAL;
        };
        int x = (graphics.guiWidth() - INDICATOR_SIZE + 1) / 2;
        int y = (graphics.guiHeight() - INDICATOR_SIZE + 1) / 2 + (debugScreenVisible ? 15 : 0);
        //#if MC >= 1.21.8
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, INDICATOR_SIZE, INDICATOR_SIZE);
        //#elseif MC >= 1.21.4
        //$$ graphics.blitSprite(RenderType::guiTextured, sprite, x, y, INDICATOR_SIZE, INDICATOR_SIZE);
        //#else
        //$$ graphics.blitSprite(sprite, x, y, INDICATOR_SIZE, INDICATOR_SIZE);
        //#endif
    }

    //#if MC >= 1.21.11
    //#elseif MC >= 1.21.10
    //$$ public void renderOutline(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
    //$$         double cameraX, double cameraY, double cameraZ)
    //$$ {
    //$$     BridgingTarget target = this.currentTarget;
    //$$     if (target == null || !Configs.BRIDGING_SHOW_OUTLINE.getBooleanValue()) return;
    //$$     int color = Configs.BRIDGING_OUTLINE_COLOR.getIntegerValue();
    //$$     float alpha = ((color >>> 24) & 0xFF) / 255.0F;
    //$$     float red = ((color >>> 16) & 0xFF) / 255.0F;
    //$$     float green = ((color >>> 8) & 0xFF) / 255.0F;
    //$$     float blue = (color & 0xFF) / 255.0F;
    //$$     AABB box = new AABB(target.placementPosition()).inflate(0.002D).move(-cameraX, -cameraY, -cameraZ);
    //$$     VertexConsumer vertices = bufferSource.getBuffer(RenderType.lines());
    //$$     ShapeRenderer.renderLineBox(poseStack.last(), vertices, box, red, green, blue, alpha);
    //$$ }
    //#elseif MC >= 1.21.4
    //$$ public void renderOutline(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
    //$$         double cameraX, double cameraY, double cameraZ)
    //$$ {
    //$$     BridgingTarget target = this.currentTarget;
    //$$     if (target == null || !Configs.BRIDGING_SHOW_OUTLINE.getBooleanValue()) return;
    //$$     int color = Configs.BRIDGING_OUTLINE_COLOR.getIntegerValue();
    //$$     float alpha = ((color >>> 24) & 0xFF) / 255.0F;
    //$$     float red = ((color >>> 16) & 0xFF) / 255.0F;
    //$$     float green = ((color >>> 8) & 0xFF) / 255.0F;
    //$$     float blue = (color & 0xFF) / 255.0F;
    //$$     AABB box = new AABB(target.placementPosition()).inflate(0.002D).move(-cameraX, -cameraY, -cameraZ);
    //$$     VertexConsumer vertices = bufferSource.getBuffer(RenderType.lines());
    //$$     ShapeRenderer.renderLineBox(poseStack, vertices, box, red, green, blue, alpha);
    //$$ }
    //#else
    //$$ public void renderOutline(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
    //$$         double cameraX, double cameraY, double cameraZ)
    //$$ {
    //$$     BridgingTarget target = this.currentTarget;
    //$$     if (target == null || !Configs.BRIDGING_SHOW_OUTLINE.getBooleanValue()) return;
    //$$     int color = Configs.BRIDGING_OUTLINE_COLOR.getIntegerValue();
    //$$     float alpha = ((color >>> 24) & 0xFF) / 255.0F;
    //$$     float red = ((color >>> 16) & 0xFF) / 255.0F;
    //$$     float green = ((color >>> 8) & 0xFF) / 255.0F;
    //$$     float blue = (color & 0xFF) / 255.0F;
    //$$     AABB box = new AABB(target.placementPosition()).inflate(0.002D).move(-cameraX, -cameraY, -cameraZ);
    //$$     VertexConsumer vertices = bufferSource.getBuffer(RenderType.lines());
    //$$     LevelRenderer.renderLineBox(poseStack, vertices, box, red, green, blue, alpha);
    //$$ }
    //#endif

    //#if MC >= 1.21.11
    public void emitOutlineGizmo()
    {
        BridgingTarget target = this.currentTarget;
        if (target == null || !Configs.BRIDGING_SHOW_OUTLINE.getBooleanValue())
        {
            return;
        }

        int color = Configs.BRIDGING_OUTLINE_COLOR.getIntegerValue();
        Gizmos.cuboid(new AABB(target.placementPosition()).inflate(0.002D), GizmoStyle.stroke(color));
    }
    //#endif

    private boolean canCalculateTarget(Minecraft client, LocalPlayer player)
    {
        return Configs.BRIDGING_ASSIST.getBooleanValue() && player != null && client.level != null &&
                client.gameMode != null && MinecraftClientCompat.getScreen(client) == null && !player.isSpectator() &&
                client.hitResult != null && client.hitResult.getType() == HitResult.Type.MISS &&
                (!Configs.BRIDGING_ONLY_WHEN_CROUCHING.getBooleanValue() || player.isCrouching()) &&
                (this.isPlaceableStack(player.getMainHandItem()) || this.isPlaceableStack(player.getOffhandItem()));
    }

    private BridgingTarget findTarget(Minecraft client, LocalPlayer player)
    {
        ViewLine view = this.getViewLine(client, player);
        double reach = player.blockInteractionRange();
        double minimumDistance = reach * Configs.BRIDGING_MINIMUM_DISTANCE.getIntegerValue() / 100.0D;
        Vec3 start = view.origin().add(view.direction().scale(minimumDistance));
        Vec3 end = view.origin().add(view.direction().scale(reach));
        BridgingAdjacencyMode adjacency = (BridgingAdjacencyMode) Configs.BRIDGING_ADJACENCY.getOptionListValue();
        List<BlockPos> path = BridgingPath.trace(start, end, adjacency, Configs.BRIDGING_SNAP_STRENGTH.getDoubleValue());
        List<Direction> supportDirections = this.getSupportDirections(view.direction());
        BridgingAxisMode axisMode = this.getAxisMode(player);

        for (BlockPos position : path)
        {
            if (!this.isPlacementPositionValid(player, position))
            {
                continue;
            }
            for (Direction supportDirection : supportDirections)
            {
                if (!axisMode.allows(supportDirection) || !this.canBuildOff(player, position, supportDirection))
                {
                    continue;
                }
                BridgingTarget target = new BridgingTarget(position.immutable(), supportDirection);
                double allowedReach = reach + REACH_TOLERANCE;
                if (player.getEyePosition().distanceToSqr(target.faceCenter()) <= allowedReach * allowedReach)
                {
                    return target;
                }
            }
        }
        return null;
    }

    private ViewLine getViewLine(Minecraft client, LocalPlayer player)
    {
        BridgingPerspectiveMode mode = (BridgingPerspectiveMode) Configs.BRIDGING_PERSPECTIVE.getOptionListValue();
        boolean useCamera = mode == BridgingPerspectiveMode.CAMERA ||
                (mode == BridgingPerspectiveMode.AUTO && client.options.getCameraType().isFirstPerson());
        if (useCamera)
        {
            Camera camera = MinecraftClientCompat.getMainCamera(client);
            //#if MC >= 1.21.11
            org.joml.Vector3fc look = camera.forwardVector();
            return new ViewLine(camera.position(), new Vec3(look.x(), look.y(), look.z()).normalize());
            //#else
            //$$ Vector3f look = camera.getLookVector();
            //$$ return new ViewLine(camera.getPosition(), new Vec3(look.x(), look.y(), look.z()).normalize());
            //#endif
        }
        return new ViewLine(player.getEyePosition(), player.getViewVector(1.0F).normalize());
    }

    private BridgingAxisMode getAxisMode(LocalPlayer player)
    {
        BridgingAxisMode base = (BridgingAxisMode) Configs.BRIDGING_AXES.getOptionListValue();
        if (!player.isCrouching())
        {
            return base;
        }
        return ((BridgingAxisOverride) Configs.BRIDGING_CROUCHING_AXES.getOptionListValue()).resolve(base);
    }

    private List<Direction> getSupportDirections(Vec3 lookDirection)
    {
        List<DirectionScore> scores = new ArrayList<>();
        for (Direction facing : Direction.values())
        {
            //#if MC >= 1.21.4
            Vec3 normal = facing.getUnitVec3();
            //#else
            //$$ Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
            //#endif
            double similarity = lookDirection.dot(normal);
            if (similarity >= DIRECTION_SIMILARITY_THRESHOLD)
            {
                scores.add(new DirectionScore(facing.getOpposite(), similarity));
            }
        }
        scores.sort(Comparator.comparingDouble(DirectionScore::similarity).reversed());
        return scores.stream().map(DirectionScore::direction).toList();
    }

    private boolean isPlacementPositionValid(LocalPlayer player, BlockPos position)
    {
        if (!player.level().getWorldBorder().isWithinBounds(position) || player.getBoundingBox().intersects(new AABB(position)))
        {
            return false;
        }
        BlockState state = player.level().getBlockState(position);
        return Configs.BRIDGING_REPLACE_NON_SOLID.getBooleanValue() ? state.canBeReplaced() : state.isAir();
    }

    private boolean canBuildOff(LocalPlayer player, BlockPos placementPosition, Direction supportDirection)
    {
        BlockPos supportPosition = placementPosition.relative(supportDirection);
        if (!player.level().getWorldBorder().isWithinBounds(supportPosition))
        {
            return false;
        }
        BlockState supportState = player.level().getBlockState(supportPosition);
        return !supportState.isAir() && supportState.getFluidState().isEmpty() && !supportState.canBeReplaced();
    }

    private boolean isPlaceableStack(ItemStack stack)
    {
        if (!(stack.getItem() instanceof BlockItem blockItem))
        {
            return false;
        }
        return !Configs.BRIDGING_SKIP_TORCHES.getBooleanValue() || !(blockItem.getBlock() instanceof BaseTorchBlock);
    }

    private BlockHitResult createPlacementHit(LocalPlayer player, ItemStack stack, BridgingTarget target)
    {
        Vec3 location = target.faceCenter();
        if (Configs.BRIDGING_SLAB_ASSIST.getBooleanValue() && target.clickedFace().getAxis().isHorizontal() &&
                stack.getItem() instanceof BlockItem blockItem &&
                (blockItem.getBlock() instanceof SlabBlock || blockItem.getBlock() instanceof TrapDoorBlock))
        {
            boolean lowerHalf = this.lastKnownYFraction > 3.0D / 16.0D && this.lastKnownYFraction < 0.5D;
            location = new Vec3(location.x, target.supportPosition().getY() + (lowerHalf ? 0.1D : 0.9D), location.z);
        }
        return new BlockHitResult(location, target.clickedFace(), target.supportPosition(), false);
    }

    private record ViewLine(Vec3 origin, Vec3 direction) {}
    private record DirectionScore(Direction direction, double similarity) {}
}
