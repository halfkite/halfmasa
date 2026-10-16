package io.github.halfmasa.xaerobinding.mixin;

//#if MC < 1.21.11
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnDataBridge;

/** Older MiniHUD can cache client-generated NBT; only its server reply path is authoritative. */
@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.data.EntitiesDataManager", remap = false)
public abstract class ItemDespawnLegacyMiniHudMixin
{
    @Inject(method = "handleEntityData", at = @At("TAIL"), remap = false, require = 1)
    private void halfmasa$serverItemAge(int id, @Coerce Object data, CallbackInfoReturnable<Object> ci)
    {
        if (data instanceof CompoundTag tag) ItemDespawnDataBridge.receiveLegacyMiniHud(id, tag);
    }
}
//#endif
