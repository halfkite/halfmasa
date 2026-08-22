package io.github.halfmasa.xaerobinding.feature;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class ElytraTimeService
{
    private ElytraTimeService()
    {
    }

    public static boolean reportEquippedElytra()
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null)
        {
            return false;
        }

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() != Items.ELYTRA)
        {
            //#if MC >= 26.0
            player.sendSystemMessage(
                    Component.translatable("halfmasa.feature.elytra_time.no_elytra").withStyle(ChatFormatting.RED));
            //#else
            //$$ player.displayClientMessage(
            //$$         Component.translatable("halfmasa.feature.elytra_time.no_elytra").withStyle(ChatFormatting.RED),
            //$$         false);
            //#endif
            return false;
        }

        TimeDetails details = calculate(stack, minecraft.level);
        //#if MC >= 26.0
        player.sendSystemMessage(
                Component.translatable(
                        "halfmasa.feature.elytra_time.report",
                        formatTime(details.seconds()),
                        details.percent()).withStyle(ChatFormatting.GREEN));
        //#else
        //$$ player.displayClientMessage(
        //$$         Component.translatable(
        //$$                 "halfmasa.feature.elytra_time.report",
        //$$                 formatTime(details.seconds()),
        //$$                 details.percent()).withStyle(ChatFormatting.GREEN),
        //$$         false);
        //#endif
        return true;
    }

    public static void addTooltip(ItemStack stack, List<Component> lines)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Configs.ELYTRA_TIME_TOOLTIP.getBooleanValue()
                || stack.getItem() != Items.ELYTRA
                || minecraft.level == null)
        {
            return;
        }

        TimeDetails details = calculate(stack, minecraft.level);
        Component line = Component.translatable(
                "halfmasa.feature.elytra_time.tooltip",
                formatTime(details.seconds()),
                details.percent()).withStyle(ChatFormatting.GREEN);
        lines.add(Math.min(1, lines.size()), line);
    }

    private static TimeDetails calculate(ItemStack stack, Level level)
    {
        int unbreaking = level.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(Enchantments.UNBREAKING))
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack))
                .orElse(0);

        int totalSeconds = Math.max(0, stack.getMaxDamage() * (unbreaking + 1) - 1);
        int remainingDurability = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        int remainingSeconds = Math.max(0, remainingDurability * (unbreaking + 1) - 1);
        int percent = totalSeconds == 0 ? 0 : Math.round(remainingSeconds * 100.0F / totalSeconds);
        return new TimeDetails(remainingSeconds, percent);
    }

    private static String formatTime(int seconds)
    {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private record TimeDetails(int seconds, int percent)
    {
    }
}
