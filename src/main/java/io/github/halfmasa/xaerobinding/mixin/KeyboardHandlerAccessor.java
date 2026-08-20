package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.KeyboardHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor
{
    @Invoker("charTyped")
    void halfmasa$charTyped(long window, int codePoint, int modifiers);
}
