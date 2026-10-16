package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

/** Linked only when the optional Fabric creative-tab module is installed. */
final class CreativeScreenMemoryPages
{
    private CreativeScreenMemoryPages() {}

    static int currentPage(CreativeModeInventoryScreen screen)
    {
        return ((FabricCreativeModeInventoryScreen) screen).getCurrentPage();
    }

    static void restorePage(CreativeModeInventoryScreen screen, CreativeModeTab tab, int savedPage)
    {
        FabricCreativeModeInventoryScreen pages = (FabricCreativeModeInventoryScreen) screen;
        if (pages.getPageCount() == 0) return;
        pages.switchToPage(Math.clamp(savedPage, 0, pages.getPageCount() - 1));
        // A mod may have moved the remembered tab since it was last opened.
        int target = pages.getPage(tab);
        if (target >= 0 && target < pages.getPageCount() && target != pages.getCurrentPage())
            pages.switchToPage(target);
    }
}
//#endif
