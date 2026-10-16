package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC >= 26.0
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.0
@Mixin(GuiGraphicsExtractor.class)
//#else
//$$ @Mixin(GuiGraphics.class)
//#endif
public abstract class RemoveItemCooldownOverlayMixin
{
    //#if MC >= 1.21.3
    //#if MC >= 26.0
    @Inject(method = "itemCooldown", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "renderItemCooldown", at = @At("HEAD"), cancellable = true)
    //#endif
    private void halfmasa$hideCooldownOverlay(CallbackInfo ci)
    {
        if (Configs.REMOVE_COOLDOWN_ANIMATIONS.getBooleanValue() && Configs.REMOVE_ITEM_COOLDOWN_OVERLAY.getBooleanValue()) ci.cancel();
    }
    //#else
    //$$ @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
    //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"))
    //$$ private float halfmasa$hideLegacyCooldown(float original)
    //$$ {
    //$$ return Configs.REMOVE_COOLDOWN_ANIMATIONS.getBooleanValue() && Configs.REMOVE_ITEM_COOLDOWN_OVERLAY.getBooleanValue() ? 0 : original;
    //$$ }
    //#endif
}
//#endif
