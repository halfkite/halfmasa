package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** GUI-only presentation; stored block state and placement data remain unchanged. */
public final class SpawnerItemPresentation
{
    public static final float ICON_PIXEL = 1.0F / 16.0F;
    private static final float MOB_ENLARGEMENT = 1.20F;
    private static final Identifier SLIME = Identifier.withDefaultNamespace("slime");
    private SpawnerItemPresentation() {}

    public static float guiScale(EntityType<?> type, float nativeScale)
    {
        return SLIME.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)) ? nativeScale : nativeScale * MOB_ENLARGEMENT;
    }

    public static Matrix4f foregroundTransform(ItemTransform itemTransform, Matrix4fc localTransform,
                                               float width, float height, float scale, float cageFront)
    {
        var pose = new PoseStack();
        itemTransform.apply(false, pose.last());
        Matrix4f transform = new Matrix4f(pose.last().pose()).mul(localTransform);
        // Use the cage's center and size, but not its isometric rotation: GUI mobs stand
        // upright and face the viewer instead of inheriting the spawner's tilted pose.
        Vector3f center = transform.transformPosition(new Vector3f(0.5F));
        Vector3f itemScale = transform.getScale(new Vector3f());
        float guiScale = scale * Math.min(itemScale.x, Math.min(itemScale.y, itemScale.z));
        float depth = Math.max(center.z, cageFront + width * guiScale * 0.5F) + ICON_PIXEL;
        // NO_TRANSFORM still centers the layer. Compensate once so the standing model
        // remains centered in the cage icon and entirely in front of its geometry.
        return new Matrix4f().translation(center.x + 0.5F,
                center.y - height * guiScale * 0.5F + 0.5F, depth + 0.5F).scale(guiScale);
    }
}
//#endif
