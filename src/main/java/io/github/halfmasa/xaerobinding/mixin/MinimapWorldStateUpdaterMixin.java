package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;

import io.github.halfmasa.xaerobinding.binding.WorldBindingStore;

@Mixin(value = MinimapWorldStateUpdater.class, remap = false)
public abstract class MinimapWorldStateUpdaterMixin
{
    @Inject(
            method = "getAutoRootContainerPath(I)Lxaero/hud/path/XaeroPath;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void xaeroWorldBinding$bindMinimapRoot(int mode, CallbackInfoReturnable<XaeroPath> cir)
    {
        if (mode != 4)
        {
            return;
        }

        XaeroPath original = cir.getReturnValue();
        if (original != null)
        {
            cir.setReturnValue(XaeroPath.root(WorldBindingStore.resolveMinimapRoot(original.toString())));
        }
    }
}
