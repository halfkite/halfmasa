package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** A render-state snapshot: submission never looks up entities in the live world. */
public interface ItemDespawnLabelState
{
    record Label(Component text, Vec3 attachment, boolean throughWalls) {}
    Label halfmasa$getDespawnLabel();
    void halfmasa$setDespawnLabel(Label label);
}
//#endif
