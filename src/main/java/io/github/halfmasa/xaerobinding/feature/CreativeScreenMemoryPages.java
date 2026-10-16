package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
//#if MC >= 26.0
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
//#else
//$$ import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
//#endif
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

/** Linked only when the optional Fabric creative-tab module is installed. */
final class CreativeScreenMemoryPages
{
    private CreativeScreenMemoryPages() {}

    static int currentPage(CreativeModeInventoryScreen screen)
    {
        //#if MC >= 26.0
        return ((FabricCreativeModeInventoryScreen) screen).getCurrentPage();
        //#else
        //$$ return ((FabricCreativeInventoryScreen) screen).getCurrentPage();
        //#endif
    }

    static void restorePage(CreativeModeInventoryScreen screen, CreativeModeTab tab, int savedPage)
    {
        //#if MC >= 26.0
        FabricCreativeModeInventoryScreen pages = (FabricCreativeModeInventoryScreen) screen;
        //#else
        //$$ FabricCreativeInventoryScreen pages = (FabricCreativeInventoryScreen) screen;
        //#endif
        if (pages.getPageCount() == 0) return;
        pages.switchToPage(Math.clamp(savedPage, 0, pages.getPageCount() - 1));
        // A mod may have moved the remembered tab since it was last opened.
        int target = pages.getPage(tab);
        if (target >= 0 && target < pages.getPageCount() && target != pages.getCurrentPage())
            pages.switchToPage(target);
    }
}
//#endif
