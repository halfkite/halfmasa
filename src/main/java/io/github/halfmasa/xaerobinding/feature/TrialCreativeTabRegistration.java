package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
//#if MC >= 26.0
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//#else
//$$ import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
//#endif
//#if MC >= 26.0
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//#else
//$$ import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//#endif
import net.minecraft.core.Registry;
//#if MC >= 26.0
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
//#else
//$$ import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
//#endif
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Loaded only when Fabric's optional creative-tab API is installed. */
final class TrialCreativeTabRegistration
{
    private TrialCreativeTabRegistration() {}

    static CreativeModeTab register()
    {
        //#if MC >= 26.0
        CreativeModeTab tab = FabricCreativeModeTab.builder()
        //#else
        //$$ CreativeModeTab tab = FabricItemGroup.builder()
        //#endif
                .title(Component.translatable("halfmasa.creative_tab.trial"))
                .icon(() -> new ItemStack(Items.TRIAL_SPAWNER))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("halfmasa", "trial"), tab);
        //#if MC >= 26.0
        CreativeModeTabEvents.modifyOutputEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow())
        //#else
        //$$ ItemGroupEvents.modifyEntriesEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow())
        //#endif
                .register(output -> {
                    if (TrialCreativeTab.isEnabled())
                        for (ItemStack stack : TrialCreativeItems.getItems()) output.accept(stack.copy());
                });
        return tab;
    }
    /** Rebuilding pages can hide the selected tab or move another tab to a different page. */
    static void synchronizeSelection(CreativeModeInventoryScreen screen)
    {
        //#if MC >= 26.0
        FabricCreativeModeInventoryScreen pages = (FabricCreativeModeInventoryScreen) screen;
        //#else
        //$$ FabricCreativeInventoryScreen pages = (FabricCreativeInventoryScreen) screen;
        //#endif
        //#if MC >= 26.0
        CreativeModeTab selected = pages.getSelectedTab();
        //#else
        //$$ CreativeModeTab selected = pages.getSelectedItemGroup();
        //#endif
        CreativeModeTab desired = selected.shouldDisplay() ? selected : CreativeModeTabs.getDefaultTab();
        int desiredPage = pages.getPage(desired);
        if (pages.getCurrentPage() != desiredPage) pages.switchToPage(desiredPage);
        //#if MC >= 26.0
        if (pages.getSelectedTab() != desired) pages.setSelectedTab(desired);
        //#else
        //$$ if (pages.getSelectedItemGroup() != desired) pages.setSelectedItemGroup(desired);
        //#endif
    }

}
//#endif
