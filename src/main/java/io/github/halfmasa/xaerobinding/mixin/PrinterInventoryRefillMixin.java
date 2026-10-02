package io.github.halfmasa.xaerobinding.mixin;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill;

/** Fallback for older printer forks that retain the shared InventoryUtils material check. */
@Pseudo
@Mixin(targets = "me.aleksilassila.litematica.printer.utils.InventoryUtils", remap = false)
public abstract class PrinterInventoryRefillMixin
{
    @Inject(method = "playerHasAccessToItems",
            at = @At("RETURN"), require = 0)
    private static void halfmasa$requestMissingStock(LocalPlayer player, Item[] items, CallbackInfoReturnable<Boolean> cir)
    {
        if (!cir.getReturnValueZ() && player != null && items != null && items.length > 0)
        {
            var accepted = Arrays.stream(items).filter(Objects::nonNull).toList();
            if (!accepted.isEmpty()) LitematicaMaterialRefill.getInstance().onPrinterMaterialNeeded(accepted, accepted.get(0));
        }
    }
}
