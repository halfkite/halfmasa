package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldSelectionList.class)
public interface WorldSelectionListAccessor
{
    @Invoker("reloadWorldList")
    void halfmasa$reloadWorldList();
    //#if MC >= 1.21.1
    @org.spongepowered.asm.mixin.gen.Accessor("currentlyDisplayedLevels")
    java.util.List<net.minecraft.world.level.storage.LevelSummary> halfmasa$getLevels();

    @org.spongepowered.asm.mixin.gen.Accessor("filter")
    String halfmasa$getFilter();

    @Invoker("fillLevels")
    void halfmasa$fillLevels(String filter, java.util.List<net.minecraft.world.level.storage.LevelSummary> levels);

    //#endif
}
