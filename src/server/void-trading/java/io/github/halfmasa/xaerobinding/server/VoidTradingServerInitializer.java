//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.server;

import java.lang.reflect.Method;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.halfmasa.xaerobinding.feature.VoidTradingPayloads.MaterialRequest;
import io.github.halfmasa.xaerobinding.feature.VoidTradingPayloads.MaterialResult;

/** Optional server-side support for authoritative QuickShulker extraction and emerald uncrafting. */
public final class VoidTradingServerInitializer implements ModInitializer
{
    private static final Logger LOGGER = LogManager.getLogger("halfmasa-void-trading-server");
    private static final int PLAYER_STORAGE_SLOTS = 36;
    private static Method quickieGetter;
    private static Method quickInventoryGetter;
    private static boolean quickShulkerApiChecked;
    private static boolean quickShulkerApiAvailable;

    @Override
    public void onInitialize()
    {
        // The client mod registers these codecs on a physical client (including an integrated server).
        // Register here only on a dedicated server to avoid duplicate client-side registrations.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
        {
            PayloadTypeRegistry.serverboundPlay().register(MaterialRequest.TYPE, MaterialRequest.STREAM_CODEC);
            PayloadTypeRegistry.clientboundPlay().register(MaterialResult.TYPE, MaterialResult.STREAM_CODEC);
        }
        ServerPlayNetworking.registerGlobalReceiver(MaterialRequest.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            MaterialResult result = prepareMaterials(player, request);
            context.responseSender().sendPacket(result);
        });
        LOGGER.info("Void trading server support is ready");
    }

    private static MaterialResult prepareMaterials(ServerPlayer player, MaterialRequest request)
    {
        boolean quickShulkerAvailable = request.pullQuickShulker() && isQuickShulkerApiAvailable();
        int emeraldsPulled = 0;
        int emeraldBlocksPulled = 0;
        int emeraldBlocksUncrafted = 0;

        if (!(player.containerMenu instanceof MerchantMenu))
        {
            return new MaterialResult(
                    request.requestId(), quickShulkerAvailable, 0, 0, 0);
        }

        Inventory inventory = player.getInventory();
        if (request.uncraftEmeraldBlocks())
        {
            emeraldBlocksUncrafted += uncraftInventoryBlocks(inventory);
        }

        if (request.pullQuickShulker() && quickShulkerAvailable)
        {
            for (int inventorySlot = 0; inventorySlot < storageSlotCount(inventory); inventorySlot++)
            {
                ItemStack boxStack = inventory.getItem(inventorySlot);
                Container boxInventory = getQuickShulkerInventory(player, boxStack);
                if (boxInventory == null)
                {
                    continue;
                }

                boolean boxChanged = false;
                for (int boxSlot = 0; boxSlot < boxInventory.getContainerSize(); boxSlot++)
                {
                    ItemStack source = boxInventory.getItem(boxSlot);
                    if (source.isEmpty())
                    {
                        continue;
                    }

                    if (source.is(Items.EMERALD))
                    {
                        int moved = insertFromSource(inventory, source, source.getCount());
                        if (moved > 0)
                        {
                            source.shrink(moved);
                            emeraldsPulled += moved;
                            boxChanged = true;
                        }
                    }
                    else if (source.is(Items.EMERALD_BLOCK))
                    {
                        if (request.uncraftEmeraldBlocks())
                        {
                            int blocksToUncraft = Math.min(
                                    source.getCount(),
                                    inventoryCapacity(inventory, Items.EMERALD) / 9);
                            if (blocksToUncraft > 0)
                            {
                                int converted = insertStack(
                                        inventory,
                                        new ItemStack(Items.EMERALD, blocksToUncraft * 9));
                                int convertedBlocks = converted / 9;
                                source.shrink(convertedBlocks);
                                emeraldBlocksUncrafted += convertedBlocks;
                                boxChanged |= convertedBlocks > 0;
                            }
                        }

                        if (!source.isEmpty())
                        {
                            int moved = insertFromSource(inventory, source, source.getCount());
                            if (moved > 0)
                            {
                                source.shrink(moved);
                                emeraldBlocksPulled += moved;
                                boxChanged = true;
                            }
                        }
                    }

                    if (source.isEmpty())
                    {
                        boxInventory.setItem(boxSlot, ItemStack.EMPTY);
                        boxChanged = true;
                    }
                }

                if (boxChanged)
                {
                    boxInventory.setChanged();
                }
            }
        }

        if (request.uncraftEmeraldBlocks())
        {
            emeraldBlocksUncrafted += uncraftInventoryBlocks(inventory);
        }

        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        LOGGER.info(
                "Prepared trade materials for {} (QuickShulker={}, emeralds={}, blocks={}, uncrafted={})",
                player.getName().getString(),
                quickShulkerAvailable,
                emeraldsPulled,
                emeraldBlocksPulled,
                emeraldBlocksUncrafted);
        return new MaterialResult(
                request.requestId(),
                quickShulkerAvailable,
                emeraldsPulled,
                emeraldBlocksPulled,
                emeraldBlocksUncrafted);
    }

    private static int uncraftInventoryBlocks(Inventory inventory)
    {
        int uncrafted = 0;
        for (int slot = 0; slot < storageSlotCount(inventory); slot++)
        {
            ItemStack blocks = inventory.getItem(slot);
            if (blocks.isEmpty() || !blocks.is(Items.EMERALD_BLOCK))
            {
                continue;
            }

            int blocksToUncraft = Math.min(blocks.getCount(), inventoryCapacity(inventory, Items.EMERALD) / 9);
            if (blocksToUncraft <= 0)
            {
                continue;
            }

            int inserted = insertStack(inventory, new ItemStack(Items.EMERALD, blocksToUncraft * 9));
            int convertedBlocks = inserted / 9;
            blocks.shrink(convertedBlocks);
            uncrafted += convertedBlocks;
        }
        return uncrafted;
    }

    private static int insertFromSource(Inventory inventory, ItemStack source, int requestedCount)
    {
        int count = Math.min(requestedCount, inventoryCapacity(inventory, source));
        if (count <= 0)
        {
            return 0;
        }
        return insertStack(inventory, source.copyWithCount(count));
    }

    private static int insertStack(Inventory inventory, ItemStack offered)
    {
        ItemStack remaining = offered.copy();
        int initialCount = remaining.getCount();
        int size = storageSlotCount(inventory);

        for (int slot = 0; slot < size && !remaining.isEmpty(); slot++)
        {
            ItemStack current = inventory.getItem(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining))
            {
                int room = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize()) - current.getCount();
                int moved = Math.min(Math.max(room, 0), remaining.getCount());
                if (moved > 0)
                {
                    current.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }

        for (int slot = 0; slot < size && !remaining.isEmpty(); slot++)
        {
            if (inventory.getItem(slot).isEmpty())
            {
                int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                inventory.setItem(slot, remaining.split(moved));
            }
        }

        return initialCount - remaining.getCount();
    }

    private static int inventoryCapacity(Inventory inventory, Item item)
    {
        return inventoryCapacity(inventory, new ItemStack(item));
    }

    private static int inventoryCapacity(Inventory inventory, ItemStack offered)
    {
        long capacity = 0;
        int size = storageSlotCount(inventory);
        for (int slot = 0; slot < size; slot++)
        {
            ItemStack current = inventory.getItem(slot);
            if (current.isEmpty())
            {
                capacity += Math.min(offered.getMaxStackSize(), inventory.getMaxStackSize());
            }
            else if (ItemStack.isSameItemSameComponents(current, offered))
            {
                capacity += Math.max(
                        Math.min(current.getMaxStackSize(), inventory.getMaxStackSize()) - current.getCount(), 0);
            }
        }
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    private static int storageSlotCount(Inventory inventory)
    {
        return Math.min(PLAYER_STORAGE_SLOTS, inventory.getContainerSize());
    }

    private static boolean isQuickShulkerApiAvailable()
    {
        if (quickShulkerApiChecked)
        {
            return quickShulkerApiAvailable;
        }
        quickShulkerApiChecked = true;

        if (!FabricLoader.getInstance().isModLoaded("quickshulker"))
        {
            return false;
        }

        try
        {
            ClassLoader classLoader = VoidTradingServerInitializer.class.getClassLoader();
            Class<?> registry = Class.forName(
                    "net.kyrptonaught.quickshulker.api.QuickOpenableRegistry", true, classLoader);
            Class<?> data = Class.forName(
                    "net.kyrptonaught.quickshulker.api.QuickShulkerData", true, classLoader);
            quickieGetter = registry.getMethod("getQuickie", net.minecraft.world.level.ItemLike.class);
            quickInventoryGetter = data.getMethod("getInventory", Player.class, ItemStack.class);
            quickShulkerApiAvailable = true;
        }
        catch (ReflectiveOperationException | LinkageError exception)
        {
            LOGGER.warn("QuickShulker is loaded but its inventory API could not be found", exception);
        }

        return quickShulkerApiAvailable;
    }

    private static Container getQuickShulkerInventory(Player player, ItemStack stack)
    {
        if (stack.isEmpty() || quickieGetter == null || quickInventoryGetter == null)
        {
            return null;
        }

        try
        {
            Object quickShulkerData = quickieGetter.invoke(null, stack.getItem());
            if (quickShulkerData == null)
            {
                return null;
            }
            Object inventory = quickInventoryGetter.invoke(quickShulkerData, player, stack);
            return inventory instanceof Container container ? container : null;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            LOGGER.warn("Could not read an item inventory through the QuickShulker API", exception);
            return null;
        }
    }
}

//#endif
