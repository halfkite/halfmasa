package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ActionConfigListWidget;
import io.github.halfmasa.xaerobinding.gui.TweakerooConfigExpansionProvider;

@Mixin(value = GuiConfigsBase.class, remap = false)
public abstract class TweakerooConfigListMixin
{
    @Inject(
            method = "createListWidget(II)Lfi/dy/masa/malilib/gui/widgets/WidgetListConfigOptions;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void halfmasa$replaceTweakerooList(
            int listX,
            int listY,
            CallbackInfoReturnable<WidgetListConfigOptions> cir)
    {
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        if ((!Configs.TWEAKEROO_COLLAPSIBLE_CONFIG.getBooleanValue() &&
                !Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue()) ||
                !screen.getClass().getName().equals("fi.dy.masa.tweakeroo.gui.GuiConfigs"))
        {
            return;
        }

        GuiConfigsBaseAccessor dimensions = (GuiConfigsBaseAccessor) (Object) screen;
        cir.setReturnValue(new ActionConfigListWidget(
                listX,
                listY,
                dimensions.halfmasa$getBrowserWidth(),
                dimensions.halfmasa$getBrowserHeight(),
                dimensions.halfmasa$getConfigWidth(),
                0.0F,
                dimensions.halfmasa$useKeybindSearch(),
                screen,
                new TweakerooConfigExpansionProvider()));
    }
}
