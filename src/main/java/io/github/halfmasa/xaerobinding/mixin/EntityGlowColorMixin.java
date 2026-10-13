package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 1.21.10
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.EntityGlowService;
//#endif

/** Supplies a rule color to the legacy entity outline buffer. */
@Mixin(Entity.class)
public abstract class EntityGlowColorMixin
{
    //#if MC < 1.21.10
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void halfmasa$glowColor(CallbackInfoReturnable<Integer> cir)
    {
        Entity entity = (Entity) (Object) this;
        if (entity.isCurrentlyGlowing()) return;
        Integer color = EntityGlowService.resolve(entity);
        if (color != null) cir.setReturnValue(color);
    }
    //#endif
}
