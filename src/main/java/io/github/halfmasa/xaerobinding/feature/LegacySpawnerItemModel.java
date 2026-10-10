package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.4 && MC < 1.21.10
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import io.github.halfmasa.xaerobinding.mixin.SpawnerItemLayerAccessor;
import io.github.halfmasa.xaerobinding.mixin.SpawnerItemRenderStateAccessor;

/** Native buffer-rendering equivalent of the newer submitted item preview. */
public final class LegacySpawnerItemModel implements ItemModel
{
    private static final int MAX_PREVIEWS = 128;
    private static final AtomicInteger PREVIEW_IDS = new AtomicInteger(Integer.MIN_VALUE);
    private static final Map<CompoundTag, Entity> PREVIEWS = new LinkedHashMap<>(MAX_PREVIEWS, 0.75F, true);
    private static ClientLevel previewLevel;
    private static final Vector3f[] EXTENTS = {new Vector3f(0), new Vector3f(1)};
    private static final PreviewRenderer RENDERER = new PreviewRenderer();
    private final ItemModel cage;
    private final SpawnerItemAppearance.Info info;

    public LegacySpawnerItemModel(ItemModel cage, SpawnerItemAppearance.Info info)
    { this.cage = cage; this.info = info; }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext context, ClientLevel level, LivingEntity owner, int seed)
    {
        var layers = (SpawnerItemRenderStateAccessor) state;
        int first = layers.halfmasa$getActiveLayerCount();
        cage.update(state, stack, resolver, context, level, owner, seed);
        //#if MC >= 1.21.8
        state.appendModelIdentityElement(info.model());
        //#endif
        if (layers.halfmasa$getActiveLayerCount() <= first || level == null || info.mobs().isEmpty()) return;
        Entity entity = preview(info.mobs().getFirst(), level);
        if (entity == null) return;
        boolean gui = context == ItemDisplayContext.GUI;
        float[] front = {Float.NEGATIVE_INFINITY};
        //#if MC >= 1.21.5
        if (gui) state.visitExtents(point -> front[0] = Math.max(front[0], point.z()));
        //#endif
        // Match the native SpawnerRenderer's scale before applying the requested GUI enlargement.
        float scale = 0.53125F / Math.max(1, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        var base = (SpawnerItemLayerAccessor) layers.halfmasa$getLayers()[first];
        //#if MC >= 1.21.5
        ItemTransform itemTransform = base.halfmasa$getItemTransform();
        //#else
        //$$ var baseModel = base.halfmasa$getModel();
        //$$ ItemTransform itemTransform = baseModel.getTransforms().getTransform(context);
        //$$ if (gui) front[0] = SpawnerPreviewBakedModel.front(baseModel, itemTransform);
        //#endif
        var layer = state.newLayer();
        Matrix4f transform = null;
        EntityRenderState renderState = null;
        if (gui && Float.isFinite(front[0]))
        {
            //#if MC >= 1.21.5
            layer.setTransform(ItemTransform.NO_TRANSFORM);
            //#endif
            transform = SpawnerItemPresentation.foregroundTransform(itemTransform, new Matrix4f(),
                    entity.getBbWidth(), entity.getBbHeight(), SpawnerItemPresentation.guiScale(entity.getType(), scale), front[0]);
            renderState = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 0);
            renderState.nameTag = null;
            //#if MC >= 1.21.8
            renderState.leashStates = null;
            //#else
            //$$ renderState.leashState = null;
            //#endif
            renderState.displayFireAnimation = false;
        }
        //#if MC >= 1.21.5
        else layer.setTransform(itemTransform);
        layer.setUsesBlockLight(true);
        layer.setExtents(() -> EXTENTS);
        layer.setupSpecialModel(RENDERER, new Preview(entity, renderState, transform));
        //#else
        //$$ layer.setupSpecialModel(RENDERER, new Preview(entity, renderState, transform), transform != null ? new SpawnerPreviewBakedModel(baseModel) : baseModel);
        //#endif
        //#if MC >= 1.21.8
        state.appendModelIdentityElement(info.mobs().getFirst());
        //#endif
    }

    private static Entity preview(CompoundTag data, ClientLevel level)
    {
        clearIfWorldChanged(level);
        Entity cached = PREVIEWS.get(data);
        if (cached != null) return cached;
        CompoundTag copy = data.copy();
        for (String field : new String[]{"Passengers", "UUID", "Pos", "Motion", "Rotation"}) copy.remove(field);
        Entity entity = EntityType.loadEntityRecursive(copy, level, EntitySpawnReason.SPAWNER, value -> {
            value.setId(PREVIEW_IDS.getAndIncrement()); return value;
        });
        if (entity != null)
        {
            if (PREVIEWS.size() >= MAX_PREVIEWS) PREVIEWS.remove(PREVIEWS.keySet().iterator().next());
            PREVIEWS.put(data.copy(), entity);
        }
        return entity;
    }

    public static void clearIfWorldChanged(ClientLevel level)
    {
        if (previewLevel != level) { PREVIEWS.clear(); previewLevel = level; }
    }

    //#if MC < 1.21.5
    //$$ @SuppressWarnings("unchecked")
    //$$ private static void renderPrepared(Entity entity, EntityRenderState state, PoseStack poses, MultiBufferSource buffers, int light) {
    //$$     // This state was created by the renderer of this same cached preview entity.
    //$$     var renderer = (net.minecraft.client.renderer.entity.EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    //$$     renderer.render(state, poses, buffers, light);
    //$$ }
    //#endif
    private record Preview(Entity entity, EntityRenderState renderState, Matrix4f transform) {}
    private static final class PreviewRenderer implements SpecialModelRenderer<Preview>
    {
        @Override public void render(Preview preview, ItemDisplayContext context, PoseStack poses,
                                     MultiBufferSource buffers, int light, int overlay, boolean foil)
        {
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            if (preview.transform() != null)
            {
                poses.pushPose();
                poses.mulPose(preview.transform());
                //#if MC >= 1.21.5
                dispatcher.getRenderer(preview.renderState()).render(preview.renderState(), poses, buffers, light);
                //#else
                //$$ renderPrepared(preview.entity(), preview.renderState(), poses, buffers, light);
                //#endif
                poses.popPose();
            }
            else SpawnerRenderer.renderEntityInSpawner(0, poses, buffers, light, preview.entity(), dispatcher, 0, 0);
        }
        //#if MC >= 1.21.8
        @Override public void getExtents(Set<Vector3f> points) { java.util.Collections.addAll(points, EXTENTS); }
        //#endif
        @Override public Preview extractArgument(ItemStack stack) { return null; }
    }
}
//#endif
