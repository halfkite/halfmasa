package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import io.github.halfmasa.xaerobinding.feature.FastScrolling;

@Mixin(MouseHandler.class)
public abstract class FastScrollingMouseMixin
{
    @ModifyArgs(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private void halfmasa$accelerateScreenScrolling(Args args)
    {
        int multiplier = FastScrolling.getMultiplier();
        if (multiplier <= 1)
        {
            return;
        }

        double horizontalAmount = args.get(2);
        double verticalAmount = args.get(3);
        args.set(2, horizontalAmount * multiplier);
        args.set(3, verticalAmount * multiplier);
    }
}
