//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOptionBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.favorites.MasaConfigCatalog;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoriteButton;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritePosition;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritesScreen;
import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;

@Mixin(value = WidgetConfigOption.class, remap = false)
public abstract class MasaFavoritesRowMixin extends WidgetConfigOptionBase<ConfigOptionWrapper>
{
    @Shadow @Final protected IKeybindConfigGui host;
    @Unique private int halfmasa$valueEnd;
    @Unique private int halfmasa$sourceWidth;
    @Unique private ButtonBase halfmasa$resetButton;
    protected MasaFavoritesRowMixin(int x, int y, int width, int height,
            WidgetListConfigOptionsBase<?, ?> parent, ConfigOptionWrapper wrapper, int index)
    { super(x, y, width, height, parent, wrapper, index); }

    @Inject(method = "render", at = @At("TAIL"))
    //#if MC >= 1.21.11
    private void halfmasa$drawSourceHighlight(fi.dy.masa.malilib.render.GuiContext context,
            int mouseX, int mouseY, boolean selected, CallbackInfo ci)
    //#elseif MC >= 1.21.8
    //$$ private void halfmasa$drawSourceHighlight(net.minecraft.client.gui.GuiGraphics context,
    //$$         int mouseX, int mouseY, boolean selected, CallbackInfo ci)
    //#else
    //$$ private void halfmasa$drawSourceHighlight(int mouseX, int mouseY, boolean selected,
    //$$         net.minecraft.client.gui.GuiGraphics context, CallbackInfo ci)
    //#endif
    {
        if (!io.github.halfmasa.xaerobinding.favorites.MasaConfigNavigation.isHighlighted(
                this.host, this.getEntry().getConfig())) return;
        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();
        int color = 0xFFAAD8FF;
        context.fill(x, y, right, y + 1, color);
        context.fill(x, bottom - 1, right, bottom, color);
        context.fill(x, y + 1, x + 1, bottom - 1, color);
        context.fill(right - 1, y + 1, right, bottom - 1, color);
    }

    @Override
    protected ButtonGeneric createResetButton(int x, int y, IConfigResettable config)
    {
        ButtonGeneric reset = super.createResetButton(x, y, config);
        this.halfmasa$resetButton = reset;
        return reset;
    }

    // Modify the virtual call, which also covers ActionConfigOptionWidget's early-return branches.
    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target =
            //#if MC >= 1.21.10
            "Lfi/dy/masa/malilib/gui/widgets/WidgetConfigOption;addConfigOption(IIIILfi/dy/masa/malilib/config/IConfigBase;)V"))
            //#else
            //$$ "Lfi/dy/masa/malilib/gui/widgets/WidgetConfigOption;addConfigOption(IIFIILfi/dy/masa/malilib/config/IConfigBase;)V"))
            //#endif
    private void halfmasa$reserveStar(Args args)
    {
        if (!MasaFavoriteButton.isShown(this.host)) return;
        //#if MC >= 1.21.10
        int labelIndex=2, valueIndex=3, configIndex=4;
        //#else
        //$$ int labelIndex=3, valueIndex=4, configIndex=5;
        //#endif
        if (args.get(configIndex) instanceof ConfigGroupHeader) return;
        int space = MasaFavoriteButton.space();
        if (this.host instanceof MasaFavoritesScreen)
        {
            var entry = MasaConfigCatalog.lookup(args.get(configIndex));
            if (entry != null)
            {
                this.halfmasa$sourceWidth = Math.min(78, this.getStringWidth(MasaConfigCatalog.sourceName(entry.modId())) + 10);
                space += this.halfmasa$sourceWidth + 2;
            }
        }
        boolean customRow = (Object) this instanceof io.github.halfmasa.xaerobinding.gui.ActionConfigOptionWidget;
        if (customRow && MasaFavoritePosition.isLeft())
            space += io.github.halfmasa.xaerobinding.gui.ActionConfigOptionWidget.getLeadingControlOverhang();
        // Reserve the native reset button, keybind settings button and their gaps.
        int reset = this.getStringWidth(StringUtils.translate("malilib.gui.button.reset.caps")) + 38;
        int group = customRow ? ((io.github.halfmasa.xaerobinding.gui.ActionConfigOptionWidget) (Object) this)
                .getFavoriteGroupControlsWidth(args.get(configIndex)) : 0;
        int configWidth = args.get(valueIndex);
        int preferredLabel = Math.min(160, this.getWidth() / 3);
        configWidth = Math.min(configWidth,
                Math.max(90, this.getWidth() - reset - space - group - 18 - preferredLabel));
        args.set(valueIndex, configWidth);
        int label = Math.max(20, Math.min((int) args.get(labelIndex), this.getWidth() - configWidth - reset - space - group - 18));
        args.set(labelIndex, label);
        if (MasaFavoritePosition.isLeft())
            args.set(0, (int) args.get(0) + space - (this.halfmasa$sourceWidth > 0 ? this.halfmasa$sourceWidth + 2 : 0));
        this.halfmasa$valueEnd = (int) args.get(0) + label + 10 + configWidth;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void halfmasa$addStar(int x, int y, int width, int height, int labelWidth, int configWidth,
            ConfigOptionWrapper wrapper, int listIndex, IKeybindConfigGui host,
            WidgetListConfigOptionsBase<?, ?> parent, CallbackInfo ci)
    {
        if (!MasaFavoriteButton.isShown(host)) return;
        var entry = MasaConfigCatalog.observe(host.getModId(), wrapper.getConfig());
        if (entry == null) return;
        int starX;
        boolean left = MasaFavoritePosition.isLeft();
        int afterReset = 0;
        if (!left || this.halfmasa$sourceWidth > 0)
        {
            ButtonBase reset = this.halfmasa$resetButton;
            if (reset == null)
                reset = this.subWidgets.stream().filter(widget -> widget instanceof ButtonBase)
                        .map(widget -> (ButtonBase) widget).filter(button -> button.getX() > this.halfmasa$valueEnd)
                        .min(java.util.Comparator.comparingInt(ButtonBase::getX)).orElse(null);
            // Action-only and empty custom rows omit reset, but still occupy its column.
            // Measure the same localized native button so these rows align in every language.
            int resetRight = reset != null ? reset.getX() + reset.getWidth()
                    : this.halfmasa$valueEnd + 2 + new ButtonGeneric(0, 0, -1, 20,
                            StringUtils.translate("malilib.gui.button.reset.caps")).getWidth();
            afterReset = resetRight + 2;
            // The star follows reset; only trailing group controls need to move.
            if (!left)
                for (var widget : this.subWidgets)
                    if (widget.getX() >= afterReset) widget.setX(widget.getX() + MasaFavoriteButton.space());
        }
        starX = left ? this.getX() : afterReset;
        if (host instanceof MasaFavoritesScreen favorites && this.halfmasa$sourceWidth > 0)
        {
            int sourceX = afterReset + (left ? 0 : MasaFavoriteButton.space());
            String source = MasaConfigCatalog.sourceName(entry.modId());
            if (MasaFavoritesScreen.canJumpToSource())
                this.addWidget(favorites.sourceButton(entry.modId(), entry, sourceX, y + 1, this.halfmasa$sourceWidth, source));
            else
                this.addLabel(sourceX + 4, y + 7, this.halfmasa$sourceWidth, 8, 0xFFCCCCCC,
                        favorites.sourceLabel(source, this.halfmasa$sourceWidth));
        }
        this.addWidget(new MasaFavoriteButton(starX, y + (22 - MasaFavoriteButton.size()) / 2, entry, () -> {
            if (host instanceof io.github.halfmasa.xaerobinding.favorites.MasaFavoritesScreen ||
                    !Configs.MASA_FAVORITES_PIN.getBooleanValue()) return;
            int scroll = parent.getScrollbar().getValue();
            parent.applyPendingModifications();
            parent.wereConfigsModified();
            if (host instanceof fi.dy.masa.malilib.gui.GuiConfigsBase screen) screen.clearOptions();
            parent.refreshEntries();
            parent.getScrollbar().setValue(scroll);
        }));
    }
}
//#endif
