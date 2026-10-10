package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.feature.ElytraTimeService;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin
{
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void halfmasa$addElytraTime(
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> callback)
    {
        ElytraTimeService.addTooltip((ItemStack) (Object) this, callback.getReturnValue());
        //#if MC >= 1.21.1
        var client = net.minecraft.client.Minecraft.getInstance();
        io.github.halfmasa.xaerobinding.feature.SpawnerItemAppearance.addTooltip(
                (ItemStack) (Object) this, context.registries(),
                client.level != null ? client.level.getGameTime() : 0, callback.getReturnValue());
        //#endif
    }
}
