package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.TrialSpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import io.github.halfmasa.xaerobinding.mixin.SpawnerItemLayerAccessor;
import io.github.halfmasa.xaerobinding.mixin.SpawnerItemRenderStateAccessor;

/** Adds the native spawner's contained-entity rendering to its normal block model. */
public final class SpawnerItemModel implements ItemModel
{
    private static final int MAX_PREVIEW_ENTITIES = 128;
    // Display entities are outside the server's positive entity-ID space used by aggregation.
    private static final AtomicInteger PREVIEW_IDS = new AtomicInteger(Integer.MIN_VALUE);
    private static final Map<CompoundTag, Entity> PREVIEWS = new LinkedHashMap<>(MAX_PREVIEW_ENTITIES, 0.75F, true);
    private static ClientLevel previewLevel;
    private static final PreviewRenderer PREVIEW_RENDERER = new PreviewRenderer(false);
    private static final PreviewRenderer GUI_PREVIEW_RENDERER = new PreviewRenderer(true);
    private static final Vector3fc[] EXTENTS = {new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)};
    private final ItemModel cage;
    private final SpawnerItemAppearance.Info info;

    public SpawnerItemModel(ItemModel cage, SpawnerItemAppearance.Info info)
    {
        this.cage = cage;
        this.info = info;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext context, ClientLevel level, ItemOwner owner, int seed)
    {
        var layers = (SpawnerItemRenderStateAccessor) state;
        int firstLayer = layers.halfmasa$getActiveLayerCount();
        this.cage.update(state, stack, resolver, context, level, owner, seed);
        // Include the state model explicitly so identical geometry cannot share the GUI icon.
        state.appendModelIdentityElement(this.info.model());
        if (layers.halfmasa$getActiveLayerCount() <= firstLayer) return;
        boolean gui = context == ItemDisplayContext.GUI;
        float[] front = {Float.NEGATIVE_INFINITY};
        if (gui) state.visitExtents(point -> front[0] = Math.max(front[0], point.z()));
        if (level == null || this.info.mobs().isEmpty()) return;
        Entity entity = preview(this.info.mobs().getFirst(), level);
        if (entity == null) return;
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var preview = new SpawnerRenderState();
        TrialSpawnerRenderer.extractSpawnerData(preview, 0, entity, dispatcher, 0, 0);
        if (preview.displayEntity == null) return;
        if (gui) preview.scale = SpawnerItemPresentation.guiScale(entity.getType(), preview.scale);
        preview.displayEntity.nameTag = null;
        preview.displayEntity.scoreText = null;
        preview.displayEntity.shadowRadius = 0;
        preview.displayEntity.shadowPieces.clear();
        preview.displayEntity.leashStates = null;
        preview.displayEntity.displayFireAnimation = false;
        var base = (SpawnerItemLayerAccessor) layers.halfmasa$getLayers()[firstLayer];
        var layer = state.newLayer();
        boolean foreground = gui && Float.isFinite(front[0]);
        if (foreground)
        {
            layer.setItemTransform(ItemTransform.NO_TRANSFORM);
            layer.setLocalTransform(SpawnerItemPresentation.foregroundTransform(base.halfmasa$getItemTransform(),
                    base.halfmasa$getLocalTransform(), entity.getBbWidth(), entity.getBbHeight(), preview.scale, front[0]));
        }
        else
        {
            layer.setItemTransform(base.halfmasa$getItemTransform());
            layer.setLocalTransform(base.halfmasa$getLocalTransform());
        }
        layer.setUsesBlockLight(true);
        layer.setExtents(() -> EXTENTS);
        layer.setupSpecialModel(foreground ? GUI_PREVIEW_RENDERER : PREVIEW_RENDERER, preview);
        // GUI atlas entries must distinguish entity NBT such as baby/slime/armor variants.
        state.appendModelIdentityElement(this.info.mobs().getFirst());
    }

    private static Entity preview(CompoundTag data, ClientLevel level)
    {
        clearIfWorldChanged(level);
        Entity cached = PREVIEWS.get(data);
        if (cached != null) return cached;
        CompoundTag copy = data.copy();
        // Preview only the occupant, without spawning its passenger tree or retaining world positions.
        for (String field : new String[]{"Passengers", "UUID", "Pos", "Motion", "Rotation"}) copy.remove(field);
        Entity entity = EntityType.loadEntityRecursive(copy, level,
                new EntitySpawnRequest(EntitySpawnReason.SPAWNER, true), entityToProcess -> {
                    entityToProcess.setId(PREVIEW_IDS.getAndIncrement());
                    return entityToProcess;
                });
        if (entity != null)
        {
            if (PREVIEWS.size() >= MAX_PREVIEW_ENTITIES) PREVIEWS.remove(PREVIEWS.keySet().iterator().next());
            PREVIEWS.put(data.copy(), entity);
        }
        return entity;
    }

    public static void clearIfWorldChanged(ClientLevel level)
    {
        if (previewLevel != level)
        {
            PREVIEWS.clear();
            previewLevel = level;
        }
    }

    private static final class PreviewRenderer implements SpecialModelRenderer<SpawnerRenderState>
    {
        private final boolean gui;

        private PreviewRenderer(boolean gui) { this.gui = gui; }

        @Override
        public void submit(SpawnerRenderState state, PoseStack poses, SubmitNodeCollector collector,
                           int light, int overlay, boolean foil, int outline)
        {
            var client = Minecraft.getInstance();
            var camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            state.displayEntity.lightCoords = light;
            if (this.gui)
            {
                client.getEntityRenderDispatcher().submit(state.displayEntity, camera, 0, 0, 0, poses, collector);
            }
            else
            {
                SpawnerRenderer.submitEntityInSpawner(poses, collector, state.displayEntity,
                        client.getEntityRenderDispatcher(), state.spin, state.scale, camera);
            }
        }

        @Override public void getExtents(Consumer<Vector3fc> consumer) { for (var point : EXTENTS) consumer.accept(point); }
        @Override public SpawnerRenderState extractArgument(ItemStack stack) { return null; }
    }

}
//#endif
