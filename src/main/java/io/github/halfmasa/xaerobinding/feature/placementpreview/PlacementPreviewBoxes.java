package io.github.halfmasa.xaerobinding.feature.placementpreview;

//#if MC >= 1.21.1
import net.minecraft.world.phys.AABB;
//#if MC >= 1.21.11
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$ import net.minecraft.client.renderer.RenderType;
//#if MC >= 1.21.3
//$$ import net.minecraft.client.renderer.ShapeRenderer;
//$$ import net.minecraft.util.ARGB;
//#else
//$$ import net.minecraft.client.renderer.LevelRenderer;
//$$ import net.minecraft.util.FastColor;
//#endif
//#if MC >= 1.21.10
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//#else
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//$$ import net.minecraft.world.phys.Vec3;
//#endif

final class PlacementPreviewBoxes
{
    private PlacementPreviewBoxes() {}

    static void draw(AABB bounds, int outline) { draw(bounds, outline, 0); }

    static void draw(AABB bounds, int outline, int fill)
    {
        //#if MC >= 1.21.11
        Gizmos.cuboid(bounds, fill == 0 ? GizmoStyle.stroke(outline) : GizmoStyle.strokeAndFill(outline, 1F, fill));
        //#else
        //$$ boxes.add(new Box(bounds, outline, fill));
        //#endif
    }

    //#if MC < 1.21.11
    //$$ private record Box(AABB bounds, int outline, int fill) {}
    //$$ private static final List<Box> boxes = new ArrayList<>();
    //$$
    //$$ static void begin() { boxes.clear(); }
    //$$
    //#if MC >= 1.21.10
    //$$ static void submit(Vec3 camera, SubmitNodeCollector collector)
    //$$ {
    //$$     if (boxes.isEmpty()) return;
    //$$     var frame = List.copyOf(boxes);
    //$$     var pose = new PoseStack();
    //$$     pose.translate(-camera.x, -camera.y, -camera.z);
    //$$     collector.submitCustomGeometry(pose, RenderType.lines(), (matrix, vertices) -> {
    //$$         for (Box box : frame) {
    //$$             int color = box.outline();
    //$$             ShapeRenderer.renderLineBox(matrix, vertices, box.bounds(), ARGB.redFloat(color),
    //$$                     ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
    //$$         }
    //$$     });
    //$$     collector.submitCustomGeometry(pose, RenderType.debugFilledBox(), (matrix, vertices) -> {
    //$$         for (Box box : frame) {
    //$$             int color = box.fill();
    //$$             if (color == 0) continue;
    //$$             AABB bounds = box.bounds();
    //$$             ShapeRenderer.addChainedFilledBoxVertices(pose, vertices, bounds.minX, bounds.minY, bounds.minZ,
    //$$                     bounds.maxX, bounds.maxY, bounds.maxZ, ARGB.redFloat(color), ARGB.greenFloat(color),
    //$$                     ARGB.blueFloat(color), ARGB.alphaFloat(color));
    //$$         }
    //$$     });
    //$$ }
    //#else
    //$$ static void render(Vec3 camera, MultiBufferSource.BufferSource buffers)
    //$$ {
    //$$     if (boxes.isEmpty()) return;
    //$$     var pose = new PoseStack();
    //$$     pose.translate(-camera.x, -camera.y, -camera.z);
    //$$     var fills = buffers.getBuffer(RenderType.debugFilledBox());
    //$$     for (Box box : boxes) {
    //$$         int color = box.fill();
    //$$         if (color == 0) continue;
    //$$         AABB bounds = box.bounds();
    //#if MC >= 1.21.3
    //$$         ShapeRenderer.addChainedFilledBoxVertices(pose, fills, bounds.minX, bounds.minY, bounds.minZ,
    //#if MC >= 1.21.4
    //$$                 bounds.maxX, bounds.maxY, bounds.maxZ, ARGB.redFloat(color), ARGB.greenFloat(color),
    //$$                 ARGB.blueFloat(color), ARGB.alphaFloat(color));
    //#else
    //$$                 bounds.maxX, bounds.maxY, bounds.maxZ, ARGB.red(color) / 255F, ARGB.green(color) / 255F,
    //$$                 ARGB.blue(color) / 255F, ARGB.alpha(color) / 255F);
    //#endif
    //#else
    //$$         LevelRenderer.addChainedFilledBoxVertices(pose, fills, bounds.minX, bounds.minY, bounds.minZ,
    //$$                 bounds.maxX, bounds.maxY, bounds.maxZ, FastColor.ARGB32.red(color) / 255F,
    //$$                 FastColor.ARGB32.green(color) / 255F, FastColor.ARGB32.blue(color) / 255F,
    //$$                 FastColor.ARGB32.alpha(color) / 255F);
    //#endif
    //$$     }
    //$$     buffers.endBatch(RenderType.debugFilledBox());
    //$$     var outlines = buffers.getBuffer(RenderType.lines());
    //$$     for (Box box : boxes) {
    //$$         int color = box.outline();
    //#if MC >= 1.21.3
    //#if MC >= 1.21.4
    //$$         ShapeRenderer.renderLineBox(pose, outlines, box.bounds(), ARGB.redFloat(color),
    //$$                 ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
    //#else
    //$$         ShapeRenderer.renderLineBox(pose, outlines, box.bounds(), ARGB.red(color) / 255F,
    //$$                 ARGB.green(color) / 255F, ARGB.blue(color) / 255F, ARGB.alpha(color) / 255F);
    //#endif
    //#else
    //$$         LevelRenderer.renderLineBox(pose, outlines, box.bounds(), FastColor.ARGB32.red(color) / 255F,
    //$$                 FastColor.ARGB32.green(color) / 255F, FastColor.ARGB32.blue(color) / 255F,
    //$$                 FastColor.ARGB32.alpha(color) / 255F);
    //#endif
    //$$     }
    //$$     buffers.endBatch(RenderType.lines());
    //$$ }
    //#endif
    //#endif
}
//#endif
