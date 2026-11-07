//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.registry.Registry;
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

    @Inject(method = "buildConfigSwitcher", at = @At("HEAD"))
    private void halfmasa$registerPageBeforeBuilding(CallbackInfo ci)
    {
        if (!MasaConfigSwitcher.enabled()) return;
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        if (Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(screen.getClass()) == null)
            MasaGadgetCompat.pages().stream().filter(mod -> io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod).equals(screen.getModId()))
                    .findFirst().ifPresent(Registry.CONFIG_SCREEN::registerConfigScreenFactory);
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
        var mods = MasaGadgetCompat.pages();
        ModInfo selected = mods.stream().filter(mod -> io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod).equals(screen.getModId())).findFirst().orElse(null);
        this.modSwitchWidget = new MasaConfigSwitcher(anchor.getX() + anchor.getWidth(), anchor.getY(), anchor.getWidth(), anchor.getHeight(), mods, selected);
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
