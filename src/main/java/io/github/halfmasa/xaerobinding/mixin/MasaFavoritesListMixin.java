//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.Collection;
import java.util.Comparator;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.gui.ActionConfigListWidget;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritesOrder;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritesScreen;
import io.github.halfmasa.xaerobinding.favorites.MasaConfigCatalog;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritesStore;
import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(value = WidgetListConfigOptions.class, remap = false)
public abstract class MasaFavoritesListMixin
{
    @Shadow @Final protected GuiConfigsBase parent;

    @Inject(method = "getAllEntries", at = @At("RETURN"), cancellable = true)
    private void halfmasa$pinFavorites(CallbackInfoReturnable<Collection<ConfigOptionWrapper>> cir)
    {
        if ((Object) this instanceof ActionConfigListWidget || this.parent instanceof MasaFavoritesScreen) return;
        cir.setReturnValue(MasaFavoritesOrder.sort(cir.getReturnValue(), this.parent.getModId(), null));
    }

    @Inject(method = "getComparator", at = @At("RETURN"), cancellable = true)
    private void halfmasa$sortedFavorites(CallbackInfoReturnable<Comparator<ConfigOptionWrapper>> cir)
    {
        if (!Configs.MASA_FAVORITES.getBooleanValue() || !Configs.MASA_FAVORITES_PIN.getBooleanValue() || (Object) this instanceof ActionConfigListWidget ||
                this.parent instanceof MasaFavoritesScreen) return;
        Comparator<ConfigOptionWrapper> original = cir.getReturnValue();
        cir.setReturnValue(Comparator.<ConfigOptionWrapper, Boolean>comparing(wrapper -> {
            var entry = MasaConfigCatalog.observe(this.parent.getModId(), wrapper.getConfig());
            return entry == null || !MasaFavoritesStore.get().contains(entry.key());
        }).thenComparing(original));
    }
}
//#endif
