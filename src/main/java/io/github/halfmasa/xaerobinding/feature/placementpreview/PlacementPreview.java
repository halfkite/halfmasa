package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
//#if MC >= 26.1
import com.mojang.blaze3d.vertex.QuadInstance;
//#endif
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.SubmitNodeCollector;
//#endif
//#if MC >= 1.21.11
import net.minecraft.client.renderer.rendertype.RenderTypes;
//#else
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//#if MC >= 26.1
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.LevelRenderState;
//#elseif MC >= 1.21.10
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//#endif
//#if MC < 26.1
//$$ import net.minecraft.client.renderer.block.model.BakedQuad;
//#if MC >= 1.21.5
//$$ import net.minecraft.client.renderer.block.model.BlockModelPart;
//#endif
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//#endif
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.compat.RenderPipelineCompatibility;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewBlockItemInvoker;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewStairsInvoker;

public final class PlacementPreview {
    private PlacementPreview() {}
    private static final Direction[] DIRECTIONS = Direction.values();
    private record Entry(BlockPos pos,BlockState state,boolean neighbour) {}
    private record Result(ClientLevel world, List<Entry> entries, BlockPos invalid, boolean uncertain) {}
    private static volatile Result result;
    private static List<String> lastWhite=List.of(),lastBlack=List.of();
    private static Set<String> white=Set.of(),black=Set.of();
    private static boolean allowed(BlockState state) {
        var mode=(PlacementPreviewFilter)Configs.PLACEMENT_PREVIEW_FILTER.getOptionListValue();
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        var w=Configs.PLACEMENT_PREVIEW_WHITELIST.getStrings(); var b=Configs.PLACEMENT_PREVIEW_BLACKLIST.getStrings();
        if(!lastWhite.equals(w)){lastWhite=List.copyOf(w);white=new HashSet<>(w);}
        if(!lastBlack.equals(b)){lastBlack=List.copyOf(b);black=new HashSet<>(b);}
        return switch(mode){case ALL->true;case WHITELIST->white.contains(id);case BLACKLIST->!black.contains(id);
            case DIRECTIONAL->state.getProperties().stream().anyMatch(p->p.getName().equals("facing")||p.getName().equals("axis")||p.getName().equals("rotation")||p.getName().equals("half"));};
    }
    /** Called on the extraction thread; never calls useOn/place/setBlock or sends packets. */
    public static void extract() {
        //#if MC < 1.21.11
        //$$ PlacementPreviewBoxes.begin();
        //#endif
        result=null;
        if(!Configs.PLACEMENT_PREVIEW.getBooleanValue()){FluidFlowPreview.clear();return;}
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.gameMode==null||mc.player.isSpectator()||MinecraftClientCompat.getScreen(mc)!=null){FluidFlowPreview.clear();return;}
        var player=mc.player; var world=mc.level;
        boolean tweakeroo = RenderPipelineCompatibility.SUPPORTED_TWEAKEROO
                && Configs.PLACEMENT_PREVIEW_TWEAKEROO.getBooleanValue();
        boolean mainBlock = player.getMainHandItem().getItem() instanceof BlockItem;
        boolean offhand = Configs.PLACEMENT_PREVIEW_OFFHAND.getBooleanValue();
        boolean angelItem = mainBlock || offhand && player.getOffhandItem().getItem() instanceof BlockItem;
        BlockHitResult angelHit = tweakeroo && angelItem ? TweakerooPreviewCompat.angelHit(player, mc.hitResult) : null;
        if (angelHit == null && FluidFlowPreview.renderIfHeld(mc)) return;
        BlockHitResult original;
        if (angelHit != null) {
            FluidFlowPreview.clear();
            original = angelHit;
        } else if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            original = blockHit;
        } else return;
        InteractionHand hand=InteractionHand.MAIN_HAND;
        if(!mainBlock) {
            if(!offhand || angelHit == null && !player.getMainHandItem().isEmpty())return;
            hand=InteractionHand.OFF_HAND;
        }
        if(!(player.getItemInHand(hand).getItem() instanceof BlockItem item))return;
        if(angelHit == null && player.getEyePosition().distanceToSqr(original.getLocation())>
                Math.pow(player.blockInteractionRange()+0.01,2))return;
        // Avoid predicting Litematica Easy Place when its interaction is intercepted; conservative for now.
        if(FabricLoader.getInstance().isModLoaded("litematica") && player.isUsingItem())return;
        var target=new TweakerooPreviewCompat.Target(original,null,null);
        if(tweakeroo && angelHit == null) {
            target=TweakerooPreviewCompat.resolve(player,hand,original); if(target==null)return;
        }
        BlockPlaceContext ctx=target.virtualYaw()==null ? new BlockPlaceContext(player,hand,player.getItemInHand(hand),target.hit()) : new PreviewContext(player,hand,target.hit(),target.virtualYaw());
        BlockPos pos=ctx.getClickedPos();
        if(!ctx.canPlace()||!validPosition(world,pos)){invalid(world,pos);return;}
        ctx=item.updatePlacementContext(ctx); if(ctx==null){invalid(world,pos);return;}
        pos=ctx.getClickedPos().immutable();
        if(!validPosition(world,pos)){invalid(world,pos);return;}
        var invoker=(PlacementPreviewBlockItemInvoker)item;
        BlockState state=invoker.halfmasa$placementState(ctx);
        if(state==null){invalid(world,pos);return;}
        var properties=player.getItemInHand(hand).get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        if(properties!=null)state=properties.apply(state);
        boolean uncertain=false;
        // The protocol toggle expresses intent, not proof of server support.
        if(target.protocolFacing()!=null) {
            Direction requested=target.protocolFacing();
            for(var property:state.getProperties()) {
                if(property instanceof EnumProperty<?> && property.getName().equals("facing")) {
                    if(state.hasProperty(BlockStateProperties.FACING))state=state.setValue(BlockStateProperties.FACING,requested);
                    else if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)&&requested.getAxis().isHorizontal())state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,requested);
                    break;
                }
            }
            if(state.hasProperty(BlockStateProperties.AXIS))state=state.setValue(BlockStateProperties.AXIS,requested.getAxis());
            if(state.hasProperty(BlockStateProperties.HALF)&& (state.getBlock() instanceof StairBlock || state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock))state=state.setValue(BlockStateProperties.HALF,Half.BOTTOM);
            uncertain=true;
        }
        if(!invoker.halfmasa$canPlace(ctx,state)){invalid(world,pos);return;}
        if(!allowed(state))return;
        Map<BlockPos,BlockState> predicted=new LinkedHashMap<>(); predicted.put(pos,state);
        if(state.getBlock() instanceof DoorBlock) {
            var upper=pos.above(); if(!validPosition(world,upper)||!world.getBlockState(upper).canBeReplaced()){invalid(world,pos);return;}
            predicted.put(upper,state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        } else if(state.getBlock() instanceof BedBlock) {
            var head=pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(!validPosition(world,head)||!world.getBlockState(head).canBeReplaced()){invalid(world,pos);return;}
            predicted.put(head,state.setValue(BlockStateProperties.BED_PART,BedPart.HEAD));
        }
        var view=new PreviewBlockView(world,predicted);
        if(state.getBlock() instanceof StairBlock) predicted.put(pos,stairState(state,view,pos));
        Set<BlockPos> primary=Set.copyOf(predicted.keySet());
        if(Configs.PLACEMENT_PREVIEW_NEIGHBOURS.getBooleanValue()) {
            for(Direction dir:DIRECTIONS) {
                if(!dir.getAxis().isHorizontal())continue;
                var neighbour=pos.relative(dir); if(!validPosition(world,neighbour))continue;
                var old=world.getBlockState(neighbour);
                if(old.getBlock() instanceof StairBlock){var next=stairState(old,view,neighbour);if(next!=old)predicted.put(neighbour,next);}
            }
        }
        List<Entry> entries=new ArrayList<>();
        predicted.forEach((p,s)->entries.add(new Entry(p,s,!primary.contains(p))));
        result=new Result(world,List.copyOf(entries),null,uncertain);
        emitGizmos(result);
    }
    private static BlockState stairState(BlockState state,PreviewBlockView view,BlockPos pos){return state.setValue(BlockStateProperties.STAIRS_SHAPE,PlacementPreviewStairsInvoker.halfmasa$shape(state,view,pos));}
    private static boolean validPosition(ClientLevel world,BlockPos pos){return !world.isOutsideBuildHeight(pos)&&world.getWorldBorder().isWithinBounds(pos)&&world.hasChunkAt(pos);}
    private static void invalid(ClientLevel world,BlockPos pos){if(Configs.PLACEMENT_PREVIEW_INVALID.getBooleanValue()){result=new Result(world,List.of(),pos,false);emitGizmos(result);}}
    private static void emitGizmos(Result snapshot) {
        if(snapshot.invalid()!=null){PlacementPreviewBoxes.draw(new AABB(snapshot.invalid()).inflate(.002), 0xffff4545);return;}
        var mode=(PlacementPreviewOptions)Configs.PLACEMENT_PREVIEW_MODE.getOptionListValue();
        for(var e:snapshot.entries()) {
            if(mode==PlacementPreviewOptions.MODEL&&!e.neighbour()&&!snapshot.uncertain())continue;
            int color=snapshot.uncertain()?0xffffc14d:e.neighbour()?0xff63c6ff:0xffe7f7ff;
            var shape=e.state().getShape(snapshot.world(),e.pos());
            for(var box:shape.toAabbs())PlacementPreviewBoxes.draw(box.move(e.pos().getX(),e.pos().getY(),e.pos().getZ()).inflate(.002), color);
        }
    }
    //#if MC >= 1.21.10
    /** Baked quads preserve resource-pack geometry and alpha. Existing neighbour chunks are NOT modified. */
    public static void submit(LevelRenderState renderState,SubmitNodeCollector collector) {
        //#if MC < 1.21.11
        //$$ if (renderState.cameraRenderState != null && Configs.PLACEMENT_PREVIEW.getBooleanValue())
        //$$     PlacementPreviewBoxes.submit(renderState.cameraRenderState.pos, collector);
        //#endif
        Result snapshot=result; Minecraft mc=Minecraft.getInstance();
        if(snapshot==null||snapshot.world()!=mc.level||renderState.cameraRenderState==null||!Configs.PLACEMENT_PREVIEW.getBooleanValue())return;
        if(Configs.PLACEMENT_PREVIEW_MODE.getOptionListValue()==PlacementPreviewOptions.OUTLINE)return;
        int alpha=Math.max(1,Math.min(255,Math.round(Configs.PLACEMENT_PREVIEW_OPACITY.getIntegerValue()*255f/100f)));
        //#if MC >= 26.1
        var modelSet=mc.getModelManager().getBlockStateModelSet();
        //#else
        //$$ var modelSet = mc.getBlockRenderer().getBlockModelShaper();
        //#endif
        var camera=renderState.cameraRenderState.pos;
        for(var entry:snapshot.entries()) {
            // Existing neighbour geometry cannot be removed here; render its future shape as an outline only.
            if(entry.neighbour()||entry.state().getRenderShape()!=RenderShape.MODEL)continue;
            var pos=entry.pos(); var state=entry.state();
            var rng = RandomSource.create(state.getSeed(pos));
            //#if MC >= 26.1
            var parts=new ArrayList<BlockStateModelPart>();
            modelSet.get(state).collectParts(rng,parts);
            var tints=mc.getBlockColors().getTintSources(state);
            int[] colors=new int[tints.size()];for(int i=0;i<colors.length;i++)colors[i]=tints.get(i).colorInWorld(state,snapshot.world(),pos);
            //#else
            //$$ var parts = new ArrayList<BlockModelPart>();
            //$$ modelSet.getBlockModel(state).collectParts(rng, parts);
            //#endif
            PoseStack pose=new PoseStack(); pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);pose.translate(state.getOffset(pos));
            //#if MC >= 1.21.11
            collector.submitCustomGeometry(pose,RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS,false),(matrix,buffer)->{
            //#else
            //$$ collector.submitCustomGeometry(pose,RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS,false),(matrix,buffer)->{
            //#endif
                //#if MC >= 26.1
                QuadInstance instance=new QuadInstance();instance.setLightCoords(15728880);instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                for(var part:parts){
                    for(var face:DIRECTIONS)for(var quad:part.getQuads(face)){int tint=quad.materialInfo().tintIndex();instance.setColor((alpha<<24)|(tint>=0&&tint<colors.length?colors[tint]&0xffffff:0xffffff));buffer.putBakedQuad(matrix,quad,instance);}
                    for(var quad:part.getQuads(null)){int tint=quad.materialInfo().tintIndex();instance.setColor((alpha<<24)|(tint>=0&&tint<colors.length?colors[tint]&0xffffff:0xffffff));buffer.putBakedQuad(matrix,quad,instance);}
                }
                //#else
                //$$ for (var part : parts) {
                //$$     for (var face : DIRECTIONS)
                //$$         for (var quad : part.getQuads(face)) drawLegacyQuad(mc, snapshot, entry, alpha, matrix, buffer, quad);
                //$$     for (var quad : part.getQuads(null)) drawLegacyQuad(mc, snapshot, entry, alpha, matrix, buffer, quad);
                //$$ }
                //#endif
            });
        }
    }
    //#else
    //$$ public static void renderLegacy() {
    //$$     Minecraft mc = Minecraft.getInstance();
    //$$     if (!Configs.PLACEMENT_PREVIEW.getBooleanValue() || mc.level == null) return;
    //$$     var camera = mc.gameRenderer.getMainCamera().getPosition();
    //$$     var buffers = mc.renderBuffers().bufferSource();
    //$$     PlacementPreviewBoxes.render(camera, buffers);
    //$$     Result snapshot = result;
    //$$     if (snapshot == null || snapshot.world() != mc.level
    //$$             || Configs.PLACEMENT_PREVIEW_MODE.getOptionListValue() == PlacementPreviewOptions.OUTLINE) return;
    //$$     int alpha = Math.round(Configs.PLACEMENT_PREVIEW_OPACITY.getIntegerValue() * 255F / 100F);
    //$$     var renderType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS, false);
    //$$     var vertices = buffers.getBuffer(renderType);
    //$$     var models = mc.getBlockRenderer().getBlockModelShaper();
    //$$     var pose = new PoseStack();
    //$$     for (Entry entry : snapshot.entries()) {
    //$$         if (entry.neighbour() || entry.state().getRenderShape() != RenderShape.MODEL) continue;
    //$$         var pos = entry.pos();
    //$$         var state = entry.state();
    //$$         pose.pushPose();
    //$$         pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
    //#if MC >= 1.21.3
    //$$         pose.translate(state.getOffset(pos));
    //#else
    //$$         var offset = state.getOffset(snapshot.world(), pos);
    //$$         pose.translate(offset.x, offset.y, offset.z);
    //#endif
    //#if MC >= 1.21.5
    //$$         var parts = models.getBlockModel(state).collectParts(RandomSource.create(state.getSeed(pos)));
    //$$         for (var part : parts) {
    //$$             for (var face : DIRECTIONS)
    //$$                 for (var quad : part.getQuads(face)) drawLegacyQuad(mc, snapshot, entry, alpha, pose.last(), vertices, quad);
    //$$             for (var quad : part.getQuads(null)) drawLegacyQuad(mc, snapshot, entry, alpha, pose.last(), vertices, quad);
    //$$         }
    //#else
    //$$         var model = models.getBlockModel(state);
    //$$         long seed = state.getSeed(pos);
    //$$         for (var face : DIRECTIONS)
    //$$             for (var quad : model.getQuads(state, face, RandomSource.create(seed)))
    //$$                 drawLegacyQuad(mc, snapshot, entry, alpha, pose.last(), vertices, quad);
    //$$         for (var quad : model.getQuads(state, null, RandomSource.create(seed)))
    //$$             drawLegacyQuad(mc, snapshot, entry, alpha, pose.last(), vertices, quad);
    //#endif
    //$$         pose.popPose();
    //$$     }
    //$$     buffers.endBatch(renderType);
    //$$ }
    //#endif
    //#if MC < 26.1
    //$$ private static void drawLegacyQuad(Minecraft mc, Result snapshot, Entry entry, int alpha,
    //$$         PoseStack.Pose pose, VertexConsumer vertices, BakedQuad quad) {
    //#if MC >= 1.21.5
    //$$     int tint = quad.tintIndex();
    //#else
    //$$     int tint = quad.getTintIndex();
    //#endif
    //$$     int color = tint < 0 ? 0xFFFFFF : mc.getBlockColors().getColor(entry.state(), snapshot.world(), entry.pos(), tint);
    //$$     vertices.putBulkData(pose, quad, ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F,
    //$$             (color & 255) / 255F, alpha / 255F, 15728880, OverlayTexture.NO_OVERLAY);
    //$$ }
    //#endif
}
//#endif
