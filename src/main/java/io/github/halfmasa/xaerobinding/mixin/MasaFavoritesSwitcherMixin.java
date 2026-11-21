//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.config.registry.ConfigScreenRegistry;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.util.data.ModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.xaerobinding.favorites.MasaConfigSwitcher;
import io.github.halfmasa.xaerobinding.favorites.MasaSwitcherAccess;
import io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat;

@Mixin(value = GuiConfigsBase.class, remap = false)
public abstract class MasaFavoritesSwitcherMixin implements MasaSwitcherAccess
{
    @Shadow protected WidgetDropDownList<ModInfo> modSwitchWidget;
    @Shadow protected abstract void buildConfigSwitcher();

    @WrapOperation(method = "buildConfigSwitcher", at = @At(value = "INVOKE", target =
            "Lfi/dy/masa/malilib/gui/config/registry/ConfigScreenRegistry;getModInfoFromConfigScreen(Ljava/lang/Class;)Lfi/dy/masa/malilib/util/data/ModInfo;"))
    private ModInfo halfmasa$findCurrentPage(ConfigScreenRegistry registry, Class<? extends GuiBase> type, Operation<ModInfo> original)
    {
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        String id = screen.getModId();
        return MasaConfigSwitcher.enabled() && id != null && !id.isBlank()
                ? MasaGadgetCompat.registerCurrentPage(screen) : original.call(registry, type);
    }

    @Inject(method = "buildConfigSwitcher", at = @At("TAIL"))
    private void halfmasa$enhanceSwitcher(CallbackInfo ci)
    {
        this.halfmasa$installSwitcher();
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void halfmasa$finishSwitcher(CallbackInfo ci)
    {
        // Gadget builds its menu from GuiBase.initGui and may suppress native addWidget.
        this.halfmasa$installSwitcher();
    }

    @org.spongepowered.asm.mixin.Unique
    private void halfmasa$installSwitcher()
    {
        if (!MasaConfigSwitcher.enabled()) return;
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        var gadget = MasaGadgetCompat.detach(screen);
        if (this.modSwitchWidget == null && gadget == null) return;
        var widgets = ((MasaFavoritesGuiWidgetsAccessor) screen).halfmasa$getWidgets();
        if (this.modSwitchWidget instanceof MasaConfigSwitcher)
        {
            if (!widgets.contains(this.modSwitchWidget)) screen.addWidget(this.modSwitchWidget);
            return;
        }
        var old = this.modSwitchWidget;
        var anchor = gadget != null ? gadget : old;
        int right = anchor.getX() + anchor.getWidth();
        int y = anchor.getY();
        int width = anchor.getWidth();
        int height = anchor.getHeight();
        Runnable discoverOnOpen = MasaGadgetCompat.needsPageDiscovery() ? () -> {
            var mods = MasaGadgetCompat.pages();
            var current = mods.stream().filter(mod -> MasaGadgetCompat.modId(mod).equals(screen.getModId())).findFirst().orElse(null);
            var expanded = new MasaConfigSwitcher(right, y, width, height, mods, current);
            int index = widgets.indexOf(this.modSwitchWidget);
            if (index >= 0) widgets.set(index, expanded);
            this.modSwitchWidget = expanded;
            expanded.open();
        } : null;
        var mods = MasaGadgetCompat.knownPages();
        ModInfo selected = mods.stream().filter(mod -> MasaGadgetCompat.modId(mod).equals(screen.getModId())).findFirst().orElse(null);
        this.modSwitchWidget = new MasaConfigSwitcher(right, y, width, height, mods, selected, discoverOnOpen);
        widgets.remove(old);
        screen.addWidget(this.modSwitchWidget);
    }

    @Override
    //#if MC >= 1.21.11
    public void halfmasa$renderSwitcherOverlay(fi.dy.masa.malilib.render.GuiContext context, int mouseX, int mouseY)
    //#else
    //$$ public void halfmasa$renderSwitcherOverlay(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY)
    //#endif
    {
        if (MasaConfigSwitcher.enabled() && this.modSwitchWidget instanceof MasaConfigSwitcher switcher)
            switcher.renderOverlay(context, mouseX, mouseY);
    }

    @Override
    public void halfmasa$rebuildSwitcher()
    {
        ((MasaFavoritesGuiWidgetsAccessor) this).halfmasa$getWidgets().remove(this.modSwitchWidget);
        this.modSwitchWidget = null;
        this.buildConfigSwitcher();
        if (!MasaConfigSwitcher.enabled()) MasaGadgetCompat.restore((GuiConfigsBase) (Object) this);
    }
}
//#endif
