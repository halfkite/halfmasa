package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(GameRenderer.class)
public abstract class NightVisionFadeMixin
{
    //#if MC >= 26.2
    @Inject(method = "nightVisionScale", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    //#endif
    private static void halfmasa$fadeNightVision(
            LivingEntity entity,
            float partialTick,
            CallbackInfoReturnable<Float> cir)
    {
        MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null || !Configs.NIGHT_VISION_FADE.getBooleanValue())
        {
            return;
        }

        int fadeTicks = Configs.NIGHT_VISION_FADE_SECONDS.getIntegerValue() * 20;
        if (fadeTicks <= 0 || !effect.endsWithin(fadeTicks))
        {
            cir.setReturnValue(1.0F);
            return;
        }

        float remainingTicks = Math.max(0.0F, effect.getDuration() - partialTick);
        cir.setReturnValue(Mth.clamp(remainingTicks / fadeTicks, 0.0F, 1.0F));
    }
}
