package io.github.halfmasa.xaerobinding.feature;

import java.util.Optional;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import io.github.halfmasa.xaerobinding.config.Configs;

/**
 * Creates filled container items in the selected creative inventory slot.
 * Behavior is adapted from TechUtils' Unlicense giveFullInv feature.
 */
public final class GiveFullInventory
{
    private GiveFullInventory()
    {
    }

    public static boolean onKeybind()
    {
        if (!Configs.ENABLE_GIVE_FULL_INVENTORY.getBooleanValue())
        {
            error("disabled");
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null)
        {
            return false;
        }
        if (!player.isCreative())
        {
            error("creative_only");
            return false;
        }

        Optional<ItemStack> result = create(player.getMainHandItem(), player.getOffhandItem());
        if (result.isEmpty())
        {
            return false;
        }

        //#if MC >= 1.21.8
        int selectedSlot = player.getInventory().getSelectedSlot();
        //#else
        //$$ int selectedSlot = player.getInventory().selected;
        //#endif
        ItemStack created = result.get();
        player.getInventory().setItem(selectedSlot, created);
        player.getInventory().setChanged();
        minecraft.gameMode.handleCreativeModeItemAdd(created, 36 + selectedSlot);
        player.getInventory().setItem(selectedSlot, created);
        player.getInventory().setChanged();
        return true;
    }

    static Optional<ItemStack> create(ItemStack mainHand, ItemStack offHand)
    {
        if (mainHand.isEmpty())
        {
            error("empty_hand");
            return Optional.empty();
        }

        if (isShulkerBox(mainHand))
        {
            return createFromShulker(mainHand, offHand);
        }

        if (Configs.FILL_SAFETY.getBooleanValue() && hasNestedContents(mainHand))
        {
            error("nested_container");
            return Optional.empty();
        }

        ItemStack fullStack = mainHand.copyWithCount(mainHand.getMaxStackSize());
        return Optional.of(resolveTarget(offHand, GiveFullInventory::fillShulker)
                .apply(fullStack));
    }

    private static Optional<ItemStack> createFromShulker(ItemStack mainHand, ItemStack offHand)
    {
        if (Configs.FILL_SAFETY.getBooleanValue() && isShulkerBox(offHand))
        {
            error("nested_shulker");
            return Optional.empty();
        }

        ItemStack stack = containerHasItems(mainHand) ? mainHand.copyWithCount(1) : mainHand.copyWithCount(64);
        return Optional.of(resolveTarget(offHand, GiveFullInventory::fillChest)
                .apply(stack));
    }

    private static Function<ItemStack, ItemStack> resolveTarget(
            ItemStack offHand,
            Function<ItemStack, ItemStack> fallback)
    {
        if (offHand.isEmpty())
        {
            return fallback;
        }
        if (offHand.getItem() instanceof BundleItem)
        {
            return stack -> fillBundle(offHand, stack);
        }
        if (offHand.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock provider)
        {
            BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
            if (blockEntity instanceof Container container)
            {
                return stack -> fillContainer(offHand.copyWithCount(1), blockEntity, container, stack);
            }
        }
        return fallback;
    }

    private static ItemStack fillShulker(ItemStack contents)
    {
        ShulkerBoxBlockEntity box = new ShulkerBoxBlockEntity(
                BlockPos.ZERO,
                Blocks.SHULKER_BOX.defaultBlockState());
        return fillContainer(Items.SHULKER_BOX.getDefaultInstance(), box, box, contents);
    }

    private static ItemStack fillChest(ItemStack contents)
    {
        ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        return fillContainer(Items.CHEST.getDefaultInstance(), chest, chest, contents);
    }

    private static ItemStack fillContainer(
            ItemStack result,
            BlockEntity blockEntity,
            Container container,
            ItemStack contents)
    {
        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            container.setItem(slot, contents.copy());
        }
        result.applyComponents(blockEntity.collectComponents());
        return result;
    }

    private static ItemStack fillBundle(ItemStack originalBundle, ItemStack contents)
    {
        ItemStack bundle = originalBundle.copyWithCount(1);
        BundleContents existing = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        BundleContents.Mutable mutable = new BundleContents.Mutable(existing);
        for (int index = 0; index < Configs.BUNDLE_FILL.getIntegerValue(); index++)
        {
            mutable.tryInsert(contents.copy());
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        return bundle;
    }

    private static boolean hasNestedContents(ItemStack stack)
    {
        return containerHasItems(stack) || bundleHasItems(stack);
    }

    private static boolean containerHasItems(ItemStack stack)
    {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        //#if MC >= 26.0
        return contents != null && contents.nonEmptyItemCopyStream().findAny().isPresent();
        //#else
        //$$ return contents != null && contents.nonEmptyStream().findAny().isPresent();
        //#endif
    }

    private static boolean bundleHasItems(ItemStack stack)
    {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents != null && !contents.isEmpty();
    }

    private static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static void error(String key)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
        {
            //#if MC >= 26.0
            player.sendOverlayMessage(
                    Component.translatable("halfmasa.feature.give_full_inventory." + key)
                            .withStyle(ChatFormatting.DARK_RED));
            //#else
            //$$ player.displayClientMessage(
            //$$         Component.translatable("halfmasa.feature.give_full_inventory." + key)
            //$$                 .withStyle(ChatFormatting.DARK_RED),
            //$$         true);
            //#endif
        }
    }
}
