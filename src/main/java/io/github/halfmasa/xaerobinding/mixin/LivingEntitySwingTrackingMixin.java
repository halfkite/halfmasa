package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.SwingVisualTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC < 26.3
//$$ import net.minecraft.world.InteractionHand;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingTrackingMixin
{
    //#if MC >= 26.3
    @Inject(method = "swing", at = @At("RETURN"))
    private void halfmasa$successfulSwing(CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue()) SwingVisualTracker.started((LivingEntity)(Object)this);
    }
    //#else
    //$$ @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At(value = "FIELD",
    //$$         target = "Lnet/minecraft/world/entity/LivingEntity;swingingArm:Lnet/minecraft/world/InteractionHand;", shift = At.Shift.AFTER))
    //$$ private void halfmasa$successfulSwing(InteractionHand hand, boolean broadcast, CallbackInfo ci)
    //$$ {
    //$$     SwingVisualTracker.started((LivingEntity)(Object)this);
    //$$ }
    //#endif
}
//#endif
