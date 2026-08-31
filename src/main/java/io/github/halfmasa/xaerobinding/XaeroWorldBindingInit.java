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
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
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
import io.github.halfmasa.xaerobinding.feature.bridging.BridgingAssist;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.gui.KeybindCustomizationScreen;
import io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen;
import io.github.halfmasa.xaerobinding.waypoint.WaypointClientActions;
import io.github.halfmasa.xaerobinding.waypoint.WaypointBundleService.ExportScope;
import io.github.halfmasa.xaerobinding.binding.WorldBindingStore;

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
        Configs.IMPORT_WAYPOINT_BUNDLE.setAction(0, WaypointClientActions::importBundle);
        Configs.EXPORT_ALL_DIMENSIONS.setAction(0,
                () -> WaypointClientActions.exportToClipboard(ExportScope.ALL_DIMENSIONS));
        Configs.EXPORT_ALL_DIMENSIONS.setAction(1,
                () -> WaypointClientActions.exportToFile(ExportScope.ALL_DIMENSIONS));
        Configs.EXPORT_CURRENT_DIMENSION.setAction(0,
                () -> WaypointClientActions.exportToClipboard(ExportScope.CURRENT_DIMENSION));
        Configs.EXPORT_CURRENT_DIMENSION.setAction(1,
                () -> WaypointClientActions.exportToFile(ExportScope.CURRENT_DIMENSION));
        Configs.EXPORT_CURRENT_SET.setAction(0,
                () -> WaypointClientActions.exportToClipboard(ExportScope.CURRENT_SET));
        Configs.EXPORT_CURRENT_SET.setAction(1,
                () -> WaypointClientActions.exportToFile(ExportScope.CURRENT_SET));
        Configs.DEDUPE_WAYPOINTS.setAction(0, WaypointClientActions::dedupeCurrentSet);
        Configs.DEDUPE_WAYPOINTS.setAction(1, WaypointClientActions::dedupeAll);
        Configs.WAYPOINT_HISTORY.setAction(0, WaypointClientActions::undo);
        Configs.WAYPOINT_HISTORY.setAction(1, WaypointClientActions::redo);
        registerTrigger(Configs.GIVE_FULL_INVENTORY, GiveFullInventory::onKeybind);
        registerTrigger(Configs.REPORT_ELYTRA_TIME, ElytraTimeService::reportEquippedElytra);
        registerTrigger(Configs.OPEN_KEYBIND_EDITOR, () -> {
            GuiBase.openGui(new KeybindCustomizationScreen());
            return true;
        });
        registerTrigger(Configs.OPEN_KEYMAP_BROWSER, () -> {
            GuiBase.openGui(new KeymapBrowserScreen());
            return true;
        });
        registerTrigger(Configs.RELOAD_KEYBIND_DATA, KeybindCustomizationStore.getInstance()::reload);
        registerTrigger(Configs.CLEAR_SERVER_ICON_CACHE, ServerIconCache::requestClear);
        registerTrigger(Configs.CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION, ItemManagerHistoryOverlay::cyclePosition);
        Configs.CONTINGAME_IME.getKeybind().setCallback((action, key) -> ImeService.getInstance().onModeHotkey());
        //#if MC >= 26.1
        Configs.CONTINGAME_IME.setComment("halfmasa.config.ported.comment.contingameIme.mc261");
        //#endif
        Configs.KEEP_MOD_MENU_SCROLL.setValueChangeCallback(config -> {
            if (!Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue()) ConfigScrollMemory.clear();
        });
        Configs.CJK_LATIN_SPACING.setValueChangeCallback(config -> Minecraft.getInstance().reloadResourcePacks());
        Configs.CJK_LATIN_SPACING_TRANSLATIONS.setValueChangeCallback(
                config -> Minecraft.getInstance().reloadResourcePacks());
        Configs.ENABLE_WORLD_BINDING.setValueChangeCallback(config -> WorldBindingStore.ensureCurrentWorldBinding());
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
        TickHandler.getInstance().registerClientTickHandler(KeybindPieManager.getInstance());
        TickHandler.getInstance().registerClientTickHandler(ImeService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(CooldownAutoAttack.getInstance());
        TickHandler.getInstance().registerClientTickHandler(BridgingAssist.getInstance());
        TickHandler.getInstance().registerClientTickHandler(FastLoadingController.getInstance());
        TickHandler.getInstance().registerClientTickHandler(EntityRenderAggregation.getInstance());
        TickHandler.getInstance().registerClientTickHandler(ItemSearchHistoryService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(WorldBindingStore.getInstance());
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
            MinecraftClientCompat.reloadLevelRenderer(client);
        }
    }
}
