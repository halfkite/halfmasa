//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.compat;

import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** Only load this class when Fabric Loader reports that Litematica is present. */
public final class LitematicaKelpSelection {
    private LitematicaKelpSelection() {}

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX
                && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ;
        }
    }

    public static List<Bounds> snapshot() {
        var selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) return List.of();
        var boxes = new ArrayList<Bounds>();
        for (var box : selection.getAllSubRegionBoxes()) {
            var a = box.getPos1();
            var b = box.getPos2();
            if (a == null || b == null) continue;
            boxes.add(new Bounds(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
        }
        return List.copyOf(boxes);
    }
}
//#endif
