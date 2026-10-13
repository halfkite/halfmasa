package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 1.21.10
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.EntityGlowService;
//#endif

/** Requests the vanilla outline pass for legacy entity renderers. */
@Mixin(Minecraft.class)
public abstract class MinecraftEntityGlowMixin
{
    //#if MC < 1.21.10
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void halfmasa$glowRule(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if (!cir.getReturnValue() && entity != ((Minecraft) (Object) this).player
                && EntityGlowService.resolve(entity) != null)
        {
            cir.setReturnValue(true);
        }
    }
    //#endif
}
