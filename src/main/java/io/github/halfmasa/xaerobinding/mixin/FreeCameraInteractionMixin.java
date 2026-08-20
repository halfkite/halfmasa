package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.FreeCameraInteraction;

@Mixin(Minecraft.class)
public abstract class FreeCameraInteractionMixin
{
    @Shadow private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void halfmasa_useBlockFromFreeCamera(CallbackInfo ci)
    {
        Minecraft client = (Minecraft) (Object) this;
        if (FreeCameraInteraction.tryUseTargetedBlock(client))
        {
            this.rightClickDelay = 4;
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void halfmasa_startMiningFromFreeCamera(CallbackInfoReturnable<Boolean> cir)
    {
        Minecraft client = (Minecraft) (Object) this;
        if (FreeCameraInteraction.tryStartMining(client)) cir.setReturnValue(true);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void halfmasa_continueMiningFromFreeCamera(boolean leftClick, CallbackInfo ci)
    {
        if (!leftClick) return;
        Minecraft client = (Minecraft) (Object) this;
        if (FreeCameraInteraction.tryContinueMining(client)) ci.cancel();
    }
}
