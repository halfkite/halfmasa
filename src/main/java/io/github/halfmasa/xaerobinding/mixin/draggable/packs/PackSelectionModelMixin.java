package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.duck.ResourcePackOrganizerDuckProvider;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
//#if MC >= 1.21.10
import java.util.function.Consumer;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin implements ResourcePackOrganizerDuckProvider {
    @Shadow
    @Final
    //#if MC >= 1.21.10
    Consumer<PackSelectionModel.EntryBase> onListChanged;
    //#else
    //$$ Runnable onListChanged;
    //#endif

    @Override
    public void draggable_lists$updateSelectedList() {
        //#if MC >= 1.21.10
        onListChanged.accept(null);
        //#else
        //$$ onListChanged.run();
        //#endif
    }
}
