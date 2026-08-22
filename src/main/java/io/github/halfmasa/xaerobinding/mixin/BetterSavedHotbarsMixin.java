package io.github.halfmasa.xaerobinding.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.inventory.Hotbar;
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

@Mixin(CreativeModeInventoryScreen.class)
public abstract class BetterSavedHotbarsMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
{
    @Unique private static float halfmasa_savedHotbarScroll;
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Shadow protected abstract void selectTab(CreativeModeTab tab);

    protected BetterSavedHotbarsMixin(
            CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
    }

    //#if MC >= 26.1
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void halfmasa_editSavedHotbar(
            Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci)
    {
        this.halfmasa_handleSavedHotbarClick(slot, slotId, input == ContainerInput.CLONE, ci);
    }
    //#else
    //$$ @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    //$$ private void halfmasa_editSavedHotbar(
    //$$         Slot slot, int slotId, int button, ClickType input, CallbackInfo ci)
    //$$ {
    //$$     this.halfmasa_handleSavedHotbarClick(slot, slotId, input == ClickType.CLONE, ci);
    //$$ }
    //#endif

    @Unique
    private void halfmasa_handleSavedHotbarClick(
            Slot slot, int slotId, boolean cloneClick, CallbackInfo ci)
    {
        if (!Configs.BETTER_SAVED_HOTBARS.getBooleanValue() || selectedTab == null ||
            selectedTab.getType() != CreativeModeTab.Type.HOTBAR || slot == null || slotId < 0 || slotId >= 45)
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
        {
            return;
        }

        int page = Math.round(4.0F * this.scrollOffs);
        int row = slot.getContainerSlot() / 9 + page;
        int column = (slot.x - 9) / 18;
        if (row < 0 || row >= 9 || column < 0 || column >= 9)
        {
            return;
        }

        ItemStack carried = client.player.containerMenu.getCarried().copy();
        if (carried.isEmpty())
        {
            if (!cloneClick)
            {
                return;
            }
            ItemStack previous = slot.getItem().copy();
            this.halfmasa_storeCell(row, column, ItemStack.EMPTY);
            client.player.containerMenu.setCarried(previous);
            this.halfmasa_refreshTab();
            ci.cancel();
            return;
        }

        this.halfmasa_storeCell(row, column, carried);
        client.player.containerMenu.setCarried(ItemStack.EMPTY);
        this.halfmasa_refreshTab();
        ci.cancel();
    }

    @Unique
    private void halfmasa_storeCell(int row, int column, ItemStack value)
    {
        Minecraft client = Minecraft.getInstance();
        Inventory inventory = client.player.getInventory();
        HotbarManager manager = client.getHotbarManager();
        Hotbar hotbar = manager.get(row);
        List<ItemStack> loaded = hotbar.load(client.player.level().registryAccess());
        List<ItemStack> backup = new ArrayList<>(9);

        try
        {
            for (int index = 0; index < 9; index++)
            {
                backup.add(inventory.getItem(index).copy());
                inventory.setItem(index, loaded.get(index).copy());
            }
            inventory.setItem(column, value.copy());
            hotbar.storeFrom(inventory, client.player.level().registryAccess());
        }
        finally
        {
            for (int index = 0; index < backup.size(); index++)
            {
                inventory.setItem(index, backup.get(index));
            }
        }
        manager.save();
    }

    @Unique
    private void halfmasa_refreshTab()
    {
        float scroll = this.scrollOffs;
        halfmasa_savedHotbarScroll = scroll;
        this.selectTab(selectedTab);
        this.scrollOffs = scroll;
        this.menu.scrollTo(scroll);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void halfmasa_restoreSavedHotbarScroll(CreativeModeTab tab, CallbackInfo ci)
    {
        if (Configs.BETTER_SAVED_HOTBARS.getBooleanValue() &&
            tab.getType() == CreativeModeTab.Type.HOTBAR)
        {
            this.scrollOffs = halfmasa_savedHotbarScroll;
            this.menu.scrollTo(halfmasa_savedHotbarScroll);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void halfmasa_rememberSavedHotbarScroll(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.BETTER_SAVED_HOTBARS.getBooleanValue() && selectedTab != null &&
            selectedTab.getType() == CreativeModeTab.Type.HOTBAR)
        {
            halfmasa_savedHotbarScroll = this.scrollOffs;
        }
    }
}
