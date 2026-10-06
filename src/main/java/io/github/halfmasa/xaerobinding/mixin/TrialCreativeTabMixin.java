package io.github.halfmasa.xaerobinding.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.TrialCreativeTab;

/**
 * Appends the synthetic trial tab to the vanilla creative tab lists. Both {@code tabs()} and
 * {@code allTabs()} stream the frozen {@code CREATIVE_MODE_TAB} registry, so the extra tab has
 * to be added by rewriting the returned lists rather than by registering it.
 */
@Mixin(CreativeModeTabs.class)
public abstract class TrialCreativeTabMixin
{
    @Inject(method = "tabs", at = @At("RETURN"), cancellable = true)
    private static void halfmasa_appendTrialTab(CallbackInfoReturnable<List<CreativeModeTab>> cir)
    {
        cir.setReturnValue(halfmasa$withTrialTab(cir.getReturnValue()));
    }

    @Inject(method = "allTabs", at = @At("RETURN"), cancellable = true)
    private static void halfmasa_appendTrialTabToAll(CallbackInfoReturnable<List<CreativeModeTab>> cir)
    {
        cir.setReturnValue(halfmasa$withTrialTab(cir.getReturnValue()));
    }

    @Unique
    private static List<CreativeModeTab> halfmasa$withTrialTab(List<CreativeModeTab> original)
    {
        if (!TrialCreativeTab.isEnabled() || original == null || original.contains(TrialCreativeTab.get()))
        {
            return original;
        }
        List<CreativeModeTab> result = new ArrayList<>(original.size() + 1);
        result.addAll(original);
        result.add(TrialCreativeTab.get());
        return result;
    }
}
