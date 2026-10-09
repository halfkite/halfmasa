package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
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
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("halfmasa.creative_tab.trial"))
                .icon(() -> new ItemStack(Items.TRIAL_SPAWNER))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("halfmasa", "trial"), tab);
        CreativeModeTabEvents.modifyOutputEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow())
                .register(output -> {
                    if (TrialCreativeTab.isEnabled())
                        for (ItemStack stack : TrialCreativeItems.getItems()) output.accept(stack.copy());
                });
        return tab;
    }
    /** Rebuilding pages can hide the selected tab or move another tab to a different page. */
    static void synchronizeSelection(CreativeModeInventoryScreen screen)
    {
        FabricCreativeModeInventoryScreen pages = (FabricCreativeModeInventoryScreen) screen;
        CreativeModeTab selected = pages.getSelectedTab();
        CreativeModeTab desired = selected.shouldDisplay() ? selected : CreativeModeTabs.getDefaultTab();
        int desiredPage = pages.getPage(desired);
        if (pages.getCurrentPage() != desiredPage) pages.switchToPage(desiredPage);
        if (pages.getSelectedTab() != desired) pages.setSelectedTab(desired);
    }

}
//#endif
