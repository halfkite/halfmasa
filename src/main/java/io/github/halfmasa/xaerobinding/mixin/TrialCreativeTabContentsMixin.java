package io.github.halfmasa.xaerobinding.mixin;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.TrialCreativeItems;
import io.github.halfmasa.xaerobinding.feature.TrialCreativeTab;

/**
 * Supplies the display items for the synthetic trial tab.
 *
 * <p>The tab is never registered, so the vanilla rebuild pass never calls its
 * {@code displayItems} generator and {@code getDisplayItems()} would otherwise return
 * {@code null}. Intercepting the getter keeps the vanilla {@code selectTab} path untouched.</p>
 */
@Mixin(CreativeModeTab.class)
public abstract class TrialCreativeTabContentsMixin
{
    @Inject(method = "getDisplayItems", at = @At("HEAD"), cancellable = true)
    private void halfmasa_trialTabItems(CallbackInfoReturnable<Collection<ItemStack>> cir)
    {
        if (!TrialCreativeTab.isTrialTab((CreativeModeTab) (Object) this))
        {
            return;
        }

        Collection<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : TrialCreativeItems.getItems())
        {
            items.add(stack.copy());
        }
        cir.setReturnValue(items);
    }
}
