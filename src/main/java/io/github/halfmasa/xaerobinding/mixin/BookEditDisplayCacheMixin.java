//#if MC < 1.21.8
package io.github.halfmasa.xaerobinding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.BookEditDisplayCacheAccess;
import io.github.halfmasa.xaerobinding.feature.CjkLatinSpacing;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BookEditScreen$DisplayCache")
public abstract class BookEditDisplayCacheMixin implements BookEditDisplayCacheAccess
{
    @Unique private CjkLatinSpacing.Result halfmasa$spacing;

    @Override
    public void halfmasa_setSpacing(CjkLatinSpacing.Result spacing)
    {
        this.halfmasa$spacing = spacing;
    }

    @Inject(method = "getIndexAtPosition", at = @At("RETURN"), cancellable = true)
    private void halfmasa_mapPositionToSource(CallbackInfoReturnable<Integer> cir)
    {
        if (this.halfmasa$spacing != null)
        {
            cir.setReturnValue(this.halfmasa$spacing.toOriginalIndex(cir.getReturnValue()));
        }
    }

    @ModifyVariable(method = {"changeLine", "findLineStart", "findLineEnd"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int halfmasa_mapSourceToDisplay(int index)
    {
        return this.halfmasa$spacing == null ? index : this.halfmasa$spacing.toSpacedIndex(index);
    }

    @Inject(method = {"changeLine", "findLineStart", "findLineEnd"}, at = @At("RETURN"), cancellable = true)
    private void halfmasa_mapDisplayToSource(CallbackInfoReturnable<Integer> cir)
    {
        if (this.halfmasa$spacing != null)
        {
            cir.setReturnValue(this.halfmasa$spacing.toOriginalIndex(cir.getReturnValue()));
        }
    }
}
//#endif
