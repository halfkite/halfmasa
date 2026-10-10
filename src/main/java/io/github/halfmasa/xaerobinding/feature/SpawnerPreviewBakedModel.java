package io.github.halfmasa.xaerobinding.feature;

//#if MC < 1.21.5
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** A transform-free holder for legacy special layers; geometry stays in the cage layer. */
public record SpawnerPreviewBakedModel(BakedModel original) implements BakedModel
{
    @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) { return List.of(); }
    @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return original.isGui3d(); }
    @Override public boolean usesBlockLight() { return true; }
    @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
    //#if MC < 1.21.4
    //$$ @Override public boolean isCustomRenderer() { return false; }
    //#if MC >= 1.21.3
    //$$ @Override public net.minecraft.client.renderer.block.model.BakedOverrides overrides() { return original.overrides(); }
    //#else
    //$$ @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return original.getOverrides(); }
    //#endif
    //#endif
    @Override public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

    public static float front(BakedModel model, ItemTransform transform)
    {
        var poses = new PoseStack();
        transform.apply(false, poses);
        poses.translate(-0.5F, -0.5F, -0.5F);
        float front = Float.NEGATIVE_INFINITY;
        var faces = new java.util.ArrayList<Direction>();
        faces.add(null); java.util.Collections.addAll(faces, Direction.values());
        for (Direction face : faces)
            for (BakedQuad quad : model.getQuads(null, face, RandomSource.create(0)))
            {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++)
                {
                    int offset = vertex * stride;
                    var point = new Vector3f(Float.intBitsToFloat(vertices[offset]), Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2]));
                    front = Math.max(front, poses.last().pose().transformPosition(point).z);
                }
            }
        return front;
    }
}
//#endif
