package io.github.halfmasa.xaerobinding.mixin;

import java.util.ArrayList;
import java.util.List;

//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
//#if MC >= 26.1
import net.minecraft.world.inventory.ContainerInput;
//#else
//$$ import net.minecraft.world.inventory.ClickType;
//#endif
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CondensedCreativeManager;
import io.github.halfmasa.xaerobinding.feature.CreativeItemSearchHistory;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CondensedCreativeMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
{
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Shadow private EditBox searchBox;
    @Unique private ItemStack halfmasa$pendingHistoryStack = ItemStack.EMPTY;
    @Unique private boolean halfmasa$refreshedDuringTabSelection;

    private CondensedCreativeMixin(
            CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void halfmasa_prepareCreativeTabSelection(CallbackInfo ci)
    {
        this.halfmasa$refreshedDuringTabSelection = false;
    }

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void halfmasa_refreshCreativeSearchEntries(CallbackInfo ci)
    {
        this.halfmasa$refreshedDuringTabSelection = true;
        CondensedCreativeManager.rebuild((CreativeModeInventoryScreen) (Object) this, selectedTab, this.menu.items);
        CreativeItemSearchHistory.rebuild(
                (CreativeModeInventoryScreen) (Object) this,
                selectedTab,
                this.menu.items,
                this.halfmasa$getSearchText());
        this.menu.scrollTo(this.scrollOffs);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void halfmasa_selectCreativeTabEntries(CallbackInfo ci)
    {
        if (!this.halfmasa$refreshedDuringTabSelection)
        {
            CondensedCreativeManager.rebuild((CreativeModeInventoryScreen) (Object) this, selectedTab, this.menu.items);
            CreativeItemSearchHistory.rebuild(
                    (CreativeModeInventoryScreen) (Object) this,
                    selectedTab,
                    this.menu.items,
                    this.halfmasa$getSearchText());
            this.menu.scrollTo(this.scrollOffs);
        }
        this.halfmasa$refreshedDuringTabSelection = false;
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void halfmasa_rotateCondensedPreview(CallbackInfo ci)
    {
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        if (CreativeItemSearchHistory.isApplied(screen) &&
                Configs.CONDENSED_CREATIVE.getBooleanValue() != CondensedCreativeManager.isApplied(screen))
        {
            CreativeItemSearchHistory.restoreSource(screen, this.menu.items);
        }

        boolean transformed = CondensedCreativeManager.ensureApplied(screen, selectedTab, this.menu.items) ||
                CondensedCreativeManager.rotatePreview(screen, this.menu.items);
        if (transformed)
        {
            CreativeItemSearchHistory.rebuild(screen, selectedTab, this.menu.items, this.halfmasa$getSearchText());
        }
        if (transformed || CreativeItemSearchHistory.ensureApplied(
                screen, selectedTab, this.menu.items, this.halfmasa$getSearchText()))
        {
            this.menu.scrollTo(this.scrollOffs);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void halfmasa_toggleCondensedEntry(
            Slot slot, int slotId, int button,
            //#if MC >= 26.1
            ContainerInput clickType,
            //#else
            //$$ ClickType clickType,
            //#endif
            CallbackInfo ci)
    {
        this.halfmasa$pendingHistoryStack = ItemStack.EMPTY;
        if (slot == null)
        {
            return;
        }

        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        int visibleSlotIndex = this.menu.slots.indexOf(slot);
        if (CreativeItemSearchHistory.toggle(screen, visibleSlotIndex, this.scrollOffs, this.menu.items))
        {
            this.menu.scrollTo(this.scrollOffs);
            ci.cancel();
            return;
        }

        boolean historySlot = CreativeItemSearchHistory.isHistorySlot(screen, visibleSlotIndex, this.scrollOffs);
        if (!historySlot && CondensedCreativeManager.toggle(screen, slot.getItem(), this.menu.items))
        {
            CreativeItemSearchHistory.rebuild(screen, selectedTab, this.menu.items, this.halfmasa$getSearchText());
            this.menu.scrollTo(this.scrollOffs);
            ci.cancel();
            return;
        }

        if (!slot.getItem().isEmpty() && this.searchBox != null && !this.searchBox.getValue().trim().isEmpty() &&
                //#if MC >= 26.1
                (clickType == ContainerInput.PICKUP || clickType == ContainerInput.QUICK_MOVE ||
                        clickType == ContainerInput.SWAP || clickType == ContainerInput.CLONE))
                //#else
                //$$ (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE ||
                //$$         clickType == ClickType.SWAP || clickType == ClickType.CLONE))
                //#endif
        {
            this.halfmasa$pendingHistoryStack = slot.getItem().copyWithCount(1);
        }
    }

    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void halfmasa_recordCreativeHistory(
            Slot slot, int slotId, int button,
            //#if MC >= 26.1
            ContainerInput clickType,
            //#else
            //$$ ClickType clickType,
            //#endif
            CallbackInfo ci)
    {
        if (this.halfmasa$pendingHistoryStack.isEmpty())
        {
            return;
        }
        ItemSearchHistoryService.getInstance().record(
                ItemSearchHistoryService.Channel.CREATIVE,
                this.halfmasa$pendingHistoryStack);
        this.halfmasa$pendingHistoryStack = ItemStack.EMPTY;
        CreativeItemSearchHistory.refreshHistory(
                (CreativeModeInventoryScreen) (Object) this,
                this.menu.items);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void halfmasa_addCondensedTooltip(
            ItemStack stack, CallbackInfoReturnable<List<Component>> cir)
    {
        if (!Configs.CONDENSED_CREATIVE_TOOLTIP.getBooleanValue()) return;
        Component hint = CondensedCreativeManager.getParentHint(
                (CreativeModeInventoryScreen) (Object) this, stack);
        if (hint != null)
        {
            List<Component> tooltip = new ArrayList<>(cir.getReturnValue());
            tooltip.add(hint);
            tooltip.add(Component.translatable("halfmasa.feature.condensed_creative.source"));
            cir.setReturnValue(tooltip);
        }
    }

    //#if MC >= 26.1
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void halfmasa_renderCondensedMarkers(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "render", at = @At("TAIL"))
    //$$ private void halfmasa_renderCondensedMarkers(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        int color = Configs.CONDENSED_CREATIVE_BORDER_COLOR.getIntegerValue();
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        for (int index = 0; index < this.menu.slots.size(); index++)
        {
            Slot slot = this.menu.slots.get(index);
            if (CreativeItemSearchHistory.isHistorySlot(screen, index, this.scrollOffs)) continue;
            CondensedCreativeManager.Visual visual = CondensedCreativeManager.getVisual(screen, slot.getItem());
            if (visual == null) continue;
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            if (visual.expanded() && Configs.CONDENSED_CREATIVE_BACKGROUND.getBooleanValue())
            {
                graphics.fill(x, y, x + 16, y + 16, 0x7F111111);
            }
            if (visual.expanded() && Configs.CONDENSED_CREATIVE_BORDER.getBooleanValue())
            {
                if (!this.halfmasa_sameGroup(screen, index - 9, visual.key())) graphics.fill(x - 1, y - 1, x + 17, y, color);
                if (!this.halfmasa_sameGroup(screen, index + 9, visual.key())) graphics.fill(x - 1, y + 16, x + 17, y + 17, color);
                if (index % 9 == 0 || !this.halfmasa_sameGroup(screen, index - 1, visual.key())) graphics.fill(x - 1, y - 1, x, y + 17, color);
                if (index % 9 == 8 || !this.halfmasa_sameGroup(screen, index + 1, visual.key())) graphics.fill(x + 16, y - 1, x + 17, y + 17, color);
            }
            if (visual.parent())
            {
                //#if MC >= 26.1
                graphics.text(this.font, visual.expanded() ? "-" : "+", x + 10, y + 8, 0xFFFFFFFF, true);
                //#else
                //$$ graphics.drawString(this.font, visual.expanded() ? "-" : "+", x + 10, y + 8, 0xFFFFFFFF, true);
                //#endif
            }
        }

        if (!this.menu.slots.isEmpty())
        {
            Slot first = this.menu.slots.getFirst();
            CreativeItemSearchHistory.renderToggle(
                    screen,
                    graphics,
                    this.leftPos + first.x,
                    this.topPos + first.y,
                    this.scrollOffs);
        }
    }

    private boolean halfmasa_sameGroup(CreativeModeInventoryScreen screen, int index, String key)
    {
        if (index < 0 || index >= this.menu.slots.size()) return false;
        CondensedCreativeManager.Visual other = CondensedCreativeManager.getVisual(screen, this.menu.slots.get(index).getItem());
        return other != null && other.expanded() && other.key().equals(key);
    }

    @Unique
    private String halfmasa$getSearchText()
    {
        return this.searchBox == null ? "" : this.searchBox.getValue();
    }
}
