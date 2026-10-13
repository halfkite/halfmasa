package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import io.github.halfmasa.xaerobinding.config.Configs;

/** Bounded client-side scan and vanilla outline submission for configured blocks. */
public final class BlockGlowService implements IClientTickHandler
{
    private static final int RADIUS = 16;
    private static final int DIAMETER = RADIUS * 2 + 1;
    private static final int SCAN_VOLUME = DIAMETER * DIAMETER * DIAMETER;
    private static final int SCAN_BUDGET = 1024;
    private static final int MAX_OUTLINES = 512;
    private static final List<BlockPos> OFFSETS = createOffsets();
    private static final BlockGlowService INSTANCE = new BlockGlowService();

    private final Map<BlockPos, Match> matches = new HashMap<>();
    private volatile List<Match> snapshot = List.of();
    private volatile Map<BlockPos, Integer> snapshotColors = Map.of();
    //#if MC >= 26.1
    private final Map<ModelKey, List<BlockStateModelPart>> modelPartsCache = new HashMap<>();
    private BlockStateModelSet cachedModelSet;
    //#endif
    private Map<String, Integer> colors = Map.of();
    private volatile ClientLevel level;
    private BlockPos center;
    private int cursor;

    private BlockGlowService() {}

    public static BlockGlowService getInstance() { return INSTANCE; }

    private static List<BlockPos> createOffsets()
    {
        List<BlockPos> offsets = new ArrayList<>(SCAN_VOLUME);
        for (int x = -RADIUS; x <= RADIUS; x++)
        {
            for (int y = -RADIUS; y <= RADIUS; y++)
            {
                for (int z = -RADIUS; z <= RADIUS; z++)
                {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.sort(Comparator.comparingLong(pos ->
                (long) pos.getX() * pos.getX() + (long) pos.getY() * pos.getY()
                        + (long) pos.getZ() * pos.getZ()));
        return List.copyOf(offsets);
    }

    public void reload()
    {
        Map<String, Integer> updated = new HashMap<>();
        for (EntityGlowRule rule : EntityGlowRule.loadAll())
        {
            if (rule.enabled() && rule.type() == EntityGlowRule.TargetType.BLOCK)
            {
                updated.put(rule.id().toString(), rule.color());
            }
        }
        this.colors = Map.copyOf(updated);
        this.matches.clear();
        this.snapshot = List.of();
        this.snapshotColors = Map.of();
        this.cursor = 0;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        ClientLevel currentLevel = client.level;
        if (currentLevel == null || client.player == null || !Configs.ENTITY_GLOW_HIGHLIGHT.getBooleanValue())
        {
            this.level = currentLevel;
            this.center = null;
            this.matches.clear();
            this.snapshot = List.of();
            this.snapshotColors = Map.of();
            this.cursor = 0;
            return;
        }
        if (this.colors.isEmpty())
        {
            this.matches.clear();
            this.snapshot = List.of();
            this.snapshotColors = Map.of();
            return;
        }

        BlockPos playerPos = client.player.blockPosition();
        if (this.level != currentLevel || this.center == null
                || Math.abs(playerPos.getX() - this.center.getX()) >= 4
                || Math.abs(playerPos.getY() - this.center.getY()) >= 4
                || Math.abs(playerPos.getZ() - this.center.getZ()) >= 4)
        {
            this.level = currentLevel;
            this.center = playerPos.immutable();
            this.matches.clear();
            this.snapshot = List.of();
            this.snapshotColors = Map.of();
            this.cursor = 0;
        }

        for (int count = 0; count < SCAN_BUDGET; count++)
        {
            int index = this.cursor++;
            if (this.cursor == SCAN_VOLUME) this.cursor = 0;
            BlockPos offset = OFFSETS.get(index);
            BlockPos pos = this.center.offset(offset);
            if (!currentLevel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            {
                this.matches.remove(pos);
                continue;
            }
            BlockState state = currentLevel.getBlockState(pos);
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            Integer color = this.colors.get(id);
            if (color == null)
            {
                this.matches.remove(pos);
            }
            else if (this.matches.size() < MAX_OUTLINES || this.matches.containsKey(pos))
            {
                this.matches.put(pos, new Match(pos, state, color));
            }
        }
        this.snapshot = List.copyOf(this.matches.values());
        Map<BlockPos, Integer> updatedColors = new HashMap<>();
        this.matches.forEach((pos, match) -> updatedColors.put(pos, match.color()));
        this.snapshotColors = Map.copyOf(updatedColors);
    }

    public int colorAt(BlockPos pos)
    {
        return this.snapshotColors.getOrDefault(pos, 0);
    }

    public boolean hasMatches()
    {
        return Configs.ENTITY_GLOW_HIGHLIGHT.getBooleanValue() && !this.snapshot.isEmpty();
    }

    //#if MC < 26.0
    //$$ /** Submit model blocks to vanilla's outline buffer immediately before it is flushed. */
    //$$ public void renderLegacyOutlines()
    //$$ {
        //$$ if (!this.hasMatches() || this.level != Minecraft.getInstance().level) return;
        //$$ Minecraft client = Minecraft.getInstance();
        //#if MC >= 1.21.10
        //$$ Vec3 camera = client.gameRenderer.getMainCamera().position();
        //#else
        //$$ Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
        //#endif
        //$$ OutlineBufferSource outlines = client.renderBuffers().outlineBufferSource();
        //$$ PoseStack pose = new PoseStack();
        //$$ for (Match match : this.snapshot)
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
                //$$ Vec3 offset = match.state().getOffset(this.level, pos);
                //$$ pose.translate(offset.x, offset.y, offset.z);
                //#endif
                //$$ client.getBlockRenderer().renderSingleBlock(match.state(), pose, outlines, 15728880, 0);
                //$$ pose.popPose();
            //$$ }
            //#if MC <= 1.21.8
            //$$ BlockEntity blockEntity = this.level.getBlockEntity(pos);
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
                //$$ client.getBlockRenderer().renderLiquid(pos, this.level,
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

    private record Match(BlockPos pos, BlockState state, int color) {}

    //#if MC >= 26.1
    public void submitOutlines(LevelRenderState renderState, SubmitNodeCollector collector)
    {
        List<Match> visible = this.snapshot;
        if (!Configs.ENTITY_GLOW_HIGHLIGHT.getBooleanValue() || visible.isEmpty()
                || renderState.cameraRenderState == null) return;

        PoseStack pose = new PoseStack();
        Vec3 camera = renderState.cameraRenderState.pos;
        ClientLevel activeLevel = this.level;
        FluidRenderer fluidRenderer = activeLevel == Minecraft.getInstance().level
                ? new FluidRenderer(Minecraft.getInstance().getModelManager().getFluidStateModelSet()) : null;
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

            if (fluidRenderer != null && !match.state().getFluidState().isEmpty())
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
