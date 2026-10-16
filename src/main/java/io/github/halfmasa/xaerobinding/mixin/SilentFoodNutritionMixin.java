package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.halfmasa.xaerobinding.feature.SilentFoodTransaction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 1.21.3
@Mixin(FoodProperties.class)
public abstract class SilentFoodNutritionMixin
{
    @WrapOperation(method = "onConsume", at = @At(value = "INVOKE", target =
            //#if MC >= 1.21.5
            "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
            //#else
            //$$ "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
            //#endif
    //#if MC >= 1.21.5
    private void halfmasa$silentNutrition(Level level, Entity excluded, double x, double y, double z,
    //#else
    //$$ private void halfmasa$silentNutrition(Level level, net.minecraft.world.entity.player.Player excluded, double x, double y, double z,
    //#endif
            SoundEvent sound, SoundSource category, float volume, float pitch, Operation<Void> original,
            @Local(argsOnly = true) LivingEntity consumer)
    {
        if (!SilentFoodTransaction.isSilent(consumer)) original.call(level, excluded, x, y, z, sound, category, volume, pitch);
    }
}
//#else
//$$ @Mixin(net.minecraft.world.entity.player.Player.class)
//$$ public abstract class SilentFoodNutritionMixin
//$$ {
//$$     @WrapOperation(method = "eat", at = @At(value = "INVOKE", target =
//$$             "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
//$$     private void halfmasa$silentNutrition(Level level, net.minecraft.world.entity.player.Player excluded, double x, double y, double z,
//$$             SoundEvent sound, SoundSource category, float volume, float pitch, Operation<Void> original)
//$$     {
//$$         if (!SilentFoodTransaction.isSilent((LivingEntity)(Object)this)) original.call(level, excluded, x, y, z, sound, category, volume, pitch);
//$$     }
//$$ }
//#endif
//#endif
