package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.TrialCreativeTab;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Provides the trial tab without pretending vanilla has a pagination API. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class TrialCreativeInventoryMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
{
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow protected abstract void selectTab(CreativeModeTab tab);
    @Unique private Button halfmasa$trialButton;

    protected TrialCreativeInventoryMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title)
    { super(menu, inventory, title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void halfmasa_addTrialButton(CallbackInfo ci)
    {
        if (TrialCreativeTab.isRegistered()) return;
        int x = Math.max(4, Math.min(this.width - 68, this.leftPos + this.imageWidth + 4));
        halfmasa$trialButton = this.addRenderableWidget(Button.builder(
                Component.translatable("halfmasa.creative_tab.trial"), button -> this.selectTab(TrialCreativeTab.get()))
                .bounds(x, this.topPos + 4, 64, 20).build());
        halfmasa$trialButton.visible = TrialCreativeTab.isEnabled();
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void halfmasa_refreshTrialButton(CallbackInfo ci)
    {
        if (TrialCreativeTab.isRegistered()) return;
        boolean enabled = TrialCreativeTab.isEnabled();
        if (halfmasa$trialButton != null) halfmasa$trialButton.visible = enabled;
        if (!enabled && TrialCreativeTab.isTrialTab(selectedTab)) this.selectTab(CreativeModeTabs.getDefaultTab());
    }
    @Inject(method = "containerTick", at = @At("TAIL"))
    private void halfmasa_keepTrialPageValid(CallbackInfo ci)
    {
        TrialCreativeTab.synchronizeSelection((CreativeModeInventoryScreen) (Object) this);
    }

}
//#endif
