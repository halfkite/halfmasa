package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin
{
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void halfmasa$disablePausedClientPrediction(CallbackInfo ci)
    {
        ItemEntity item = (ItemEntity) (Object) this;
        if (Configs.DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION.getBooleanValue()
                && item.level() instanceof ClientLevel
                && item.level().tickRateManager().isEntityFrozen(item))
        {
            ci.cancel();
        }
    }
}
