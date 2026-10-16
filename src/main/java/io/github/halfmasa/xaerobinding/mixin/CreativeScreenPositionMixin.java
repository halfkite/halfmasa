package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 26.3
import io.github.halfmasa.xaerobinding.feature.CreativeScreenMemory;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenPositionMixin
        //#if MC >= 26.3
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
        implements CreativeScreenMemory.ContentsReady
        //#endif
{
    //#if MC >= 26.3
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Shadow private EditBox searchBox;
    @Shadow protected abstract void selectTab(CreativeModeTab tab);
    @Shadow protected abstract void refreshSearchResults();
    @Unique private boolean halfmasa$ready;
    @Unique private boolean halfmasa$restoring;
    @Unique private boolean halfmasa$selectingTab;
    @Unique private CreativeModeTab halfmasa$pendingTab;

    protected CreativeScreenPositionMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
            Inventory inventory, Component title)
    { super(menu, inventory, title); }

    @Inject(method = "init", at = @At("HEAD"))
    private void halfmasa$beforeInit(CallbackInfo ci)
    {
        if (this.halfmasa$ready) this.halfmasa$saveClose();
        this.halfmasa$ready = false;
        this.halfmasa$pendingTab = null;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void halfmasa$afterInit(CallbackInfo ci)
    {
        this.halfmasa$ready = true;
        CreativeModeTab saved = CreativeScreenMemory.restoreTab();
        if (saved == null) return;
        this.halfmasa$restoring = true;
        CreativeScreenMemory.restorePage((CreativeModeInventoryScreen) (Object) this, saved);
        this.selectTab(saved);
        this.halfmasa$restoring = false;
        this.halfmasa$pendingTab = saved;
        this.halfmasa$restoreCreativePosition(true);
    }

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void halfmasa$beforeSelect(CreativeModeTab next, CallbackInfo ci)
    {
        this.halfmasa$selectingTab = true;
        if (!this.halfmasa$ready || this.halfmasa$restoring || !CreativeScreenMemory.enabled()) return;
        CreativeScreenMemory.saveTab(selectedTab, this.scrollOffs, this.halfmasa$query());
        this.halfmasa$pendingTab = next;
    }

    @Override
    public void halfmasa$restoreCreativePosition(boolean tabSelectionFinished)
    {
        if (tabSelectionFinished) this.halfmasa$selectingTab = false;
        if (!CreativeScreenMemory.enabled()) { this.halfmasa$pendingTab = null; return; }
        if (!this.halfmasa$ready || this.halfmasa$restoring || this.halfmasa$selectingTab
                || this.halfmasa$pendingTab != selectedTab) return;
        this.halfmasa$pendingTab = null;
        this.halfmasa$restoring = true;
        if (selectedTab == CreativeModeTabs.searchTab() && this.searchBox != null)
        {
            String query = CreativeScreenMemory.restoreSearch();
            if (!query.equals(this.searchBox.getValue()))
            {
                this.searchBox.setValue(query);
                this.refreshSearchResults();
            }
        }
        // The final contents, including condensed/history entries, determine scrollability.
        this.scrollOffs = this.menu.canScroll() ? CreativeScreenMemory.restoreScroll(selectedTab) : 0F;
        this.menu.scrollTo(this.scrollOffs);
        this.halfmasa$restoring = false;
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void halfmasa$removed(CallbackInfo ci)
    {
        if (this.halfmasa$ready) this.halfmasa$saveClose();
        this.halfmasa$ready = false;
        this.halfmasa$pendingTab = null;
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void halfmasa$afterResize(CallbackInfo ci)
    {
        if (!this.halfmasa$ready || !CreativeScreenMemory.enabled()) return;
        this.halfmasa$pendingTab = selectedTab;
        this.halfmasa$restoreCreativePosition(true);
    }

    @Unique private String halfmasa$query() { return this.searchBox == null ? "" : this.searchBox.getValue(); }

    @Unique private void halfmasa$saveClose()
    {
        if (!CreativeScreenMemory.enabled()) return;
        CreativeScreenMemory.saveClose(selectedTab, this.scrollOffs, this.halfmasa$query(),
                CreativeScreenMemory.currentPage((CreativeModeInventoryScreen) (Object) this));
    }
    //#endif
}
