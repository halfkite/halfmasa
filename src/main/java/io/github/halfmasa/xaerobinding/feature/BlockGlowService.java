package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//#if MC < 26.0
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import net.minecraft.client.renderer.OutlineBufferSource;
//$$ import net.minecraft.client.renderer.texture.TextureAtlas;
//#if MC >= 1.21.11
//$$ import net.minecraft.client.renderer.rendertype.RenderTypes;
//#else
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//$$ import net.minecraft.world.level.block.RenderShape;
//$$ import net.minecraft.world.phys.Vec3;
//#if MC <= 1.21.8
//$$ import net.minecraft.world.level.block.entity.BlockEntity;
//#endif
//#endif
//#if MC >= 26.1
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import io.github.halfmasa.xaerobinding.config.Configs;

/** Bounded client-side scan and vanilla outline submission for configured blocks. */
public final class BlockGlowService implements IClientTickHandler
{
    private static final int SECTION_SIZE = 16;
    private static final int SECTION_VOLUME = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    private static final int SCAN_BUDGET = 1024;
    private static final int SECTION_BUDGET = 128;
    private static final int MAX_OUTLINES = 512;
    private static final BlockGlowService INSTANCE = new BlockGlowService();

    private final ArrayDeque<SectionScan> scans = new ArrayDeque<>();
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    //#if MC >= 26.1
    private final Map<ModelKey, List<BlockStateModelPart>> modelPartsCache = new HashMap<>();
    private BlockStateModelSet cachedModelSet;
    //#endif
    private Map<Block, EntityGlowRule> rules = Map.of();
    private SectionPos scanCenter;
    private int scanDistance;
    private int previousGlobalDistance;

    private BlockGlowService() {}

    public static BlockGlowService getInstance() { return INSTANCE; }

    public void reload()
    {
        Map<Block, EntityGlowRule> updated = new HashMap<>();
        for (EntityGlowRule rule : EntityGlowRule.loadRules(true))
        {
            if (rule.enabled())
            {
                BuiltInRegistries.BLOCK.getOptional(rule.id()).ifPresent(block -> updated.put(block, rule));
            }
        }
        if (!updated.equals(this.rules)) this.scans.clear();
        this.rules = Map.copyOf(updated);
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        ClientLevel currentLevel = client.level;
        if (currentLevel == null || client.player == null || !Configs.BLOCK_GLOW_HIGHLIGHT.getBooleanValue()
                || this.rules.isEmpty())
        {
            this.scans.clear();
            this.snapshot = Snapshot.EMPTY;
            return;
        }

        Snapshot previous = this.snapshot;
        int globalDistance = Configs.BLOCK_GLOW_DISTANCE.getIntegerValue();
        if (previous.level() != currentLevel || this.previousGlobalDistance != globalDistance) this.scans.clear();
        this.previousGlobalDistance = globalDistance;
        int maxDistance = 0;
        for (EntityGlowRule rule : this.rules.values())
        {
            maxDistance = Math.max(maxDistance, rule.effectiveDistance(globalDistance));
        }
        Vec3 playerPos = client.player.position();
        SectionPos center = SectionPos.of(client.player.blockPosition());
        if (this.scans.isEmpty() || !center.equals(this.scanCenter) || maxDistance != this.scanDistance)
        {
            this.updateScanArea(currentLevel, center, playerPos, maxDistance);
        }

        Map<BlockPos, Match> visible = new HashMap<>();
        PriorityQueue<Match> farthestFirst = new PriorityQueue<>(
                Comparator.comparingDouble(Match::distanceSquared).reversed());
        if (previous.level() == currentLevel)
        {
            for (Match match : previous.matches())
            {
                BlockPos pos = match.pos();
                if (pos.distToCenterSqr(playerPos) <= maxDistance * maxDistance
                        && currentLevel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
                {
                    this.consider(pos, currentLevel.getBlockState(pos), playerPos, globalDistance,
                            visible, farthestFirst);
                }
            }
        }
        this.scan(currentLevel, playerPos, globalDistance, visible, farthestFirst);
        Map<BlockPos, Integer> colors = new HashMap<>();
        visible.forEach((pos, match) -> colors.put(pos, match.color()));
        this.snapshot = new Snapshot(currentLevel, List.copyOf(visible.values()), Map.copyOf(colors));
    }

    private void updateScanArea(ClientLevel level, SectionPos center, Vec3 playerPos, int distance)
    {
        this.scanCenter = center;
        this.scanDistance = distance;
        int sectionRadius = (distance + SECTION_SIZE - 1) / SECTION_SIZE;
        int minY = level.getMinSectionY();
        int maxY = minY + level.getSectionsCount() - 1;
        Set<SectionPos> wanted = new HashSet<>();
        for (int x = center.x() - sectionRadius; x <= center.x() + sectionRadius; x++)
        {
            for (int y = Math.max(minY, center.y() - sectionRadius);
                    y <= Math.min(maxY, center.y() + sectionRadius); y++)
            {
                for (int z = center.z() - sectionRadius; z <= center.z() + sectionRadius; z++)
                {
                    wanted.add(SectionPos.of(x, y, z));
                }
            }
        }
        // Keep both the queue order and partial scans for sections that are still in range.
        this.scans.removeIf(scan -> !wanted.remove(scan.pos));
        List<SectionPos> added = new ArrayList<>(wanted);
        added.sort(Comparator.comparingDouble(pos -> distanceToSectionSquared(pos, playerPos)));
        for (SectionPos pos : added) this.scans.addLast(new SectionScan(pos));
    }

    private void scan(ClientLevel level, Vec3 playerPos, int globalDistance,
            Map<BlockPos, Match> visible, PriorityQueue<Match> farthestFirst)
    {
        int blocksLeft = SCAN_BUDGET;
        int sectionsLeft = Math.min(SECTION_BUDGET, this.scans.size());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (blocksLeft > 0 && sectionsLeft-- > 0)
        {
            SectionScan scan = this.scans.removeFirst();
            SectionPos pos = scan.pos;
            LevelChunk chunk = level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
            LevelChunkSection section = chunk == null ? null : chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
            double sectionDistance = distanceToSectionSquared(pos, playerPos);
            if (section == null
                    || (visible.size() == MAX_OUTLINES && sectionDistance >= farthestFirst.element().distanceSquared())
                    || !section.maybeHas(state ->
                    inRange(this.rules.get(state.getBlock()), sectionDistance, globalDistance)))
            {
                scan.index = 0;
                this.scans.addLast(scan);
                continue;
            }

            int end = Math.min(SECTION_VOLUME, scan.index + blocksLeft);
            blocksLeft -= end - scan.index;
            while (scan.index < end)
            {
                int index = scan.index++;
                int x = index & 15;
                int y = index >> 8;
                int z = index >> 4 & 15;
                BlockState state = section.getBlockState(x, y, z);
                if (this.rules.containsKey(state.getBlock()))
                {
                    cursor.set(pos.minBlockX() + x, pos.minBlockY() + y, pos.minBlockZ() + z);
                    this.consider(cursor, state, playerPos, globalDistance, visible, farthestFirst);
                }
            }
            if (scan.index == SECTION_VOLUME)
            {
                scan.index = 0;
                this.scans.addLast(scan);
            }
            else
            {
                this.scans.addFirst(scan);
            }
        }
    }

    private static double distanceToSectionSquared(SectionPos pos, Vec3 point)
    {
        double dx = distanceToBlockCenters(point.x, pos.minBlockX());
        double dy = distanceToBlockCenters(point.y, pos.minBlockY());
        double dz = distanceToBlockCenters(point.z, pos.minBlockZ());
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToBlockCenters(double coordinate, int sectionStart)
    {
        return Math.max(0, Math.max(sectionStart + 0.5 - coordinate,
                coordinate - (sectionStart + SECTION_SIZE - 0.5)));
    }

    private static boolean inRange(EntityGlowRule rule, double distanceSquared, int globalDistance)
    {
        if (rule == null) return false;
        int distance = rule.effectiveDistance(globalDistance);
        return distanceSquared <= distance * distance;
    }

    private void consider(BlockPos pos, BlockState state, Vec3 playerPos, int globalDistance,
            Map<BlockPos, Match> visible, PriorityQueue<Match> farthestFirst)
    {
        if (visible.containsKey(pos)) return;
        EntityGlowRule rule = this.rules.get(state.getBlock());
        double distanceSquared = pos.distToCenterSqr(playerPos);
        if (!inRange(rule, distanceSquared, globalDistance)) return;
        if (visible.size() == MAX_OUTLINES)
        {
            Match farthest = farthestFirst.element();
            if (distanceSquared >= farthest.distanceSquared()) return;
            visible.remove(farthestFirst.remove().pos());
        }
        Match match = new Match(pos.immutable(), state, rule.color(), distanceSquared);
        visible.put(match.pos(), match);
        farthestFirst.add(match);
    }

    public int colorAt(BlockPos pos)
    {
        Snapshot current = this.snapshot;
        return Configs.BLOCK_GLOW_HIGHLIGHT.getBooleanValue() && current.level() == Minecraft.getInstance().level
                ? current.colors().getOrDefault(pos, 0) : 0;
    }

    public boolean hasMatches()
    {
        Snapshot current = this.snapshot;
        return Configs.BLOCK_GLOW_HIGHLIGHT.getBooleanValue() && current.level() == Minecraft.getInstance().level
                && !current.matches().isEmpty();
    }

    //#if MC < 26.0
    //$$ /** Submit model blocks to vanilla's outline buffer immediately before it is flushed. */
    //$$ public void renderLegacyOutlines()
    //$$ {
        //$$ if (!this.hasMatches()) return;
        //$$ Snapshot current = this.snapshot;
        //$$ ClientLevel level = current.level();
        //$$ Minecraft client = Minecraft.getInstance();
        //#if MC >= 1.21.10
        //$$ Vec3 camera = client.gameRenderer.getMainCamera().position();
        //#else
        //$$ Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
        //#endif
        //$$ OutlineBufferSource outlines = client.renderBuffers().outlineBufferSource();
        //$$ PoseStack pose = new PoseStack();
        //$$ for (Match match : current.matches())
        //$$ {
            //$$ int color = match.color();
            //#if MC >= 1.21.10
            //$$ outlines.setColor(color);
            //#else
            //$$ outlines.setColor(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24);
            //#endif
            //$$ BlockPos pos = match.pos();
            //$$ pose.pushPose();
            //$$ pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            //$$ if (match.state().getRenderShape() == RenderShape.MODEL)
            //$$ {
                //$$ pose.pushPose();
                //#if MC >= 1.21.3
                //$$ pose.translate(match.state().getOffset(pos));
                //#else
                //$$ Vec3 offset = match.state().getOffset(level, pos);
                //$$ pose.translate(offset.x, offset.y, offset.z);
                //#endif
                //$$ client.getBlockRenderer().renderSingleBlock(match.state(), pose, outlines, 15728880, 0);
                //$$ pose.popPose();
            //$$ }
            //#if MC <= 1.21.8
            //$$ BlockEntity blockEntity = level.getBlockEntity(pos);
            //$$ if (blockEntity != null)
            //$$ {
                //$$ client.getBlockEntityRenderDispatcher().render(blockEntity, 0.0F, pose, outlines);
            //$$ }
            //#endif
            //$$ if (!match.state().getFluidState().isEmpty())
            //$$ {
                //#if MC >= 1.21.11
                //$$ VertexConsumer vertices = outlines.getBuffer(RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS));
                //#else
                //$$ VertexConsumer vertices = outlines.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS));
                //#endif
                //$$ Vec3 origin = new Vec3((pos.getX() & ~15) - camera.x,
                        //$$ (pos.getY() & ~15) - camera.y, (pos.getZ() & ~15) - camera.z);
                //$$ client.getBlockRenderer().renderLiquid(pos, level,
                        //$$ new LegacyFluidOutlineVertices(vertices, origin, color),
                        //$$ match.state(), match.state().getFluidState());
            //$$ }
            //$$ pose.popPose();
        //$$ }
    //$$ }

    //$$ private static final class LegacyFluidOutlineVertices implements VertexConsumer
    //$$ {
        //$$ private final VertexConsumer output;
        //$$ private final Vec3 origin;
        //$$ private final int color;

        //$$ private LegacyFluidOutlineVertices(VertexConsumer output, Vec3 origin, int color)
        //$$ {
            //$$ this.output = output;
            //$$ this.origin = origin;
            //$$ this.color = color;
        //$$ }

        //$$ @Override public VertexConsumer addVertex(float x, float y, float z)
        //$$ {
            //$$ this.output.addVertex((float) (x + this.origin.x),
                    //$$ (float) (y + this.origin.y), (float) (z + this.origin.z));
            //$$ return this;
        //$$ }
        //$$ @Override public VertexConsumer setColor(int red, int green, int blue, int alpha)
        //$$ {
            //$$ this.output.setColor(this.color);
            //$$ return this;
        //$$ }
        //$$ @Override public VertexConsumer setColor(int color)
        //$$ {
            //$$ this.output.setColor(this.color);
            //$$ return this;
        //$$ }
        //$$ @Override public VertexConsumer setUv(float u, float v) { this.output.setUv(u, v); return this; }
        //$$ @Override public VertexConsumer setUv1(int u, int v) { this.output.setUv1(u, v); return this; }
        //$$ @Override public VertexConsumer setUv2(int u, int v) { this.output.setUv2(u, v); return this; }
        //$$ @Override public VertexConsumer setNormal(float x, float y, float z)
        //$$ {
            //$$ this.output.setNormal(x, y, z);
            //$$ return this;
        //$$ }
        //#if MC >= 1.21.11
        //$$ @Override public VertexConsumer setLineWidth(float width)
        //$$ {
            //$$ this.output.setLineWidth(width);
            //$$ return this;
        //$$ }
        //#endif
    //$$ }
    //#endif

    private record Match(BlockPos pos, BlockState state, int color, double distanceSquared) {}

    private record Snapshot(ClientLevel level, List<Match> matches, Map<BlockPos, Integer> colors)
    {
        private static final Snapshot EMPTY = new Snapshot(null, List.of(), Map.of());
    }

    private static final class SectionScan
    {
        private final SectionPos pos;
        private int index;

        private SectionScan(SectionPos pos) { this.pos = pos; }
    }

    //#if MC >= 26.1
    public void submitOutlines(LevelRenderState renderState, SubmitNodeCollector collector)
    {
        Snapshot current = this.snapshot;
        List<Match> visible = current.matches();
        if (!Configs.BLOCK_GLOW_HIGHLIGHT.getBooleanValue() || visible.isEmpty()
                || current.level() != Minecraft.getInstance().level || renderState.cameraRenderState == null) return;

        PoseStack pose = new PoseStack();
        Vec3 camera = renderState.cameraRenderState.pos;
        ClientLevel activeLevel = current.level();
        FluidRenderer fluidRenderer = new FluidRenderer(Minecraft.getInstance().getModelManager().getFluidStateModelSet());
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (this.cachedModelSet != modelSet)
        {
            this.cachedModelSet = modelSet;
            this.modelPartsCache.clear();
        }
        RandomSource random = RandomSource.createThreadLocalInstance();
        boolean submitted = false;
        for (Match match : visible)
        {
            BlockPos pos = match.pos();
            if (match.state().getRenderShape() == RenderShape.MODEL)
            {
                ModelKey key = new ModelKey(match.state(), match.state().getSeed(pos));
                List<BlockStateModelPart> parts = this.modelPartsCache.get(key);
                if (parts == null)
                {
                    BlockStateModel model = modelSet.get(match.state());
                    random.setSeed(key.seed());
                    List<BlockStateModelPart> collected = new ArrayList<>();
                    model.collectParts(random, collected);
                    parts = List.copyOf(collected);
                    if (this.modelPartsCache.size() >= 2048) this.modelPartsCache.clear();
                    this.modelPartsCache.put(key, parts);
                }
                if (!parts.isEmpty())
                {
                    pose.pushPose();
                    pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                    pose.translate(match.state().getOffset(pos));
                    collector.submitBlockModel(pose, RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS),
                            parts, new int[0], 15728880, 0, match.color());
                    pose.popPose();
                    submitted = true;
                }
            }

            if (!match.state().getFluidState().isEmpty())
            {
                pose.pushPose();
                pose.translate((pos.getX() & ~15) - camera.x,
                        (pos.getY() & ~15) - camera.y, (pos.getZ() & ~15) - camera.z);
                collector.submitCustomGeometry(pose, RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS),
                        (matrix, vertices) -> fluidRenderer.tesselate(activeLevel, pos,
                                layer -> new FluidOutlineVertices(vertices, matrix.pose(), match.color()),
                                match.state(), match.state().getFluidState()));
                pose.popPose();
                submitted = true;
            }
        }
        if (submitted || !visible.isEmpty())
        {
            //#if MC >= 26.2
            renderState.shouldShowEntityOutlines = true;
            //#else
            //$$ renderState.haveGlowingEntities = true;
            //#endif
        }
    }

    private record ModelKey(BlockState state, long seed) {}

    private static final class FluidOutlineVertices implements VertexConsumer
    {
        private final VertexConsumer output;
        private final Matrix4fc transform;
        private final int color;
        private final Vector3f position = new Vector3f();

        private FluidOutlineVertices(VertexConsumer output, Matrix4fc transform, int color)
        {
            this.output = output;
            this.transform = transform;
            this.color = color;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            this.transform.transformPosition(x, y, z, this.position);
            this.output.addVertex(this.position.x, this.position.y, this.position.z);
            return this;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha)
        {
            this.output.setColor(this.color);
            return this;
        }

        @Override public VertexConsumer setColor(int color)
        {
            this.output.setColor(this.color);
            return this;
        }

        @Override public VertexConsumer setUv(float u, float v) { this.output.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { this.output.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { this.output.setUv2(u, v); return this; }
        //#if MC >= 26.3
        @Override public VertexConsumer setUv3(float u, float v) { this.output.setUv3(u, v); return this; }
        //#endif
        @Override public VertexConsumer setNormal(float x, float y, float z)
        {
            this.output.setNormal(x, y, z);
            return this;
        }
        @Override public VertexConsumer setLineWidth(float width)
        {
            this.output.setLineWidth(width);
            return this;
        }
    }
    //#endif
}
//#endif
