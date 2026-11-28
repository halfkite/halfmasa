package io.github.halfmasa.xaerobinding.mixin;

import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC >= 26.2
import net.minecraft.client.gui.Hud;
//#else
//$$ import net.minecraft.client.gui.Gui;
//#endif
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC >= 26.2
@Mixin(Hud.class)
//#else
//$$ @Mixin(Gui.class)
//#endif
public abstract class SpectatorCrosshairMixin {
    @Inject(method = "canRenderCrosshairForSpectator", at = @At("HEAD"), cancellable = true)
    private void halfmasa$keepSpectatorCrosshair(HitResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (Configs.SPECTATOR_CROSSHAIR.getBooleanValue()) {
            cir.setReturnValue(true);
        }
    }
}
