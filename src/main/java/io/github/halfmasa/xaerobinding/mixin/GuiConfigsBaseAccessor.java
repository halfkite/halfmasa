package io.github.halfmasa.xaerobinding.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiConfigsBase.class, remap = false)
public interface GuiConfigsBaseAccessor
{
    @Invoker("getBrowserWidth")
    int halfmasa$getBrowserWidth();

    @Invoker("getBrowserHeight")
    int halfmasa$getBrowserHeight();

    @Invoker("getConfigWidth")
    int halfmasa$getConfigWidth();

    @Invoker("useKeybindSearch")
    boolean halfmasa$useKeybindSearch();
}
