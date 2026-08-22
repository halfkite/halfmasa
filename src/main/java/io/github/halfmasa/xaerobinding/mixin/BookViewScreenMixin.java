package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CjkLatinComponentSpacing;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin
{
    @Shadow private int cachedPage;
    @Unique private boolean halfmasa$spacingEnabled;

    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
    private void halfmasa_refreshSpacingCache(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), require = 0)
    //$$ private void halfmasa_refreshSpacingCache(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        boolean enabled = Configs.CJK_LATIN_SPACING.getBooleanValue() &&
                Configs.CJK_LATIN_SPACING_BOOKS.getBooleanValue();
        if (enabled != this.halfmasa$spacingEnabled)
        {
            this.halfmasa$spacingEnabled = enabled;
            this.cachedPage = -1;
        }
    }

    //#if MC >= 26.1
    @Redirect(
            method = "visitText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;getPage(I)Lnet/minecraft/network/chat/Component;"),
            require = 0)
    private Component halfmasa_spaceBookPage(BookViewScreen.BookAccess access, int page)
    //#elseif MC >= 1.21.8
    //$$ @Redirect(
    //$$         method = "render",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;getPage(I)Lnet/minecraft/network/chat/Component;"),
    //$$         require = 0)
    //$$ private Component halfmasa_spaceBookPage(BookViewScreen.BookAccess access, int page)
    //#else
    //$$ @Redirect(
    //$$         method = "render",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;getPage(I)Lnet/minecraft/network/chat/FormattedText;"),
    //$$         require = 0)
    //$$ private FormattedText halfmasa_spaceBookPage(BookViewScreen.BookAccess access, int page)
    //#endif
    {
        //#if MC >= 1.21.8
        Component text = access.getPage(page);
        //#else
        //$$ FormattedText text = access.getPage(page);
        //#endif
        //#if MC >= 1.21.8
        if (this.halfmasa$spacingEnabled) return CjkLatinComponentSpacing.apply(text);
        //#else
        //$$ if (this.halfmasa$spacingEnabled && text instanceof Component component)
        //$$ {
        //$$     return CjkLatinComponentSpacing.apply(component);
        //$$ }
        //#endif
        return text;
    }
}
