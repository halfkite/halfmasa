package io.github.halfmasa.xaerobinding.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
//#if MC >= 1.21.1
import org.spongepowered.asm.mixin.Shadow;
//#endif
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.TrialCreativeTab;

/**
 * Invalidates cached contents when the trial toggle changes on 26.3, allowing Fabric to
 * recalculate visibility and pages. Earlier versions retain their synthetic list extension.
 */
@Mixin(CreativeModeTabs.class)
public abstract class TrialCreativeTabMixin
{
    //#if MC >= 1.21.1
    @Shadow private static CreativeModeTab.ItemDisplayParameters CACHED_PARAMETERS;
    @Unique private static boolean halfmasa$trialEnabled;

    @Inject(method = "tryRebuildTabContents", at = @At("HEAD"))
    private static void halfmasa_invalidateTrialContents(CallbackInfoReturnable<Boolean> cir)
    {
        boolean enabled = TrialCreativeTab.isEnabled();
        if (enabled != halfmasa$trialEnabled)
        {
            halfmasa$trialEnabled = enabled;
            CACHED_PARAMETERS = null;
        }
    }
    //#endif

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
        //#if MC >= 1.21.1
        // Registered tabs are supplied by the registry and Fabric's page allocator.
        // Without Fabric's allocator, the inventory button supplies access instead.
        return original;
        //#else
        //$$ if (!TrialCreativeTab.isEnabled() || original == null || original.contains(TrialCreativeTab.get()))
        //$$ {
        //$$     return original;
        //$$ }
        //$$ List<CreativeModeTab> result = new ArrayList<>(original.size() + 1);
        //$$ result.addAll(original);
        //$$ result.add(TrialCreativeTab.get());
        //$$ return result;
        //#endif
    }
}
