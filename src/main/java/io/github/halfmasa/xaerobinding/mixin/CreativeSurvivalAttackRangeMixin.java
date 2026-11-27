package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import io.github.halfmasa.xaerobinding.feature.CreativeSurvivalReach;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttackRange.class)
public abstract class CreativeSurvivalAttackRangeMixin
{
    // A weapon with its own AttackRange uses its survival values, not a fixed 3 blocks.
    @Inject(method = "effectiveMinRange", at = @At("RETURN"), cancellable = true)
    private void halfmasa$survivalMinimum(Entity attacker, CallbackInfoReturnable<Float> cir)
    {
        if (CreativeSurvivalReach.appliesToEntity(attacker))
        {
            cir.setReturnValue(halfmasa$minReach((AttackRange) (Object) this));
        }
    }

    @Inject(method = "effectiveMaxRange", at = @At("RETURN"), cancellable = true)
    private void halfmasa$survivalMaximum(Entity attacker, CallbackInfoReturnable<Float> cir)
    {
        if (CreativeSurvivalReach.appliesToEntity(attacker))
        {
            cir.setReturnValue(halfmasa$maxReach((AttackRange) (Object) this));
        }
    }

    // Default attack ranges may be derived from the creative entity attribute.
    @Inject(method = "defaultFor", at = @At("RETURN"), cancellable = true)
    private static void halfmasa$survivalDefault(LivingEntity attacker, CallbackInfoReturnable<AttackRange> cir)
    {
        if (!(attacker instanceof Player player) || !CreativeSurvivalReach.appliesToEntity(player)) return;
        AttackRange original = cir.getReturnValue();
        float max = (float) CreativeSurvivalReach.entityRange(player, halfmasa$maxReach(original));
        float min = Math.min(halfmasa$minReach(original), max);
        cir.setReturnValue(new AttackRange(min, max, min, max,
                original.hitboxMargin(), original.mobFactor()));
    }

    @org.spongepowered.asm.mixin.Unique
    private static float halfmasa$minReach(AttackRange range)
    {
        //#if MC >= 26.1
        return range.minReach();
        //#else
        //$$ return range.minRange();
        //#endif
    }

    @org.spongepowered.asm.mixin.Unique
    private static float halfmasa$maxReach(AttackRange range)
    {
        //#if MC >= 26.1
        return range.maxReach();
        //#else
        //$$ return range.maxRange();
        //#endif
    }
}
//#endif
