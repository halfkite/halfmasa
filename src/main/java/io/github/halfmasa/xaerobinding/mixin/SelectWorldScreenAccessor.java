package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SelectWorldScreen.class)
public interface SelectWorldScreenAccessor
{
    @Accessor("list")
    WorldSelectionList halfmasa$getWorldList();

    //#if MC >= 1.21.1
    @Accessor("searchBox")
    net.minecraft.client.gui.components.EditBox halfmasa$getSearchBox();
    //#endif
}
