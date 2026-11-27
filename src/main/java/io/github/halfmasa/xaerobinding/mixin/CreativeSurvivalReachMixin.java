package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.CreativeSurvivalReach;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class CreativeSurvivalReachMixin
{
    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void halfmasa$survivalBlockReach(CallbackInfoReturnable<Double> cir)
    {
        double original = cir.getReturnValueD();
        double adjusted = CreativeSurvivalReach.blockRange((Player) (Object) this, original);
        if (adjusted != original) cir.setReturnValue(adjusted);
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void halfmasa$survivalEntityReach(CallbackInfoReturnable<Double> cir)
    {
        double original = cir.getReturnValueD();
        double adjusted = CreativeSurvivalReach.entityRange((Player) (Object) this, original);
        if (adjusted != original) cir.setReturnValue(adjusted);
    }
}
//#endif
