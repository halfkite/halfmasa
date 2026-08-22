//#if MC < 1.21.8
package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.BookEditDisplayCacheAccess;
import io.github.halfmasa.xaerobinding.feature.CjkLatinSpacing;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin
{
    @Shadow private TextFieldHelper pageEdit;
    @Shadow protected abstract String getCurrentPageText();
    @Shadow protected abstract void clearDisplayCache();

    @Unique private boolean halfmasa$spacingEnabled;
    @Unique private CjkLatinSpacing.Result halfmasa$currentSpacing = CjkLatinSpacing.applyWithMapping("");

    @Inject(method = "render", at = @At("HEAD"))
    private void halfmasa_refreshSpacingCache(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        boolean enabled = Configs.CJK_LATIN_SPACING.getBooleanValue() &&
                Configs.CJK_LATIN_SPACING_BOOKS.getBooleanValue();
        if (enabled != this.halfmasa$spacingEnabled)
        {
            this.halfmasa$spacingEnabled = enabled;
            this.clearDisplayCache();
        }
    }

    @Redirect(
            method = "rebuildDisplayCache",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/BookEditScreen;getCurrentPageText()Ljava/lang/String;"))
    private String halfmasa_prepareBookPage(BookEditScreen instance)
    {
        String original = this.getCurrentPageText();
        this.halfmasa$currentSpacing = this.halfmasa$spacingEnabled
                ? CjkLatinSpacing.applyWithMapping(original)
                : CjkLatinSpacing.unchanged(original);
        return this.halfmasa$currentSpacing.text();
    }

    @Redirect(
            method = "rebuildDisplayCache",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getCursorPos()I"))
    private int halfmasa_mapCursorToDisplay(TextFieldHelper helper)
    {
        return this.halfmasa$currentSpacing.toSpacedIndex(helper.getCursorPos());
    }

    @Redirect(
            method = "rebuildDisplayCache",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getSelectionPos()I"))
    private int halfmasa_mapSelectionToDisplay(TextFieldHelper helper)
    {
        return this.halfmasa$currentSpacing.toSpacedIndex(helper.getSelectionPos());
    }

    @Inject(method = "rebuildDisplayCache", at = @At("RETURN"))
    private void halfmasa_attachBookPageMapping(CallbackInfoReturnable<Object> cir)
    {
        ((BookEditDisplayCacheAccess) cir.getReturnValue()).halfmasa_setSpacing(this.halfmasa$currentSpacing);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/FormattedCharSequence;forward(Ljava/lang/String;Lnet/minecraft/network/chat/Style;)Lnet/minecraft/util/FormattedCharSequence;"),
            index = 0)
    private String halfmasa_spaceBookTitle(String title)
    {
        return this.halfmasa$spacingEnabled ? CjkLatinSpacing.apply(title) : title;
    }
}
//#endif
