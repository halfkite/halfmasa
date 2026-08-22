package io.github.halfmasa.xaerobinding.mixin.draggable.packs;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//#endif
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {
    @Shadow
    private TransferableSelectionList selectedPackList;

    @Shadow
    private TransferableSelectionList availablePackList;

    @Shadow @Final private PackSelectionModel model;

    @Shadow protected abstract void closeWatcher();

    protected PackSelectionScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void onClose() {
        this.halfmasa$releaseDrag(selectedPackList);
        this.halfmasa$releaseDrag(availablePackList);

        // from PackSelectionScreen::onClose()
        this.model.commit();
        this.closeWatcher();
    }

    private void halfmasa$releaseDrag(TransferableSelectionList list)
    {
        if (!list.isDragging()) return;
        //#if MC >= 1.21.10
        list.mouseReleased(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, 0)));
        //#else
        //$$ list.mouseReleased(0, 0, 0);
        //#endif
    }
}
