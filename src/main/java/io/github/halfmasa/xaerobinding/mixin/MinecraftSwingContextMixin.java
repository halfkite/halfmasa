package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.halfmasa.xaerobinding.feature.SwingVisualTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MinecraftSwingContextMixin
{
    @WrapMethod(method = "startAttack")
    private boolean halfmasa$attackSource(Operation<Boolean> original)
    {
        var previous = SwingVisualTracker.enter(SwingVisualTracker.Source.ATTACK);
        try { return original.call(); } finally { SwingVisualTracker.restore(previous); }
    }

    @WrapMethod(method = "continueAttack")
    private void halfmasa$miningSource(boolean attacking, Operation<Void> original)
    {
        var previous = SwingVisualTracker.enter(SwingVisualTracker.Source.ATTACK);
        try { original.call(attacking); } finally { SwingVisualTracker.restore(previous); }
    }

    @WrapMethod(method = "startUseItem")
    private void halfmasa$interactionSource(Operation<Void> original)
    {
        var previous = SwingVisualTracker.enter(SwingVisualTracker.Source.INTERACT);
        try { original.call(); } finally { SwingVisualTracker.restore(previous); }
    }
}
//#endif
