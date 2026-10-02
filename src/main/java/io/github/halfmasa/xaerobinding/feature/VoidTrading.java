package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
//#if MC >= 1.21.2
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
//#else
//$$ import net.minecraft.world.entity.vehicle.Boat;
//#endif
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.npc.villager.Villager;
//#if MC >= 26.0
import net.minecraft.world.inventory.ContainerInput;
//#else
//$$ import net.minecraft.world.inventory.ClickType;
//#endif
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.EntityHitResult;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.VoidTradeRecoveryMode;
import io.github.halfmasa.xaerobinding.feature.VoidTradingPayloads.MaterialResult;

/** Coordinates fake-player handling while a mounted villager's trade screen is open. */
public final class VoidTrading implements IClientTickHandler
{
    private static final VoidTrading INSTANCE = new VoidTrading();
    private static final int SPAWN_MOUNT_TIMEOUT_TICKS = 1200;
    private static final int TRADE_MENU_TIMEOUT_TICKS = 40;
    private static final int TRADE_RESULT_TIMEOUT_TICKS = 60;
    private static final int AUTO_OPEN_RETRY_TICKS = 20;
    private static final int MATERIAL_PREPARATION_TIMEOUT_TICKS = 100;

    private static Screen activeTradeScreen;
    private static List<String> disconnectedFakePlayers = List.of();
    private static VoidTradeRecoveryMode activeRecoveryMode = VoidTradeRecoveryMode.REJOIN;
    private static AutoTradeSession autoTradeSession;
    private static int autoOpenRetryTicks;
    private static final LinkedHashMap<String, Integer> pendingMounts = new LinkedHashMap<>();
    private static ClientLevel pendingMountLevel;
    private static Villager recentlyInteractedVillager;
    private static long villagerInteractionTick;

    private VoidTrading()
    {
    }

    public static VoidTrading getInstance()
    {
        return INSTANCE;
    }

    public static void onScreenChanged(Minecraft client, Screen nextScreen)
    {
        if (activeTradeScreen != null && activeTradeScreen != nextScreen)
        {
            restoreFakePlayers(client);
        }

        if (nextScreen == activeTradeScreen)
        {
            return;
        }

        if (!(nextScreen instanceof MerchantScreen))
        {
            clearRecentVillager();
            return;
        }

        Villager villager = takeRecentVillager(client);
        if (villager == null && client.hitResult instanceof EntityHitResult hit &&
                hit.getEntity() instanceof Villager hitVillager)
        {
            villager = hitVillager;
        }
        handleTradeScreen(client, nextScreen, villager);
    }

    /** Captures the exact villager that was interacted with before the server opens its trade screen. */
    public static void onVillagerInteraction(Minecraft client, Entity target)
    {
        if (Configs.VOID_TRADING.getBooleanValue() && client.level != null && target instanceof Villager villager)
        {
            recentlyInteractedVillager = villager;
            villagerInteractionTick = client.level.getGameTime();
        }
        else
        {
            clearRecentVillager();
        }
    }

    public static boolean cancelAutoOpen(Minecraft client)
    {
        boolean autoOpenEnabled = Configs.VOID_TRADING_AUTO_OPEN.getBooleanValue();
        boolean merchantScreenOpen = io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(client) instanceof MerchantScreen;
        if (!autoOpenEnabled && !merchantScreenOpen)
        {
            return false;
        }

        Configs.VOID_TRADING_AUTO_OPEN.setBooleanValue(false);
        autoOpenRetryTicks = 0;
        if (merchantScreenOpen)
        {
            io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.setScreen(client, null);
            return true;
        }
        return false;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        tickAutoOpen(client);
        tickAutoTrade(client);
        tickPendingMounts(client);
    }

    private static void tickAutoOpen(Minecraft client)
    {
        if (!Configs.VOID_TRADING.getBooleanValue() || !Configs.VOID_TRADING_AUTO_OPEN.getBooleanValue() ||
                client.player == null || client.level == null || client.gameMode == null)
        {
            autoOpenRetryTicks = 0;
            return;
        }

        if (io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(client) != null)
        {
            return;
        }

        if (autoOpenRetryTicks > 0)
        {
            autoOpenRetryTicks--;
            return;
        }

        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Villager villager)
        {
            autoOpenRetryTicks = AUTO_OPEN_RETRY_TICKS;
            //#if MC >= 26.0
            client.gameMode.interact(client.player, villager, hit, InteractionHand.MAIN_HAND);
            //#else
            //$$ if (!client.gameMode.interactAt(client.player, villager, hit, InteractionHand.MAIN_HAND).consumesAction())
            //$$     client.gameMode.interact(client.player, villager, InteractionHand.MAIN_HAND);
            //#endif
        }
    }

    private static Villager takeRecentVillager(Minecraft client)
    {
        Villager villager = recentlyInteractedVillager;
        long interactionTick = villagerInteractionTick;
        clearRecentVillager();
        if (villager == null || client.level == null)
        {
            return null;
        }

        long age = client.level.getGameTime() - interactionTick;
        return age >= 0 && age <= 100 && villager.level() == client.level
                ? villager : null;
    }

    private static void clearRecentVillager()
    {
        recentlyInteractedVillager = null;
        villagerInteractionTick = 0L;
    }

    private static void handleTradeScreen(Minecraft client, Screen screen, Villager villager)
    {
        if (!Configs.VOID_TRADING.getBooleanValue() || client.player == null || client.level == null ||
                client.getConnection() == null || villager == null)
        {
            return;
        }

        Entity vehicle = villager.getVehicle();
        //#if MC >= 1.21.2
        if (!(vehicle instanceof AbstractBoat) && !(vehicle instanceof AbstractMinecart))
        //#else
        //$$ if (!(vehicle instanceof Boat) && !(vehicle instanceof AbstractMinecart))
        //#endif
        {
            return;
        }

        Set<String> names = readConfiguredNames();
        if (Configs.VOID_TRADING_AUTO_DETECT_FAKE_PLAYERS.getBooleanValue())
        {
            addVehiclePlayerNames(vehicle, client.player, names);
        }
        boolean autoTrade = Configs.VOID_TRADING_AUTO_TRADE.getBooleanValue();
        if (names.isEmpty() && !autoTrade)
        {
            return;
        }

        activeTradeScreen = screen;
        disconnectedFakePlayers = List.copyOf(names);
        activeRecoveryMode = (VoidTradeRecoveryMode) Configs.VOID_TRADING_RECOVERY_MODE.getOptionListValue();

        if (autoTrade)
        {
            autoTradeSession = new AutoTradeSession(
                    (MerchantScreen) screen,
                    villager,
                    client.level,
                    disconnectedFakePlayers,
                    readTradeIndices(),
                    Configs.VOID_TRADING_TRADE_SPECIFIED_ITEMS.getBooleanValue(),
                    readTradeItemIds(),
                    Configs.VOID_TRADING_QUICK_SHULKER.getBooleanValue(),
                    Configs.VOID_TRADING_AUTO_UNCRAFT_EMERALD_BLOCKS.getBooleanValue());
        }

        ClientPacketListener connection = client.getConnection();
        for (String name : disconnectedFakePlayers)
        {
            connection.sendCommand("player " + name + " kill");
            XaeroWorldBinding.LOGGER.info("Void trading: sent /player {} kill", name);
        }
    }

    private static Set<String> readConfiguredNames()
    {
        Set<String> names = new LinkedHashSet<>();
        for (String name : Configs.VOID_TRADING_FAKE_PLAYER_NAMES.getStringValue().split("[,;\\s]+"))
        {
            String trimmed = name.trim();
            if (isValidPlayerName(trimmed))
            {
                names.add(trimmed);
            }
        }
        return names;
    }

    private static void addVehiclePlayerNames(Entity vehicle, Player localPlayer, Set<String> names)
    {
        for (Entity passenger : vehicle.getPassengers())
        {
            if (passenger instanceof Player player && player != localPlayer)
            {
                String name = player.getName().getString();
                if (isValidPlayerName(name))
                {
                    names.add(name);
                }
            }
            addVehiclePlayerNames(passenger, localPlayer, names);
        }
    }

    private static boolean isValidPlayerName(String name)
    {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private static List<Integer> readTradeIndices()
    {
        Set<Integer> indices = new LinkedHashSet<>();
        for (String token : Configs.VOID_TRADING_TRADE_INDICES.getStringValue().split("[,;\\s]+"))
        {
            try
            {
                int index = Integer.parseInt(token.trim());
                if (index > 0)
                {
                    indices.add(index);
                }
            }
            catch (NumberFormatException ignored)
            {
                // Ignore empty or malformed entries so a typo cannot send an invalid command.
            }
        }
        return List.copyOf(indices);
    }

    private static Set<String> readTradeItemIds()
    {
        Set<String> itemIds = new LinkedHashSet<>();
        for (String configuredEntry : Configs.VOID_TRADING_TRADE_ITEMS.getStrings())
        {
            for (String token : configuredEntry.split("[,;\\s]+"))
            {
                String itemId = token.trim().toLowerCase(Locale.ROOT);
                if (itemId.isEmpty())
                {
                    continue;
                }
                if (!itemId.contains(":"))
                {
                    itemId = "minecraft:" + itemId;
                }
                if (itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                {
                    itemIds.add(itemId);
                }
            }
        }
        return Set.copyOf(itemIds);
    }

    private static void tickPendingMounts(Minecraft client)
    {
        if (pendingMounts.isEmpty())
        {
            return;
        }

        if (client.player == null || client.level == null || client.getConnection() == null ||
                (pendingMountLevel != null && pendingMountLevel != client.level))
        {
            pendingMounts.clear();
            pendingMountLevel = null;
            return;
        }

        ClientPacketListener connection = client.getConnection();
        for (String name : List.copyOf(pendingMounts.keySet()))
        {
            int waitedTicks = pendingMounts.get(name);
            if (isPlayerOnline(client, name))
            {
                connection.sendCommand("player " + name + " mount");
                XaeroWorldBinding.LOGGER.info("Void trading: {} is online; sent /player {} mount", name, name);
                pendingMounts.remove(name);
            }
            else if (waitedTicks >= SPAWN_MOUNT_TIMEOUT_TICKS)
            {
                XaeroWorldBinding.LOGGER.warn(
                        "Void trading: {} did not appear online after /player {} spawn; skipped mount",
                        name,
                        name);
                pendingMounts.remove(name);
            }
            else
            {
                pendingMounts.put(name, waitedTicks + 1);
            }
        }

        if (pendingMounts.isEmpty())
        {
            pendingMountLevel = null;
        }
    }

    private static void tickAutoTrade(Minecraft client)
    {
        AutoTradeSession session = autoTradeSession;
        if (session == null)
        {
            return;
        }

        if (client.player == null || client.level != session.level || io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(client) != session.screen ||
                !(client.player.containerMenu instanceof MerchantMenu menu) || client.gameMode == null ||
                client.getConnection() == null)
        {
            autoTradeSession = null;
            return;
        }

        if (!session.triggered)
        {
            boolean villagerUnloaded = session.villager.isRemoved() ||
                    session.level.getEntity(session.villager.getId()) != session.villager;
            boolean fakePlayersOffline = !session.fakePlayers.isEmpty() &&
                    session.fakePlayers.stream().noneMatch(name -> isPlayerOnline(client, name));
            if (!villagerUnloaded && !fakePlayersOffline)
            {
                return;
            }

            session.triggered = true;
            XaeroWorldBinding.LOGGER.info(
                    "Void trading: starting local merchant trades after {}",
                    villagerUnloaded ? "the villager unloaded" : "all configured fake players went offline");
            if (session.filterByTradeItem && session.tradeItemIds.isEmpty())
            {
                XaeroWorldBinding.LOGGER.warn(
                        "Void trading: trade-item filtering is enabled, but the item whitelist is empty");
                autoTradeSession = null;
                return;
            }
            if (session.filterByTradeItem)
            {
                List<Integer> matchingRows = new ArrayList<>();
                for (int i = 0; i < menu.getOffers().size(); i++)
                {
                    if (session.tradeItemIds.contains(getTradeItemId(menu.getOffers().get(i).getResult())))
                    {
                        matchingRows.add(i + 1);
                    }
                }
                session.tradeIndices = List.copyOf(matchingRows);
                if (session.tradeIndices.isEmpty())
                {
                    XaeroWorldBinding.LOGGER.warn(
                            "Void trading: none of this villager's {} offers match the item whitelist {}",
                            menu.getOffers().size(),
                            session.tradeItemIds);
                    autoTradeSession = null;
                    return;
                }
                XaeroWorldBinding.LOGGER.info(
                        "Void trading: item whitelist matched trade rows {}",
                        session.tradeIndices);
            }
            if (session.tradeIndices.isEmpty())
            {
                XaeroWorldBinding.LOGGER.warn(
                        "Void trading: auto-trade is enabled, but no valid 1-based trade indices were configured");
                autoTradeSession = null;
                return;
            }

            if (session.prepareQuickShulker || session.uncraftEmeraldBlocks)
            {
                int requestId = 0;
                try
                {
                    requestId = VoidTradingClientNetwork.requestMaterials(
                            session.prepareQuickShulker,
                            session.uncraftEmeraldBlocks);
                }
                catch (LinkageError | RuntimeException error)
                {
                    XaeroWorldBinding.LOGGER.warn(
                            "Void trading: Fabric networking is unavailable for server-side material preparation",
                            error);
                }

                if (requestId == 0)
                {
                    sendNotification(client, net.minecraft.network.chat.Component.translatable(
                            "halfmasa.void_trading.server_support_missing"));
                }
                else
                {
                    session.materialRequestId = requestId;
                    session.materialWaitTicks = 0;
                    session.phase = AutoTradePhase.WAIT_FOR_MATERIALS;
                    return;
                }
            }

        }

        if (session.phase == AutoTradePhase.WAIT_FOR_MATERIALS)
        {
            if (++session.materialWaitTicks >= MATERIAL_PREPARATION_TIMEOUT_TICKS)
            {
                sendNotification(client, net.minecraft.network.chat.Component.translatable(
                        "halfmasa.void_trading.server_support_timeout"));
                session.phase = AutoTradePhase.SELECT_OFFER;
            }
            else
            {
                return;
            }
        }

        if (session.phase == AutoTradePhase.DROP_ITEMS)
        {
            if (!dropNextTradedItem(client, menu, session))
            {
                finishAutoTrade(client, session);
            }
            return;
        }

        if (session.tradePosition >= session.tradeIndices.size())
        {
            if (Configs.VOID_TRADING_DROP_TRADE_ITEMS.getBooleanValue() && !session.purchasedItems.isEmpty())
            {
                session.phase = AutoTradePhase.DROP_ITEMS;
            }
            else
            {
                finishAutoTrade(client, session);
            }
            return;
        }

        int configuredIndex = session.tradeIndices.get(session.tradePosition);
        int offerIndex = configuredIndex - 1;
        if (offerIndex < 0 || offerIndex >= menu.getOffers().size())
        {
            XaeroWorldBinding.LOGGER.warn(
                    "Void trading: trade row {} does not exist (villager has {} offers)",
                    configuredIndex,
                    menu.getOffers().size());
            advanceTradeOffer(session);
            return;
        }

        MerchantOffer offer = menu.getOffers().get(offerIndex);
        if (session.phase == AutoTradePhase.SELECT_OFFER)
        {
            if (session.filterByTradeItem &&
                    !session.tradeItemIds.contains(getTradeItemId(offer.getResult())))
            {
                advanceTradeOffer(session);
                return;
            }

            if (offer.isOutOfStock())
            {
                XaeroWorldBinding.LOGGER.info("Void trading: trade row {} is out of stock", configuredIndex);
                advanceTradeOffer(session);
                return;
            }

            session.usesBeforeClick = offer.getUses();
            menu.setSelectionHint(offerIndex);
            menu.tryMoveItems(offerIndex);
            client.getConnection().send(new ServerboundSelectTradePacket(offerIndex));
            session.phase = AutoTradePhase.WAIT_FOR_RESULT;
            session.waitTicks = 0;
            return;
        }

        if (session.phase == AutoTradePhase.WAIT_FOR_RESULT)
        {
            if (offer.isOutOfStock())
            {
                advanceTradeOffer(session);
                return;
            }

            if (menu.getSlot(2).hasItem())
            {
                //#if MC >= 26.0
                client.gameMode.handleContainerInput(
                        menu.containerId,
                        2,
                        0,
                        ContainerInput.QUICK_MOVE,
                        client.player);
                //#else
//$$                 client.gameMode.handleInventoryMouseClick(
//$$                         menu.containerId,
//$$                         2,
//$$                         0,
//$$                         ClickType.QUICK_MOVE,
//$$                         client.player);
                //#endif
                session.phase = AutoTradePhase.WAIT_FOR_TRADE;
                session.waitTicks = 0;
                return;
            }

            if (++session.waitTicks >= TRADE_MENU_TIMEOUT_TICKS)
            {
                XaeroWorldBinding.LOGGER.info(
                        "Void trading: no result for trade row {}; materials may be insufficient",
                        configuredIndex);
                advanceTradeOffer(session);
            }
            return;
        }

        if (offer.getUses() > session.usesBeforeClick)
        {
            session.purchasedItems.add(offer.getResult().copy());
            if (offer.isOutOfStock())
            {
                XaeroWorldBinding.LOGGER.info("Void trading: trade row {} is out of stock", configuredIndex);
                advanceTradeOffer(session);
            }
            else
            {
                session.phase = AutoTradePhase.SELECT_OFFER;
                session.waitTicks = 0;
            }
            return;
        }

        if (++session.waitTicks >= TRADE_RESULT_TIMEOUT_TICKS)
        {
            XaeroWorldBinding.LOGGER.warn(
                    "Void trading: trade row {} did not complete after a quick-move click; moving to the next row",
                    configuredIndex);
            advanceTradeOffer(session);
        }
    }

    private static void advanceTradeOffer(AutoTradeSession session)
    {
        session.tradePosition++;
        session.phase = AutoTradePhase.SELECT_OFFER;
        session.waitTicks = 0;
    }

    private static String getTradeItemId(ItemStack stack)
    {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static boolean dropNextTradedItem(Minecraft client, MerchantMenu menu, AutoTradeSession session)
    {
        while (session.dropItemPosition < session.purchasedItems.size())
        {
            ItemStack purchasedItem = session.purchasedItems.get(session.dropItemPosition);
            if (session.dropRemaining == 0)
            {
                session.dropRemaining = purchasedItem.getCount();
            }

            for (int slotIndex = 3; slotIndex < menu.slots.size(); slotIndex++)
            {
                ItemStack inventoryStack = menu.getSlot(slotIndex).getItem();
                if (inventoryStack.isEmpty() || !ItemStack.isSameItemSameComponents(inventoryStack, purchasedItem))
                {
                    continue;
                }

                boolean dropStack = inventoryStack.getCount() <= session.dropRemaining;
                int droppedCount = dropStack ? inventoryStack.getCount() : 1;
                //#if MC >= 26.0
                client.gameMode.handleContainerInput(
                        menu.containerId,
                        slotIndex,
                        dropStack ? 1 : 0,
                        ContainerInput.THROW,
                        client.player);
                //#else
//$$                 client.gameMode.handleInventoryMouseClick(
//$$                         menu.containerId,
//$$                         slotIndex,
//$$                         dropStack ? 1 : 0,
//$$                         ClickType.THROW,
//$$                         client.player);
                //#endif
                session.dropRemaining -= droppedCount;
                if (session.dropRemaining <= 0)
                {
                    session.dropItemPosition++;
                    session.dropRemaining = 0;
                }
                return true;
            }

            XaeroWorldBinding.LOGGER.warn(
                    "Void trading: could not find purchased trade item {} in the inventory to drop",
                    getTradeItemId(purchasedItem));
            session.dropItemPosition++;
            session.dropRemaining = 0;
        }
        return false;
    }

    private static void finishAutoTrade(Minecraft client, AutoTradeSession session)
    {
        XaeroWorldBinding.LOGGER.info("Void trading: all configured merchant offers were processed");
        autoTradeSession = null;
        if (Configs.VOID_TRADING_AUTO_CLOSE.getBooleanValue() && io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(client) == session.screen)
        {
            XaeroWorldBinding.LOGGER.info("Void trading: closing the merchant screen after trades completed");
            io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.setScreen(client, null);
        }
    }

    private static void sendNotification(Minecraft client, net.minecraft.network.chat.Component message)
    {
        if (client.player == null)
        {
            return;
        }
//#if MC >= 26.1
        client.player.sendSystemMessage(message);
//#else
        //$$ client.player.displayClientMessage(message, false);
//#endif
    }

    public static void onMaterialPreparationResult(MaterialResult result)
    {
        Minecraft client = Minecraft.getInstance();
        AutoTradeSession session = autoTradeSession;
        if (session == null || session.phase != AutoTradePhase.WAIT_FOR_MATERIALS ||
                session.materialRequestId != result.requestId() || client.player == null ||
                io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat.getScreen(client) != session.screen)
        {
            return;
        }

        if (session.prepareQuickShulker && !result.quickShulkerAvailable())
        {
            sendNotification(client, net.minecraft.network.chat.Component.translatable(
                    "halfmasa.void_trading.quick_shulker_missing"));
        }
        else if (result.emeraldsPulled() > 0 || result.emeraldBlocksPulled() > 0 ||
                result.emeraldBlocksUncrafted() > 0)
        {
            sendNotification(client, net.minecraft.network.chat.Component.translatable(
                    "halfmasa.void_trading.materials_prepared",
                    result.emeraldsPulled(),
                    result.emeraldBlocksPulled(),
                    result.emeraldBlocksUncrafted()));
        }

        XaeroWorldBinding.LOGGER.info(
                "Void trading: server prepared materials (QuickShulker available={}, emeralds pulled={}, blocks pulled={}, blocks uncrafted={})",
                result.quickShulkerAvailable(),
                result.emeraldsPulled(),
                result.emeraldBlocksPulled(),
                result.emeraldBlocksUncrafted());
        session.phase = AutoTradePhase.SELECT_OFFER;
    }


    private enum AutoTradePhase
    {
        SELECT_OFFER,
        WAIT_FOR_MATERIALS,
        WAIT_FOR_RESULT,
        WAIT_FOR_TRADE,
        DROP_ITEMS
    }

    private static final class AutoTradeSession
    {
        private final MerchantScreen screen;
        private final Villager villager;
        private final ClientLevel level;
        private final List<String> fakePlayers;
        private List<Integer> tradeIndices;
        private final boolean filterByTradeItem;
        private final Set<String> tradeItemIds;
        private final boolean prepareQuickShulker;
        private final boolean uncraftEmeraldBlocks;
        private final List<ItemStack> purchasedItems = new ArrayList<>();
        private AutoTradePhase phase = AutoTradePhase.SELECT_OFFER;
        private int tradePosition;
        private int waitTicks;
        private int usesBeforeClick;
        private int dropItemPosition;
        private int dropRemaining;
        private int materialRequestId;
        private int materialWaitTicks;
        private boolean triggered;

        private AutoTradeSession(
                MerchantScreen screen,
                Villager villager,
                ClientLevel level,
                List<String> fakePlayers,
                List<Integer> tradeIndices,
                boolean filterByTradeItem,
                Set<String> tradeItemIds,
                boolean prepareQuickShulker,
                boolean uncraftEmeraldBlocks)
        {
            this.screen = screen;
            this.villager = villager;
            this.level = level;
            this.fakePlayers = fakePlayers;
            this.tradeIndices = tradeIndices;
            this.filterByTradeItem = filterByTradeItem;
            this.tradeItemIds = tradeItemIds;
            this.prepareQuickShulker = prepareQuickShulker;
            this.uncraftEmeraldBlocks = uncraftEmeraldBlocks;
        }
    }

    private static void restoreFakePlayers(Minecraft client)
    {
        List<String> names = disconnectedFakePlayers;
        VoidTradeRecoveryMode recoveryMode = activeRecoveryMode;
        activeTradeScreen = null;
        disconnectedFakePlayers = List.of();
        autoTradeSession = null;

        if (names.isEmpty() || client.player == null || client.level == null)
        {
            return;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null)
        {
            return;
        }

        for (String name : names)
        {
            if (recoveryMode == VoidTradeRecoveryMode.SPAWN_MOUNT)
            {
                connection.sendCommand("player " + name + " spawn");
                pendingMounts.put(name, 0);
                pendingMountLevel = client.level;
                XaeroWorldBinding.LOGGER.info("Void trading: sent /player {} spawn; waiting for it to appear online", name);
            }
            else
            {
                connection.sendCommand("player " + name + " rejoin");
                XaeroWorldBinding.LOGGER.info("Void trading: sent /player {} rejoin", name);
            }
        }
    }

    private static boolean isPlayerOnline(Minecraft client, String name)
    {
        ClientPacketListener connection = client.getConnection();
        return connection != null && connection.getOnlinePlayers().stream()
                .anyMatch(info -> info.getProfile().name().equalsIgnoreCase(name)) ||
                client.level.players().stream()
                        .anyMatch(player -> player.getName().getString().equalsIgnoreCase(name));
    }
}
