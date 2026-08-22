package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.draggable.Cursor;

@Mixin(Minecraft.class)
public abstract class DraggableListsMinecraftMixin
{
    //#if MC >= 26.2
    @Inject(method = "setScreenAndShow", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "setScreen", at = @At("HEAD"))
    //#endif
    private void halfmasa_resetDragCursor(Screen screen, CallbackInfo ci)
    {
        Cursor.reset();
    }
}
