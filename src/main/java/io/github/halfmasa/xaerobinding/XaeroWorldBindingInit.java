package io.github.halfmasa.xaerobinding;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ActionHotkey;
import io.github.halfmasa.xaerobinding.feature.GiveFullInventory;
import io.github.halfmasa.xaerobinding.feature.ElytraTimeService;
import io.github.halfmasa.xaerobinding.feature.ImeService;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;
import io.github.halfmasa.xaerobinding.feature.CooldownAutoAttack;
import io.github.halfmasa.xaerobinding.feature.FastLoadingController;
import io.github.halfmasa.xaerobinding.feature.ServerIconCache;
import io.github.halfmasa.xaerobinding.feature.EntityRenderAggregation;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService;
import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.gui.KeybindCustomizationScreen;
import io.github.halfmasa.xaerobinding.waypoint.WaypointChatSender;
import io.github.halfmasa.xaerobinding.waypoint.WaypointClientActions;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;

final class XaeroWorldBindingInit implements IInitializationHandler, IKeybindProvider
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(XaeroWorldBinding.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                XaeroWorldBinding.MOD_ID,
                XaeroWorldBinding.MOD_NAME,
                HalfMasaConfigScreen::new));

        Configs.OPEN_TOOLS.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new HalfMasaConfigScreen());
            return true;
        });
        registerTrigger(Configs.COPY_WAYPOINT_BUNDLE, WaypointClientActions::copyBundle);
        registerTrigger(Configs.IMPORT_WAYPOINT_BUNDLE, WaypointClientActions::importBundle);
        registerTrigger(Configs.SHARE_CURRENT_SET, WaypointClientActions::shareCurrentSet);
        registerTrigger(Configs.SHARE_ALL_WAYPOINTS, WaypointClientActions::shareAll);
        registerTrigger(Configs.DEDUPE_CURRENT_SET, WaypointClientActions::dedupeCurrentSet);
        registerTrigger(Configs.DEDUPE_ALL_WAYPOINTS, WaypointClientActions::dedupeAll);
        registerTrigger(Configs.GIVE_FULL_INVENTORY, GiveFullInventory::onKeybind);
        registerTrigger(Configs.REPORT_ELYTRA_TIME, ElytraTimeService::reportEquippedElytra);
        registerTrigger(Configs.OPEN_KEYBIND_EDITOR, () -> {
            GuiBase.openGui(new KeybindCustomizationScreen());
            return true;
        });
        registerTrigger(Configs.RELOAD_KEYBIND_DATA, KeybindCustomizationStore.getInstance()::reload);
        registerTrigger(Configs.CLEAR_SERVER_ICON_CACHE, ServerIconCache::clear);
        registerTrigger(Configs.CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION, ItemManagerHistoryOverlay::cyclePosition);
        Configs.CONTINGAME_IME.getKeybind().setCallback((action, key) -> ImeService.getInstance().onModeHotkey());
        Configs.KEEP_MOD_MENU_SCROLL.setValueChangeCallback(config -> {
            if (!Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue()) ConfigScrollMemory.clear();
        });
        Configs.DISABLE_FLUID_RENDERING.setValueChangeCallback(config -> refreshWorldRendering());
        Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.setValueChangeCallback(config -> refreshWorldRendering());
        Configs.ENTITY_RENDER_AGGREGATION.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_RADIUS.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_THRESHOLD.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_LIST_MODE.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_WHITELIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ENTITY_AGGREGATION_BLACKLIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clear());
        Configs.ITEM_SEARCH_HISTORY.setValueChangeCallback(config -> {
            ItemSearchHistoryService.getInstance().flush();
        });
        Configs.ITEM_MANAGER_RECIPE_HISTORY.setValueChangeCallback(config -> {
            ItemSearchHistoryService.getInstance().flush();
            ItemManagerHistoryOverlay.reset();
        });
        Configs.ITEM_MANAGER_RECIPE_HISTORY_ROWS.setValueChangeCallback(config -> ItemManagerHistoryOverlay.resetBounds());
        Configs.ITEM_MANAGER_RECIPE_HISTORY_POSITION.setValueChangeCallback(config -> ItemManagerHistoryOverlay.resetBounds());
        InputEventHandler.getKeybindManager().registerKeybindProvider(this);
        TickHandler.getInstance().registerClientTickHandler(WaypointChatSender.getInstance());
        TickHandler.getInstance().registerClientTickHandler(KeybindPieManager.getInstance());
        TickHandler.getInstance().registerClientTickHandler(ImeService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(CooldownAutoAttack.getInstance());
        TickHandler.getInstance().registerClientTickHandler(FastLoadingController.getInstance());
        TickHandler.getInstance().registerClientTickHandler(EntityRenderAggregation.getInstance());
        TickHandler.getInstance().registerClientTickHandler(ItemSearchHistoryService.getInstance());
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (var hotkey : Configs.HOTKEYS)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(XaeroWorldBinding.MOD_NAME, "halfmasa.hotkeys.category", Configs.HOTKEYS);
    }

    private static void registerTrigger(ActionHotkey config, BooleanSupplier action)
    {
        config.setAction(action);
        config.getKeybind().setCallback((keyAction, keybind) -> {
            config.trigger();
            return true;
        });
    }

    private static void refreshWorldRendering()
    {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null)
        {
            client.levelRenderer.allChanged();
        }
    }
}
