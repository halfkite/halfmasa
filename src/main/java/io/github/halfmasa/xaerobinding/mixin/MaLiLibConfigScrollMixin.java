package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;
import io.github.halfmasa.xaerobinding.feature.MaLiLibConfigScrollAccess;

@Mixin(value = GuiConfigsBase.class, remap = false)
public abstract class MaLiLibConfigScrollMixin implements MaLiLibConfigScrollAccess
{
    @Unique private String halfmasa$currentScrollKey;

    @Inject(method = "initGui", at = @At("TAIL"), remap = false)
    private void halfmasa_restoreAfterInit(CallbackInfo ci)
    {
        this.halfmasa$restoreConfigScroll();
    }

    @Inject(method = {"removed", "method_25432"}, at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa_saveWhenClosed(CallbackInfo ci)
    {
        this.halfmasa$saveConfigScroll();
    }

    @Override
    public void halfmasa$saveConfigScroll()
    {
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        ConfigScrollMemory.save(screen, this.halfmasa$getWidget(), this.halfmasa$currentScrollKey);
    }

    @Override
    public void halfmasa$restoreConfigScroll()
    {
        GuiConfigsBase screen = (GuiConfigsBase) (Object) this;
        this.halfmasa$currentScrollKey = ConfigScrollMemory.restore(screen, this.halfmasa$getWidget());
    }

    @Unique
    private WidgetListBase<?, ?> halfmasa$getWidget()
    {
        return ((GuiListBaseAccessor) (Object) this).halfmasa$getListWidget();
    }
}
