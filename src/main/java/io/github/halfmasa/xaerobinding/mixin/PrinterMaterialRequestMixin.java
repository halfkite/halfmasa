package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.PrinterRefillBridge;

@Pseudo
@Mixin(targets = "me.aleksilassila.litematica.printer.integration.inventory.MaterialRequestCoordinator", remap = false)
public abstract class PrinterMaterialRequestMixin
{
    @Inject(method = "request",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void halfmasa$requestStock(List<Item> items, Item preferred, @Coerce Object source, CallbackInfoReturnable<Object> cir)
    {
        Object original = cir.getReturnValue();
        Object result = PrinterRefillBridge.onReservation(items, preferred, source, original);
        if (result != original) cir.setReturnValue(result);
    }
}
