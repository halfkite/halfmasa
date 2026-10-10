package io.github.halfmasa.xaerobinding.feature;

//#if MC < 1.21.4
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
//#if MC >= 1.21.3
import net.minecraft.world.entity.EntitySpawnReason;
//#endif
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Render stateful vanilla cages and standing occupants before the ItemModel API existed. */
public final class BakedSpawnerPreview
{
    private static final int MAX_PREVIEWS = 128;
    private static final AtomicInteger PREVIEW_IDS = new AtomicInteger(Integer.MIN_VALUE);
    private static final Map<CompoundTag, Entity> PREVIEWS = new LinkedHashMap<>(MAX_PREVIEWS, 0.75F, true);
    private static ClientLevel previewLevel;
    private BakedSpawnerPreview() {}

    public static boolean supports(ItemStack stack)
    {
        return SpawnerItemAppearance.supports(stack) && stack.get(DataComponents.CUSTOM_MODEL_DATA) == null;
    }

    public static BakedModel cageModel(ItemStack stack)
    {
        var block = stack.is(Items.TRIAL_SPAWNER) ? Blocks.TRIAL_SPAWNER : stack.is(Items.VAULT) ? Blocks.VAULT : Blocks.SPAWNER;
        var properties = stack.get(DataComponents.BLOCK_STATE);
        var state = properties != null ? properties.apply(block.defaultBlockState()) : block.defaultBlockState();
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                              MultiBufferSource buffers, int light, BakedModel cage)
    {
        Minecraft client = Minecraft.getInstance();
        if (context != ItemDisplayContext.GUI || client.level == null || !supports(stack)) return;
        var info = SpawnerItemAppearance.read(stack, client.level.registryAccess(), client.level.getGameTime());
        if (info.mobs().isEmpty()) return;
        Entity entity = preview(info.mobs().getFirst(), client.level);
        if (entity == null) return;
        var transform = cage.getTransforms().getTransform(context);
        float front = SpawnerPreviewBakedModel.front(cage, transform);
        if (!Float.isFinite(front)) return;
        float scale = 0.53125F / Math.max(1, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        poses.pushPose();
        // This legacy hook runs after the native layer popped its transform.
        poses.translate(-0.5F, -0.5F, -0.5F);
        poses.mulPose(SpawnerItemPresentation.foregroundTransform(transform, new org.joml.Matrix4f(),
                entity.getBbWidth(), entity.getBbHeight(), SpawnerItemPresentation.guiScale(entity.getType(), scale), front));
        var renderer = client.getEntityRenderDispatcher().getRenderer(entity);
        //#if MC >= 1.21.3
        var state = renderer.createRenderState(entity, 0);
        state.nameTag = null;
        state.leashState = null;
        state.displayFireAnimation = false;
        ((net.minecraft.client.renderer.entity.EntityRenderer<Entity, net.minecraft.client.renderer.entity.state.EntityRenderState>) renderer).render(state, poses, buffers, light);
        //#else
        //$$ renderer.render(entity, 0, 0, poses, buffers, light);
        //#endif
        poses.popPose();
    }

    private static Entity preview(CompoundTag data, ClientLevel level)
    {
        clearIfWorldChanged(level);
        Entity cached = PREVIEWS.get(data);
        if (cached != null) return cached;
        CompoundTag copy = data.copy();
        for (String field : new String[]{"Passengers", "UUID", "Pos", "Motion", "Rotation", "CustomName", "CustomNameVisible"}) copy.remove(field);
        Entity entity = EntityType.loadEntityRecursive(copy, level,
                //#if MC >= 1.21.3
                EntitySpawnReason.SPAWNER,
                //#endif
                value -> { value.setId(PREVIEW_IDS.getAndIncrement()); return value; });
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
}
//#endif
