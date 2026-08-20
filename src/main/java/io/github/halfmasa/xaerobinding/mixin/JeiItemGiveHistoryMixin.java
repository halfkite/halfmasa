package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryCompat;

@Pseudo
@Mixin(targets = "mezz.jei.gui.util.CommandUtil", remap = false)
public abstract class JeiItemGiveHistoryMixin
{
    @Dynamic
    @Inject(method = "giveStack", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_recordJeiGive(ItemStack stack, @Coerce Object amount, CallbackInfo ci)
    {
        ItemManagerHistoryCompat.recordJeiGive(stack);
    }
}
