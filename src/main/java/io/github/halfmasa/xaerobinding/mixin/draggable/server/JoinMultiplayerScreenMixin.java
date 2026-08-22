package io.github.halfmasa.xaerobinding.mixin.draggable.server;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
//#endif
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Shadow
    protected ServerSelectionList serverSelectionList;

    protected JoinMultiplayerScreenMixin(Component component) {
        super(component);
    }

    //#if MC >= 1.21.10
    @Inject(method = "join", at = @At("HEAD"))
    private void injectedConnect(ServerData serverData, CallbackInfo info) {
    //#else
    //$$ @Inject(method = "joinSelectedServer", at = @At("HEAD"))
    //$$ private void injectedConnect(CallbackInfo info) {
    //#endif
        this.halfmasa$releaseDrag(0, 0, 0);
    }

    //#if MC >= 1.21.10
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (serverSelectionList.isDragging()) serverSelectionList.mouseReleased(event);
        return super.mouseReleased(event);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //$$     if (serverSelectionList.isDragging()) serverSelectionList.mouseReleased(mouseX, mouseY, button);
    //$$     return super.mouseReleased(mouseX, mouseY, button);
    //$$ }
    //#endif

    @Override
    public void onClose() {
        this.halfmasa$releaseDrag(0, 0, 0);
        super.onClose();
    }

    private void halfmasa$releaseDrag(double mouseX, double mouseY, int button)
    {
        if (!serverSelectionList.isDragging()) return;
        //#if MC >= 1.21.10
        serverSelectionList.mouseReleased(new MouseButtonEvent(
                mouseX, mouseY, new MouseButtonInfo(button, 0)));
        //#else
        //$$ serverSelectionList.mouseReleased(mouseX, mouseY, button);
        //#endif
    }
}
