package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.duck.ResourcePackOrganizerDuckProvider;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin implements ResourcePackOrganizerDuckProvider {
    @Shadow
    @Final
    Runnable onListChanged;

    @Override
    public void draggable_lists$updateSelectedList() {
        onListChanged.run();
    }
}
