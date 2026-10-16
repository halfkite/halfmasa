package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.3
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnLabelState;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemDespawnRenderStateMixin implements ItemDespawnLabelState
{
    @Unique private Label halfmasa$despawnLabel;
    @Override public Label halfmasa$getDespawnLabel() { return this.halfmasa$despawnLabel; }
    @Override public void halfmasa$setDespawnLabel(Label label) { this.halfmasa$despawnLabel = label; }
}
//#endif
