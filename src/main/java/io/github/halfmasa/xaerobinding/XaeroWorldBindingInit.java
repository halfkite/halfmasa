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
import io.github.halfmasa.xaerobinding.feature.KeybindPieManager;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;

import io.github.halfmasa.xaerobinding.feature.CooldownAutoAttack;
import io.github.halfmasa.xaerobinding.feature.FastLoadingController;
import io.github.halfmasa.xaerobinding.feature.ServerIconCache;
import io.github.halfmasa.xaerobinding.feature.EntityRenderAggregation;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService;
import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;
import io.github.halfmasa.xaerobinding.feature.VoidTrading;
import io.github.halfmasa.xaerobinding.feature.ConfluxMapTemporaryWaypoint;

import io.github.halfmasa.xaerobinding.feature.bridging.BridgingAssist;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.gui.CustomConfigGroupScreen;
import io.github.halfmasa.xaerobinding.gui.KeybindCustomizationScreen;
import io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen;
//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.config.EntityGlowMode;
import io.github.halfmasa.xaerobinding.feature.EntityGlowService;
import io.github.halfmasa.xaerobinding.feature.BlockGlowService;
import io.github.halfmasa.xaerobinding.feature.GlowRuleHotkeys;
import io.github.halfmasa.xaerobinding.gui.EntityGlowRulesScreen;
//#endif

import io.github.halfmasa.xaerobinding.waypoint.WaypointClientActions;
import io.github.halfmasa.xaerobinding.waypoint.WaypointBundleService.ExportScope;
import io.github.halfmasa.xaerobinding.binding.WorldBindingStore;

import java.util.function.BooleanSupplier;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

final class XaeroWorldBindingInit implements IInitializationHandler, IKeybindProvider
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(XaeroWorldBinding.MOD_ID, new Configs());
        //#if MC >= 1.21.1
        var feeding = io.github.halfmasa.xaerobinding.feature.RangeFeedingService.getInstance();
        registerTrigger(Configs.RANGE_FEEDING_ONCE, feeding::requestSingleFeed);
        Configs.RANGE_FEEDING_HOLD.getKeybind().setCallback((action, key) -> {
            if (action == fi.dy.masa.malilib.hotkeys.KeyAction.PRESS) feeding.setHoldActive(true);
            else if (action == fi.dy.masa.malilib.hotkeys.KeyAction.RELEASE) feeding.setHoldActive(false);
            return true;
        });
        Configs.RANGE_FEEDING.setValueChangeCallback(config -> feeding.onMasterChanged());
        Configs.RANGE_FEEDING_SKIP_BABIES.setValueChangeCallback(config -> feeding.onProtectionChanged());
        Configs.RANGE_FEEDING_INTERVAL.setValueChangeCallback(config -> feeding.onIntervalChanged());
        Configs.RANGE_FEEDING_BLACKLIST.setValueChangeCallback(config -> feeding.reloadLists());
        Configs.RANGE_FEEDING_WHITELIST.setValueChangeCallback(config -> feeding.reloadLists());
        feeding.reloadLists();
        Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind().setCallback((action, key) -> {
            if (action != fi.dy.masa.malilib.hotkeys.KeyAction.PRESS) return false;
            var mode = (io.github.halfmasa.xaerobinding.config.EntityRenderDisableMode)Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue();
            var next = mode.cycle(true);
            Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(next);
            new Configs().save();
            var client = Minecraft.getInstance();
            if (client.player != null) MinecraftClientCompat.setOverlayMessage(client, net.minecraft.network.chat.Component.translatable(
                    "halfmasa.message.entity_render_disable_mode", next.getDisplayName()), false);
            return true;
        });
        Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.EntityRenderDisableService.reload());
        Configs.ENTITY_RENDER_DISABLE_WHITELIST.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.EntityRenderDisableService.reload());
        io.github.halfmasa.xaerobinding.feature.EntityRenderDisableService.reload();
        Configs.AUTO_EAT_SOURCE_HOTKEY.getKeybind().setCallback((action, key) -> {
            var mode = (io.github.halfmasa.xaerobinding.config.AutoEatSourceMode) Configs.AUTO_EAT_SOURCE.getOptionListValue();
            Configs.AUTO_EAT_SOURCE.setOptionListValue(mode.cycle(true));
            return true;
        });
        Configs.AUTO_EAT.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        Configs.AUTO_EAT_SOURCE.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        Configs.AUTO_EAT_FILTER_MODE.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        Configs.AUTO_EAT_WHITELIST.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        Configs.AUTO_EAT_BLACKLIST.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        Configs.AUTO_EAT_SILENT.setValueChangeCallback(config -> io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance().onSettingsChanged());
        //#endif
        //#if MC >= 1.21.1
        io.github.halfmasa.xaerobinding.feature.ItemDespawnDataBridge.initialize();
        Configs.ITEM_DESPAWN_TIMER.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ItemDespawnTimerService.getInstance().clear());
        Configs.ITEM_DESPAWN_SOURCE.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ItemDespawnTimerService.getInstance().clear());
        //#endif
        if (FabricLoader.getInstance().isModLoaded("fabric-api"))
        {
            io.github.halfmasa.xaerobinding.feature.VoidTradingClientNetwork.initialize();
            io.github.halfmasa.xaerobinding.feature.LitematicaRefillNetwork.initialize();
            //#if MC >= 1.21.1
            io.github.halfmasa.xaerobinding.feature.HalfMasaSupportNetwork.initialize();
            //#endif
        }

        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                XaeroWorldBinding.MOD_ID,
                XaeroWorldBinding.MOD_NAME,
                HalfMasaConfigScreen::new));

        Configs.OPEN_TOOLS.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new HalfMasaConfigScreen());
            return true;
        });
        //#if MC >= 1.21.1
        Configs.ENTITY_GLOW_MODE_HOTKEY.getKeybind().setCallback((action, key) -> {
            EntityGlowMode current = (EntityGlowMode) Configs.ENTITY_GLOW_MODE.getOptionListValue();
            Configs.ENTITY_GLOW_MODE.setOptionListValue(current.cycle(true));
            return true;
        });
        Configs.ENTITY_GLOW_RULE_EDITOR.setAction(0, () -> {
            MinecraftClientCompat.setScreen(Minecraft.getInstance(),
                    new EntityGlowRulesScreen(MinecraftClientCompat.getScreen(Minecraft.getInstance())));
            return true;
        });
        Configs.ENTITY_GLOW_RULES.setValueChangeCallback(config -> {
            EntityGlowService.reload();
            BlockGlowService.getInstance().reload();
        });
        Configs.BLOCK_GLOW_RULE_EDITOR.setAction(0, () -> {
            MinecraftClientCompat.setScreen(Minecraft.getInstance(),
                    new EntityGlowRulesScreen(MinecraftClientCompat.getScreen(Minecraft.getInstance())));
            return true;
        });
        Configs.BLOCK_GLOW_RULES.setValueChangeCallback(config -> {
            EntityGlowService.reload();
            BlockGlowService.getInstance().reload();
        });
        BlockGlowService.getInstance().reload();
        //#endif
        //#if MC >= 1.21.1
        Configs.KEEP_CONFIG_SEARCH_POSITION.setValueChangeCallback(config -> {
            if (!Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue()) ConfigScrollMemory.clearConfigState();
        });
        Configs.KEEP_CONFIG_SELECTED_TAB.setValueChangeCallback(config -> {
            if (!Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue()) ConfigScrollMemory.clearSelectedTab();
        });
        //#endif
        Configs.VOID_TRADING_AUTO_OPEN_CANCEL.getKeybind().setCallback(
                (action, key) -> VoidTrading.cancelAutoOpen(Minecraft.getInstance()));
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
        //#if MC >= 1.21.1
        BooleanSupplier openKeymap = () -> {
            if (!Configs.KEYMAP_SETTINGS_GROUP.getBooleanValue()) return false;
            if (!KeymapBrowserScreen.isEditingBindings(MinecraftClientCompat.getScreen(Minecraft.getInstance())))
                GuiBase.openGui(new KeymapBrowserScreen());
            return true;
        };
        Configs.KEYMAP_SETTINGS_GROUP.getKeybind().setCallback((action, key) -> openKeymap.getAsBoolean());
        Configs.OPEN_KEYMAP_BROWSER.setAction(openKeymap);
        Configs.OPEN_KEYMAP_BROWSER.getKeybind().setCallback((action, key) -> openKeymap.getAsBoolean());
        //#else
        //$$ registerTrigger(Configs.OPEN_KEYBIND_EDITOR, () -> {
        //$$     GuiBase.openGui(new KeybindCustomizationScreen());
        //$$     return true;
        //$$ });
        //$$ registerTrigger(Configs.OPEN_KEYMAP_BROWSER, () -> {
        //$$     GuiBase.openGui(new KeymapBrowserScreen());
        //$$     return true;
        //$$ });
        //$$ registerTrigger(Configs.RELOAD_KEYBIND_DATA, () -> {
        //$$     KeybindCustomizationStore.getInstance().reload();
        //$$     KeybindPieManager.getInstance().invalidateCustomMappingSync();
        //$$     return true;
        //$$ });
        //#endif

        registerTrigger(Configs.OPEN_CUSTOM_CONFIG_GROUPS, () -> {
            GuiBase.openGui(new CustomConfigGroupScreen(MinecraftClientCompat.getScreen(Minecraft.getInstance())));
            return true;
        });
        registerTrigger(Configs.CLEAR_SERVER_ICON_CACHE, ServerIconCache::requestClear);
        registerTrigger(Configs.CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION, ItemManagerHistoryOverlay::cyclePosition);
        registerTrigger(Configs.CONFLUX_MAP_TEMPORARY_WAYPOINT, ConfluxMapTemporaryWaypoint::placeAtPlayer);
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.getInstance());
        Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.clear());
        //#endif
        TickHandler.getInstance().registerClientTickHandler(ConfluxMapTemporaryWaypoint.getInstance());
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.ConfluxMapApiCompat.getInstance());
        //#endif
        TickHandler.getInstance().registerClientTickHandler(
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance());
        Configs.LITEMATICA_AUTO_REFILL.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.LITEMATICA_REFILL_SILENT.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.LITEMATICA_REFILL_ALLOW_FAKE.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.LITEMATICA_REFILL_AMOUNT.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.PRINTER_AUTO_REFILL.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.PRINTER_REFILL_ALLOW_FAKE.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.PRINTER_REFILL_SILENT.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());
        Configs.PRINTER_REFILL_AMOUNT.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill.getInstance().onToggle());

        Configs.CONTINGAME_IME.getKeybind().setCallback((action, key) -> ImeService.getInstance().onModeHotkey());
        //#if MC >= 26.1
        Configs.CONTINGAME_IME.setComment("halfmasa.config.ported.comment.contingameIme.mc261");
        //#endif
        Configs.KEEP_MOD_MENU_SCROLL.setValueChangeCallback(config -> {
            if (!Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue()) ConfigScrollMemory.clear();
            //#if MC >= 1.21.1
            if (!Configs.KEEP_SCREEN_POSITION.getBooleanValue())
                io.github.halfmasa.xaerobinding.feature.CreativeScreenMemory.clear();
            //#endif
        });
        //#if MC >= 1.21.1
        Configs.KEEP_CONFIG_POSITION_EXPANDED.setComment("halfmasa.config.ported.comment.keepConfigPositionExpanded.screen");
        Configs.KEEP_CREATIVE_SCREEN_POSITION.setValueChangeCallback(config -> {
            if (!Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue())
                io.github.halfmasa.xaerobinding.feature.CreativeScreenMemory.clear();
        });
        //#endif
        //#if MC >= 1.21.1
        Configs.CJK_LATIN_SPACING.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ConfigRenderingRefresh.refreshTranslations());
        Configs.CJK_LATIN_SPACING_TRANSLATIONS.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ConfigRenderingRefresh.refreshTranslations());
        //#else
        //$$ Configs.CJK_LATIN_SPACING.setValueChangeCallback(config -> {
        //$$     if (Configs.isConfigLoaded()) Minecraft.getInstance().reloadResourcePacks();
        //$$ });
        //$$ Configs.CJK_LATIN_SPACING_TRANSLATIONS.setValueChangeCallback(
        //$$         config -> {
        //$$             if (Configs.isConfigLoaded()) Minecraft.getInstance().reloadResourcePacks();
        //$$         });
        //#endif
        Configs.ENABLE_WORLD_BINDING.setValueChangeCallback(config -> WorldBindingStore.ensureCurrentWorldBinding());
        Configs.DISABLE_FLUID_RENDERING.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ConfigRenderingRefresh.refreshFluids());
        Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.setValueChangeCallback(config ->
                io.github.halfmasa.xaerobinding.feature.ConfigRenderingRefresh.refreshFluids());
        Configs.ENTITY_RENDER_AGGREGATION.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        //#if MC >= 1.21.1
        Configs.ITEM_RENDER_AGGREGATION.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ENTITY_AGGREGATION_SEPARATE_SIZES.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ITEM_AGGREGATION_RADIUS.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_THRESHOLD.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_SCAN_INTERVAL.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_LABEL_POSITION.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_LIST_MODE.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_WHITELIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ITEM_AGGREGATION_BLACKLIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearItems());
        Configs.ENTITY_RENDER_AGGREGATION.setComment("halfmasa.config.disabled.comment.entityRenderAggregation.mc263");
        Configs.ENTITY_AGGREGATION_THRESHOLD.setComment("halfmasa.config.disabled.comment.entityAggregationThreshold.mc263");
        //#endif
        Configs.ENTITY_AGGREGATION_RADIUS.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_THRESHOLD.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_LABEL_POSITION.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_LIST_MODE.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_WHITELIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
        Configs.ENTITY_AGGREGATION_BLACKLIST.setValueChangeCallback(config -> EntityRenderAggregation.getInstance().clearEntities());
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
        //#if MC >= 1.21.1
        GlowRuleHotkeys.reload();
        //#endif
        TickHandler.getInstance().registerClientTickHandler(KeybindPieManager.getInstance());
        TickHandler.getInstance().registerClientTickHandler(ImeService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(CooldownAutoAttack.getInstance());
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.AutoEatService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.RangeFeedingService.getInstance());
        //#endif
        TickHandler.getInstance().registerClientTickHandler(BridgingAssist.getInstance());
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(BlockGlowService.getInstance());
        //#endif
        TickHandler.getInstance().registerClientTickHandler(FastLoadingController.getInstance());
        TickHandler.getInstance().registerClientTickHandler(EntityRenderAggregation.getInstance());
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.ItemDespawnTimerService.getInstance());
        //#endif
        //#if MC >= 1.21.1
        TickHandler.getInstance().registerClientTickHandler(io.github.halfmasa.xaerobinding.feature.SpawnerPickCapture.getInstance());
        //#endif
        TickHandler.getInstance().registerClientTickHandler(ItemSearchHistoryService.getInstance());
        TickHandler.getInstance().registerClientTickHandler(VoidTrading.getInstance());
        TickHandler.getInstance().registerClientTickHandler(WorldBindingStore.getInstance());
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (var hotkey : Configs.HOTKEYS)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
        //#if MC >= 1.21.1
        for (var hotkey : GlowRuleHotkeys.getHotkeys())
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
        //#endif
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(XaeroWorldBinding.MOD_NAME, "halfmasa.hotkeys.category", Configs.HOTKEYS);
        //#if MC >= 1.21.1
        manager.addHotkeysForCategory(XaeroWorldBinding.MOD_NAME, "halfmasa.hotkeys.glow_rules", GlowRuleHotkeys.getHotkeys());
        //#endif
    }

    private static void registerTrigger(ActionHotkey config, BooleanSupplier action)
    {
        config.setAction(action);
        config.getKeybind().setCallback((keyAction, keybind) -> {
            config.trigger();
            return true;
        });
    }

}
