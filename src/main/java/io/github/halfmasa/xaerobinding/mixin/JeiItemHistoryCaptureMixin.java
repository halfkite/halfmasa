package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryCompat;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.history.LookupHistory", remap = false)
public abstract class JeiItemHistoryCaptureMixin
{
    @Dynamic
    @Inject(method = "add", at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa_recordJeiLookup(@Coerce Object bookmark, CallbackInfo ci)
    {
        ItemManagerHistoryCompat.recordJeiBookmark(bookmark);
    }
}
