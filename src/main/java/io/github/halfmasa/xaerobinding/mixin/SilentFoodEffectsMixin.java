package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.SilentFoodTransaction;
//#if MC >= 1.21.3
import net.minecraft.world.item.component.Consumable;
//#endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.21.3
@Mixin(Consumable.class)
public abstract class SilentFoodEffectsMixin
{
    @Inject(method = "emitParticlesAndSounds", at = @At("HEAD"), cancellable = true)
    private void halfmasa$silentFood(RandomSource random, LivingEntity entity, ItemStack stack, int count, CallbackInfo ci)
    { if (SilentFoodTransaction.isSilent(entity)) ci.cancel(); }
}
//#else
//$$ @Mixin(LivingEntity.class)
//$$ public abstract class SilentFoodEffectsMixin
//$$ {
//$$     @Inject(method = "triggerItemUseEffects", at = @At("HEAD"), cancellable = true)
//$$     private void halfmasa$silentFood(ItemStack stack, int count, CallbackInfo ci)
//$$     { if (SilentFoodTransaction.isSilent((LivingEntity)(Object)this)) ci.cancel(); }
//$$ }
//#endif
//#endif
