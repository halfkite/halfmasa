package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.KeyboardHandler;
//#if MC >= 1.21.10
import net.minecraft.client.input.CharacterEvent;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor
{
    //#if MC >= 1.21.10
    @Invoker("charTyped")
    void halfmasa$charTyped(long window, CharacterEvent event);
    //#else
    //$$ @Invoker("charTyped")
    //$$ void halfmasa$charTyped(long window, int codePoint, int modifiers);
    //#endif
}
