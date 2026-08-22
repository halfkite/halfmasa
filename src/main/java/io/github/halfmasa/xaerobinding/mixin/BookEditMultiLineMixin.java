//#if MC >= 1.21.8
package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.feature.CjkLatinSpacing;

@Mixin(MultiLineEditBox.class)
public abstract class BookEditMultiLineMixin
{
    @ModifyArg(
            method = "renderContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"),
            index = 1)
    private String halfmasa$spaceBookPageText(String text)
    {
        return this.halfmasa$bookSpacingEnabled() ? CjkLatinSpacing.apply(text) : text;
    }

    @Redirect(
            method = "renderContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int halfmasa$measureSpacedBookPageText(Font font, String text)
    {
        return font.width(this.halfmasa$bookSpacingEnabled() ? CjkLatinSpacing.apply(text) : text);
    }

    private boolean halfmasa$bookSpacingEnabled()
    {
        return MinecraftClientCompat.getScreen(Minecraft.getInstance()) instanceof BookEditScreen &&
                Configs.CJK_LATIN_SPACING.getBooleanValue() &&
                Configs.CJK_LATIN_SPACING_BOOKS.getBooleanValue();
    }
}
//#endif
