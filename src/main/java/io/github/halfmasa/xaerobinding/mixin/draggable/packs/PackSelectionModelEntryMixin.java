package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import io.github.halfmasa.xaerobinding.draggable.duck.AbstractPackDuckProvider;
import java.util.List;
//#if MC >= 1.21.10
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
//#endif
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//#if MC >= 1.21.10
@Mixin(PackSelectionModel.EntryBase.class)
//#else
//$$ @Mixin(targets = "net.minecraft.client.gui.screens.packs.PackSelectionModel$EntryBase")
//#endif
public abstract class PackSelectionModelEntryMixin implements AbstractPackDuckProvider {
    @Shadow @Final private Pack pack;

    @Shadow protected abstract List<Pack> getSelfList();

    @Shadow protected abstract void move(int offset);

    @Override
    public void draggable_lists$moveTo(String targetPackId) {
        List<Pack> packs = getSelfList();
        int currentIndex = packs.indexOf(pack);
        if (currentIndex < 0 || pack.isFixedPosition()) return;

        int requestedIndex = 0;
        while (requestedIndex < packs.size() && !packs.get(requestedIndex).getId().equals(targetPackId)) {
            requestedIndex++;
        }
        if (requestedIndex == packs.size()) return;

        int direction = Integer.compare(requestedIndex, currentIndex);
        int targetIndex = currentIndex;
        while (targetIndex != requestedIndex) {
            int nextIndex = targetIndex + direction;
            if (packs.get(nextIndex).isFixedPosition()) break;
            targetIndex = nextIndex;
        }

        if (targetIndex != currentIndex) move(targetIndex - currentIndex);
    }
}
