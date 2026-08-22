package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldSelectionList.class)
public interface WorldSelectionListAccessor
{
    @Invoker("reloadWorldList")
    void halfmasa$reloadWorldList();
}
