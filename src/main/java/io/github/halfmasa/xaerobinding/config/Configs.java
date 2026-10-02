package io.github.halfmasa.xaerobinding.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.FileUtils;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.util.data.json.JsonUtils;
//#else
//$$ import fi.dy.masa.malilib.util.JsonUtils;
//#endif

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;

public final class Configs implements IConfigHandler
{
    private static final String CONFIG_DIRECTORY_NAME = "halfmasa";
    private static final String CONFIG_FILE_NAME = "halfmasa.json";
    private static final int CONFIG_VERSION = 38;
    private static final String GENERIC_KEY = "halfmasa.config.generic";
    private static volatile boolean configLoaded;

    public static final ConfigHotkey OPEN_TOOLS = new ConfigHotkey(
            "openWaypointTools",
            "X,H").apply(GENERIC_KEY);

    public static final List<IConfigBase> GENERIC = List.of(OPEN_TOOLS);

    private static final String WAYPOINT_KEY = "halfmasa.config.waypoint";
    public static final ConfigBooleanHotkeyed ENABLE_WORLD_BINDING = new ConfigBooleanHotkeyed(
            "enableWorldBinding",
            false,
            "").apply(WAYPOINT_KEY);
    public static final ActionConfig IMPORT_WAYPOINT_BUNDLE = new ActionConfig(
            "importWaypointBundle", "halfmasa.gui.import").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionConfig EXPORT_ALL_DIMENSIONS = new ActionConfig(
            "exportAllDimensions", "halfmasa.gui.export_text", "halfmasa.gui.export_file").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionConfig EXPORT_CURRENT_DIMENSION = new ActionConfig(
            "exportCurrentDimension", "halfmasa.gui.export_text", "halfmasa.gui.export_file").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionConfig EXPORT_CURRENT_SET = new ActionConfig(
            "exportCurrentWaypointSet", "halfmasa.gui.export_text", "halfmasa.gui.export_file").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionConfig DEDUPE_WAYPOINTS = new ActionConfig(
            "dedupeWaypoints", "halfmasa.gui.merge_current", "halfmasa.gui.merge_all").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionConfig WAYPOINT_HISTORY = new ActionConfig(
            "waypointHistory", "halfmasa.gui.undo", "halfmasa.gui.redo").applyTranslationKey(WAYPOINT_KEY);
    public static final ConfigBoolean WAYPOINT_SHARING_EXPANDED = new ConfigBoolean(
            "waypointSharingExpanded", false).apply(WAYPOINT_KEY);
    public static final ConfigGroupHeader WAYPOINT_SHARING_GROUP = new ConfigGroupHeader(
            "waypointSharingGroup", "halfmasa.config.waypoint", WAYPOINT_SHARING_EXPANDED);
    public static final List<IConfigBase> WAYPOINT = List.of(
            ENABLE_WORLD_BINDING,
            IMPORT_WAYPOINT_BUNDLE,
            EXPORT_ALL_DIMENSIONS,
            EXPORT_CURRENT_DIMENSION,
            EXPORT_CURRENT_SET,
            DEDUPE_WAYPOINTS,
            WAYPOINT_HISTORY,
            WAYPOINT_SHARING_EXPANDED);
    private static final List<IConfigBase> WAYPOINT_PERSISTED = List.of(
            ENABLE_WORLD_BINDING,
            WAYPOINT_SHARING_EXPANDED);

    private static final String CREATIVE_KEY = "halfmasa.config.creative";
    public static final ConfigBooleanHotkeyed ENABLE_GIVE_FULL_INVENTORY = new ConfigBooleanHotkeyed(
            "enableGiveFullInventory", false, "").apply(CREATIVE_KEY);
    public static final ActionHotkey GIVE_FULL_INVENTORY = new ActionHotkey(
            "giveFullInventory", "G").applyTranslationKey(CREATIVE_KEY);
    public static final ConfigInteger BUNDLE_FILL = new ConfigInteger(
            "bundleFill", 1, 1, 100, true).apply(CREATIVE_KEY);
    public static final ConfigBooleanHotkeyed FILL_SAFETY = new ConfigBooleanHotkeyed(
            "fillSafety", true, "").apply(CREATIVE_KEY);
    public static final ConfigBoolean GIVE_FULL_INVENTORY_EXPANDED = new ConfigBoolean(
            "giveFullInventoryExpanded", false).apply(CREATIVE_KEY);
    public static final ConfigBooleanHotkeyed ITEM_SEARCH_HISTORY = new ConfigBooleanHotkeyed(
            "itemSearchHistory", false, "").apply(CREATIVE_KEY);
    public static final ConfigBoolean ITEM_SEARCH_HISTORY_EXPANDED = new ConfigBoolean(
            "itemSearchHistoryExpanded", false).apply(CREATIVE_KEY);
    public static final ConfigInteger ITEM_SEARCH_HISTORY_ROWS = new ConfigInteger(
            "itemSearchHistoryRows", 3, 1, 9, true).apply(CREATIVE_KEY);
    public static final ConfigBoolean ITEM_SEARCH_HISTORY_DURING_SEARCH = new ConfigBoolean(
            "itemSearchHistoryDuringSearch", false).apply(CREATIVE_KEY);
    private static final ConfigBoolean LEGACY_ITEM_MANAGER_SEARCH_HISTORY = new ConfigBoolean(
            "itemManagerSearchHistory", false).apply(CREATIVE_KEY);
    private static final List<IConfigBase> ITEM_SEARCH_HISTORY_CHILDREN = List.of(
            ITEM_SEARCH_HISTORY_ROWS,
            ITEM_SEARCH_HISTORY_DURING_SEARCH);
    public static final List<IConfigBase> CREATIVE = List.of(
            ENABLE_GIVE_FULL_INVENTORY,
            GIVE_FULL_INVENTORY,
            BUNDLE_FILL,
            FILL_SAFETY,
            GIVE_FULL_INVENTORY_EXPANDED,
            ITEM_SEARCH_HISTORY,
            ITEM_SEARCH_HISTORY_ROWS,
            ITEM_SEARCH_HISTORY_DURING_SEARCH,
            ITEM_SEARCH_HISTORY_EXPANDED);

    private static final String PORTED_KEY = "halfmasa.config.ported";
    private static final KeybindSettings ANY_SCREEN_HOTKEY = KeybindSettings.create(
            KeybindSettings.Context.ANY, KeyAction.PRESS, false, true, false, true);
    public static final ConfigString CUSTOM_SAVES_PATH = new ConfigString(
            "customSavesPath",
            "").apply(PORTED_KEY);
    public static final ConfigStringList CUSTOM_SAVES_PATHS = new ConfigStringList(
            "customSavesPaths", ImmutableList.of()).apply(PORTED_KEY);
    public static final ConfigBoolean KEEP_WORLD_SELECTION_ON_EMPTY = new ConfigBoolean(
            "keepWorldSelectionOnEmpty", false).apply(PORTED_KEY);
    /** Internal state used by the world-selection saves path switcher. */
    public static final ConfigString CUSTOM_SAVES_ACTIVE_PATH = new ConfigString(
            "activeCustomSavesPath", "").apply(PORTED_KEY);
    public static final ConfigInteger CUSTOM_SAVES_BUTTON_X = new ConfigInteger(
            "customSavesButtonX", -1, -1, 10000, true).apply(PORTED_KEY);
    public static final ConfigInteger CUSTOM_SAVES_BUTTON_Y = new ConfigInteger(
            "customSavesButtonY", -1, -1, 10000, true).apply(PORTED_KEY);
    private static final List<IConfigBase> CUSTOM_SAVES_INTERNAL = List.of(
            CUSTOM_SAVES_ACTIVE_PATH,
            CUSTOM_SAVES_BUTTON_X,
            CUSTOM_SAVES_BUTTON_Y);
    public static final ConfigBoolean CUSTOM_SAVES_PATHS_EXPANDED = new ConfigBoolean(
            "customSavesPathsExpanded", false).apply(PORTED_KEY);
    public static final ConfigGroupHeader CUSTOM_SAVES_PATHS_GROUP = new ConfigGroupHeader(
            "customSavesPathsGroup", PORTED_KEY, CUSTOM_SAVES_PATHS_EXPANDED);
    public static final ConfigBooleanHotkeyed SCREENSHOT_TO_CLIPBOARD = new ConfigBooleanHotkeyed(
            "screenshotToClipboard", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK = new ConfigBooleanHotkeyed(
            "skipResourcePackCompatibilityCheck", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed ELYTRA_TIME_TOOLTIP = new ConfigBooleanHotkeyed(
            "elytraTimeTooltip", false, "").apply(PORTED_KEY);
    public static final ActionHotkey REPORT_ELYTRA_TIME = new ActionHotkey(
            "reportElytraTime", "").applyTranslationKey(PORTED_KEY);
    public static final ConfigBoolean NIGHT_VISION_FADE = new ConfigBoolean(
            "nightVisionFade", true).apply(PORTED_KEY);
    public static final ConfigInteger NIGHT_VISION_FADE_SECONDS = new ConfigInteger(
            "nightVisionFadeSeconds", 5, 0, 60, true).apply(PORTED_KEY);
    public static final ConfigBoolean NIGHT_VISION_FADE_EXPANDED = new ConfigBoolean(
            "nightVisionFadeExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed BOAT_VIEW_360 = new ConfigBooleanHotkeyed(
            "boatView360", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed BOAT_ITEM_VIEW = new ConfigBooleanHotkeyed(
            "boatItemView", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING = new ConfigBooleanHotkeyed(
            "voidTrading", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_EXPANDED = new ConfigBooleanHotkeyed(
            "voidTradingExpanded", false, "").apply(PORTED_KEY);
    public static final ConfigString VOID_TRADING_FAKE_PLAYER_NAMES = new ConfigString(
            "voidTradingFakePlayerNames", "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_AUTO_DETECT_FAKE_PLAYERS = new ConfigBooleanHotkeyed(
            "voidTradingAutoDetectFakePlayers", false, "").apply(PORTED_KEY);
    public static final ConfigOptionList VOID_TRADING_RECOVERY_MODE = new ConfigOptionList(
            "voidTradingRecoveryMode", VoidTradeRecoveryMode.REJOIN).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_AUTO_TRADE = new ConfigBooleanHotkeyed(
            "voidTradingAutoTrade", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_AUTO_OPEN = new ConfigBooleanHotkeyed(
            "voidTradingAutoOpen", false, "").apply(PORTED_KEY);
    public static final ConfigHotkey VOID_TRADING_AUTO_OPEN_CANCEL = new ConfigHotkey(
            "voidTradingAutoOpenCancel", "ESC", KeybindSettings.GUI).apply(PORTED_KEY);
    public static final ConfigString VOID_TRADING_TRADE_INDICES = new ConfigString(
            "voidTradingTradeIndices", "1").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_TRADE_SPECIFIED_ITEMS = new ConfigBooleanHotkeyed(
            "voidTradingTradeSpecifiedItems", false, "").apply(PORTED_KEY);
    public static final ConfigStringList VOID_TRADING_TRADE_ITEMS = new ConfigStringList(
            "voidTradingTradeItems", ImmutableList.of()).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_AUTO_CLOSE = new ConfigBooleanHotkeyed(
            "voidTradingAutoClose", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_DROP_TRADE_ITEMS = new ConfigBooleanHotkeyed(
            "voidTradingDropTradeItems", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_AUTO_UNCRAFT_EMERALD_BLOCKS = new ConfigBooleanHotkeyed(
            "voidTradingAutoUncraftEmeraldBlocks", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed VOID_TRADING_QUICK_SHULKER = new ConfigBooleanHotkeyed(
            "voidTradingQuickShulker", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed LITEMATICA_AUTO_REFILL = new ConfigBooleanHotkeyed(
            "litematicaAutoRefill", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed LITEMATICA_REFILL_SILENT = new ConfigBooleanHotkeyed(
            "litematicaRefillSilent", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean LITEMATICA_REFILL_EXPANDED = new ConfigBoolean(
            "litematicaRefillExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed LITEMATICA_REFILL_ALLOW_FAKE = new ConfigBooleanHotkeyed(
            "litematicaRefillAllowFake", true, "").apply(PORTED_KEY);
    public static final ConfigInteger LITEMATICA_REFILL_AMOUNT = new ConfigInteger(
            "litematicaRefillAmount", 32, 0, 2304, true).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed PRINTER_AUTO_REFILL = new ConfigBooleanHotkeyed(
            "printerAutoRefill", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean PRINTER_REFILL_EXPANDED = new ConfigBoolean(
            "printerRefillExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed PRINTER_REFILL_ALLOW_FAKE = new ConfigBooleanHotkeyed(
            "printerRefillAllowFake", true, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed PRINTER_REFILL_SILENT = new ConfigBooleanHotkeyed(
            "printerRefillSilent", false, "").apply(PORTED_KEY);
    public static final ConfigInteger PRINTER_REFILL_AMOUNT = new ConfigInteger(
            "printerRefillAmount", 32, 0, 2304, true).apply(PORTED_KEY);
    public static final List<IConfigBase> LITEMATICA_REFILL_CHILDREN = List.of(
            LITEMATICA_REFILL_ALLOW_FAKE, LITEMATICA_REFILL_AMOUNT, LITEMATICA_REFILL_SILENT);
    public static final List<IConfigBase> PRINTER_REFILL_CHILDREN = List.of(
            PRINTER_REFILL_ALLOW_FAKE, PRINTER_REFILL_AMOUNT, PRINTER_REFILL_SILENT);
    public static final List<IConfigBase> REFILL_EXTENSION_CONFIGS = Stream.of(
            List.of(LITEMATICA_AUTO_REFILL, LITEMATICA_REFILL_EXPANDED), LITEMATICA_REFILL_CHILDREN,
            List.of(PRINTER_AUTO_REFILL, PRINTER_REFILL_EXPANDED), PRINTER_REFILL_CHILDREN)
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config)).toList();
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_EXTENSIONS = new ConfigBooleanHotkeyed(
            "confluxMapExtensions", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_EXTENSIONS_EXPANDED = new ConfigBooleanHotkeyed(
            "confluxMapExtensionsExpanded", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_ALL_DIMENSIONS = new ConfigBooleanHotkeyed(
            "confluxMapAllDimensions", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_PUBLIC_WAYPOINTS = new ConfigBooleanHotkeyed(
            "confluxMapPublicWaypoints", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_SHOW_BOTH_WAYPOINTS = new ConfigBooleanHotkeyed(
            "confluxMapShowBothWaypoints", false, "").apply(PORTED_KEY);
    public static final ConfigOptionList CONFLUX_MAP_WAYPOINT_LIST_LAYOUT = new ConfigOptionList(
            "confluxMapWaypointListLayout", ConfluxMapWaypointListLayout.SIDE_BY_SIDE).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CONFLUX_MAP_CLOSE_AFTER_TELEPORT = new ConfigBooleanHotkeyed(
            "confluxMapCloseAfterTeleport", true, "").apply(PORTED_KEY);
    public static final ConfigInteger CONFLUX_MAP_UNKNOWN_HEIGHT = new ConfigInteger(
            "confluxMapUnknownHeight", 128, -2048, 4096, true).apply(PORTED_KEY);
    public static final ActionHotkey CONFLUX_MAP_TEMPORARY_WAYPOINT = new ActionHotkey(
            "confluxMapTemporaryWaypoint", "").applyTranslationKey(PORTED_KEY);

    private static final ConfigString LEGACY_VOID_TRADING_FAKE_PLAYER_PREFIX = new ConfigString(
            "voidTradingFakePlayerPrefix", "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed INVENTORY_MOVE = new ConfigBooleanHotkeyed(
            "inventoryMove", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed FAST_WORLD_LOADING_SCREEN = new ConfigBooleanHotkeyed(
            "fastWorldLoadingScreen", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed FAST_RESOURCE_PACK_LOADING_SCREEN = new ConfigBooleanHotkeyed(
            "fastResourcePackLoadingScreen", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed BETTER_SAVED_HOTBARS = new ConfigBooleanHotkeyed(
            "betterSavedHotbars", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed COOLDOWN_AUTO_ATTACK = new ConfigBooleanHotkeyed(
            "cooldownAutoAttack", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed DRAGGABLE_LISTS = new ConfigBooleanHotkeyed(
            "draggableLists", false, "").apply(PORTED_KEY);
    public static final ConfigOptionList DRAG_RESOURCE_MODE = new ConfigOptionList(
            "draggableResourcePackMode", DragMode.DISABLED).apply(PORTED_KEY);
    public static final ConfigOptionList DRAG_SERVER_MODE = new ConfigOptionList(
            "draggableServerMode", DragMode.DISABLED).apply(PORTED_KEY);
    public static final ConfigBoolean DRAG_HIDE_RESOURCE_ARROWS = new ConfigBoolean(
            "draggableHideResourcePackArrows", true).apply(PORTED_KEY);
    public static final ConfigBoolean DRAG_HIDE_SERVER_ARROWS = new ConfigBoolean(
            "draggableHideServerArrows", true).apply(PORTED_KEY);
    public static final ConfigBoolean DRAGGABLE_LISTS_EXPANDED = new ConfigBoolean(
            "draggableListsExpanded", false).apply(PORTED_KEY);
    public static final ConfigBoolean FAST_SCROLLING = new ConfigBoolean(
            "fastScrolling", false).apply(PORTED_KEY);
    public static final ConfigBoolean FAST_SCROLLING_PRIMARY_ENABLED = new ConfigBoolean(
            "fastScrollingPrimaryEnabled", true).apply(PORTED_KEY);
    public static final ConfigHotkey FAST_SCROLLING_PRIMARY_HOTKEY = new ConfigHotkey(
            "fastScrollingPrimaryHotkey", "LEFT_CONTROL", KeybindSettings.MODIFIER_GUI).apply(PORTED_KEY);
    public static final ConfigInteger FAST_SCROLLING_PRIMARY_MULTIPLIER = new ConfigInteger(
            "fastScrollingPrimaryMultiplier", 2, 1, 32, true).apply(PORTED_KEY);
    public static final ConfigBoolean FAST_SCROLLING_SECONDARY_ENABLED = new ConfigBoolean(
            "fastScrollingSecondaryEnabled", true).apply(PORTED_KEY);
    public static final ConfigHotkey FAST_SCROLLING_SECONDARY_HOTKEY = new ConfigHotkey(
            "fastScrollingSecondaryHotkey", "LEFT_CONTROL,LEFT_SHIFT", KeybindSettings.MODIFIER_GUI).apply(PORTED_KEY);
    public static final ConfigInteger FAST_SCROLLING_SECONDARY_MULTIPLIER = new ConfigInteger(
            "fastScrollingSecondaryMultiplier", 6, 1, 32, true).apply(PORTED_KEY);
    public static final ConfigBoolean FAST_SCROLLING_EXPANDED = new ConfigBoolean(
            "fastScrollingExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed BRIDGING_ASSIST = new ConfigBooleanHotkeyed(
            "bridgingAssist", false, "").apply(PORTED_KEY);
    public static final ConfigInteger BRIDGING_MINIMUM_DISTANCE = new ConfigInteger(
            "bridgingMinimumDistance", 20, 0, 100, true).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_ONLY_WHEN_CROUCHING = new ConfigBoolean(
            "bridgingOnlyWhenCrouching", false).apply(PORTED_KEY);
    public static final ConfigOptionList BRIDGING_AXES = new ConfigOptionList(
            "bridgingAxes", BridgingAxisMode.BOTH).apply(PORTED_KEY);
    public static final ConfigOptionList BRIDGING_CROUCHING_AXES = new ConfigOptionList(
            "bridgingCrouchingAxes", BridgingAxisOverride.SAME_AS_DEFAULT).apply(PORTED_KEY);
    public static final ConfigInteger BRIDGING_PLACEMENT_DELAY = new ConfigInteger(
            "bridgingPlacementDelay", 4, 0, 20, true).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_SHOW_CROSSHAIR = new ConfigBoolean(
            "bridgingShowCrosshair", true).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_SHOW_OUTLINE = new ConfigBoolean(
            "bridgingShowOutline", true).apply(PORTED_KEY);
    public static final ConfigColor BRIDGING_OUTLINE_COLOR = new ConfigColor(
            "bridgingOutlineColor", "#66000000").apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_SLAB_ASSIST = new ConfigBoolean(
            "bridgingSlabAssist", true).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_SKIP_TORCHES = new ConfigBoolean(
            "bridgingSkipTorches", true).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_REPLACE_NON_SOLID = new ConfigBoolean(
            "bridgingReplaceNonSolid", true).apply(PORTED_KEY);
    public static final ConfigOptionList BRIDGING_PERSPECTIVE = new ConfigOptionList(
            "bridgingPerspective", BridgingPerspectiveMode.AUTO).apply(PORTED_KEY);
    public static final ConfigDouble BRIDGING_SNAP_STRENGTH = new ConfigDouble(
            "bridgingSnapStrength", 1.0D, 0.0D, 1.0D, true).apply(PORTED_KEY);
    public static final ConfigOptionList BRIDGING_ADJACENCY = new ConfigOptionList(
            "bridgingAdjacency", BridgingAdjacencyMode.CORNERS).apply(PORTED_KEY);
    public static final ConfigBoolean BRIDGING_EXPANDED = new ConfigBoolean(
            "bridgingExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed KEYBIND_PIE_MENU = new ConfigBooleanHotkeyed(
            "keybindPieMenu", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean TWEAKEROO_COLLAPSIBLE_CONFIG = new ConfigBoolean(
            "tweakerooCollapsibleConfig", false).apply(PORTED_KEY);
    public static final ConfigBoolean CUSTOM_CONFIG_GROUPS = new ConfigBoolean(
            "customConfigGroups", false).apply(PORTED_KEY);
    public static final ActionHotkey OPEN_CUSTOM_CONFIG_GROUPS = new ActionHotkey(
            "openCustomConfigGroups", "").applyTranslationKey(PORTED_KEY);
    public static final ConfigBoolean CONFIG_GROUPING_EXPANDED = new ConfigBoolean(
            "configGroupingExpanded", false).apply(PORTED_KEY);
    public static final ConfigGroupHeader CONFIG_GROUPING_GROUP = new ConfigGroupHeader(
            "configGroupingGroup", PORTED_KEY, CONFIG_GROUPING_EXPANDED);
    public static final ConfigBoolean KEYBIND_WHEEL_EXPANDED = new ConfigBoolean(
            "keybindWheelExpanded", false).apply(PORTED_KEY);
    // Better Key Settings is available across the supported version matrix.
    public static final ConfigBoolean KEYMAP_DIRECT_REBIND = new ConfigBoolean(
            "keymapDirectRebind", false).apply(PORTED_KEY);
    public static final ConfigBoolean KEYMAP_RELEASE_CONFIRM = new ConfigBoolean(
            "keymapReleaseConfirm", false).apply(PORTED_KEY);
    public static final ConfigHotkey KEYMAP_CONFIRM_SETTING = new ConfigHotkey(
            "keymapConfirmSetting", "ENTER").apply(PORTED_KEY);
    public static final ConfigBoolean KEYMAP_SETTINGS_EXPANDED = new ConfigBoolean(
            "keymapSettingsExpanded", false).apply(PORTED_KEY);
    public static final ConfigGroupHeader KEYMAP_SETTINGS_GROUP = new ConfigGroupHeader(
            "keymapSettingsGroup", PORTED_KEY, KEYMAP_SETTINGS_EXPANDED);
    public static final ConfigInteger KEYBIND_REPEAT_COOLDOWN = new ConfigInteger(
            "keybindPieRepeatCooldown", 20, 0, 200, true).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_SELECTION_COOLDOWN = new ConfigInteger(
            "keybindPieSelectionCooldown", 10, 0, 100, true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_ATTACK_WORKAROUND = new ConfigBoolean(
            "keybindPieAttackWorkaround", true).apply(PORTED_KEY);
    public static final ConfigString KEYBIND_IGNORED_KEYS = new ConfigString(
            "keybindPieIgnoredKeys", "87,65,83,68,340").apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_INVERT_IGNORED_KEYS = new ConfigBoolean(
            "keybindPieInvertIgnoredKeys", false).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_CIRCLE_VERTICES = new ConfigInteger(
            "keybindPieCircleVertices", 60, 12, 360, true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_BLEND = new ConfigBoolean(
            "keybindPieBlend", true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_DARKEN_BACKGROUND = new ConfigBoolean(
            "keybindPieDarkenBackground", true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_BLUR_BACKGROUND = new ConfigBoolean(
            "keybindPieBlurBackground", true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_LABEL_SHADOW = new ConfigBoolean(
            "keybindPieLabelShadow", false).apply(PORTED_KEY);
    public static final ConfigDouble KEYBIND_EXPANSION = new ConfigDouble(
            "keybindPieExpansion", 1.15D, 1.0D, 2.0D, true).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_MARGIN = new ConfigInteger(
            "keybindPieMargin", 0, 0, 200, true).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_LABEL_INSET = new ConfigInteger(
            "keybindPieLabelInset", 6, 0, 50, true).apply(PORTED_KEY);
    public static final ConfigDouble KEYBIND_SCALE = new ConfigDouble(
            "keybindPieScale", 0.6D, 0.2D, 1.0D, true).apply(PORTED_KEY);
    public static final ConfigDouble KEYBIND_CANCEL_ZONE = new ConfigDouble(
            "keybindPieCancelZone", 0.25D, 0.0D, 0.9D, true).apply(PORTED_KEY);
    public static final ConfigColor KEYBIND_MENU_COLOR = new ConfigColor(
            "keybindPieMenuColor", "#404040").apply(PORTED_KEY);
    public static final ConfigColor KEYBIND_SELECTED_COLOR = new ConfigColor(
            "keybindPieSelectedColor", "#FFFFFF").apply(PORTED_KEY);
    public static final ConfigColor KEYBIND_HIGHLIGHT_COLOR = new ConfigColor(
            "keybindPieHighlightColor", "#EED202").apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_ALTERNATE_LIGHTEN = new ConfigInteger(
            "keybindPieAlternateLighten", 25, 0, 127, true).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_ALPHA = new ConfigInteger(
            "keybindPieAlpha", 144, 0, 255, true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_GRADATION = new ConfigBoolean(
            "keybindPieGradation", true).apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_ANIMATE = new ConfigBoolean(
            "keybindPieAnimate", true).apply(PORTED_KEY);
    public static final ActionHotkey OPEN_KEYBIND_EDITOR = new ActionHotkey(
            "openKeybindPieEditor", "").applyTranslationKey(PORTED_KEY);
    public static final ActionHotkey RELOAD_KEYBIND_DATA = new ActionHotkey(
            "reloadKeybindPieData", "").applyTranslationKey(PORTED_KEY);
    public static final ActionHotkey OPEN_KEYMAP_BROWSER = new ActionHotkey(
            "openKeymapBrowser", "").applyTranslationKey(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CLICK_AND_SEND = new ConfigBooleanHotkeyed(
            "clickAndSend", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed CJK_LATIN_SPACING = new ConfigBooleanHotkeyed(
            "cjkLatinSpacing", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean CJK_LATIN_SPACING_TRANSLATIONS = new ConfigBoolean(
            "cjkLatinSpacingTranslations", true).apply(PORTED_KEY);
    public static final ConfigBoolean CJK_LATIN_SPACING_SIGNS = new ConfigBoolean(
            "cjkLatinSpacingSigns", true).apply(PORTED_KEY);
    public static final ConfigBoolean CJK_LATIN_SPACING_BOOKS = new ConfigBoolean(
            "cjkLatinSpacingBooks", true).apply(PORTED_KEY);
    public static final ConfigBoolean CJK_LATIN_SPACING_EXPANDED = new ConfigBoolean(
            "cjkLatinSpacingExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed MAP_IN_SLOT = new ConfigBooleanHotkeyed(
            "mapInSlot", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_HOTBAR = new ConfigBoolean(
            "mapInHotbar", true).apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_INVENTORY = new ConfigBoolean(
            "mapInInventory", true).apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_SLOT_EXPANDED = new ConfigBoolean(
            "mapInSlotExpanded", false).apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed SERVER_ICON_CACHE = new ConfigBooleanHotkeyed(
            "serverIconCache", true, "").apply(PORTED_KEY);
    public static final ConfigOptionList SERVER_ICON_MATCH_MODE = new ConfigOptionList(
            "serverIconMatchMode", ServerIconMatchMode.NAME_AND_IP).apply(PORTED_KEY);
    public static final ConfigInteger SERVER_ICON_CACHE_LIMIT = new ConfigInteger(
            "serverIconCacheLimit", 256, 16, 2048, true).apply(PORTED_KEY);
    public static final ActionHotkey CLEAR_SERVER_ICON_CACHE = new ActionHotkey(
            "clearServerIconCache", "").applyTranslationKey(PORTED_KEY);
    public static final ConfigBooleanHotkeyed TOAST_KILLER = new ConfigBooleanHotkeyed(
            "toastKiller", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed SERVER_PINGER_FIX = new ConfigBooleanHotkeyed(
            "serverPingerFix", false, "").apply(PORTED_KEY);
    public static final ExpandableImeConfig CONTINGAME_IME = new ExpandableImeConfig(
            "contingameIme", false, "HOME", PORTED_KEY);
    public static final ConfigBoolean IME_SETTINGS_EXPANDED = new ConfigBoolean(
            "contingameImeSettingsExpanded", false).apply(PORTED_KEY);
    public static final ConfigBoolean IME_DISABLE_IN_COMMAND_MODE = new ConfigBoolean(
            "imeDisableInCommandMode", false).apply(PORTED_KEY);
    public static final ConfigBoolean IME_AUTO_REPLACE_SLASH = new ConfigBoolean(
            "imeAutoReplaceSlash", true).apply(PORTED_KEY);
    public static final ConfigStringList IME_SLASH_CHARACTERS = new ConfigStringList(
            "imeSlashCharacters", ImmutableList.of("、")).apply(PORTED_KEY);
    //#if MC >= 26.1
    public static final ConfigBoolean IME_STATUS_INDICATOR = new ConfigBoolean(
            "imeStatusIndicator", true).apply(PORTED_KEY);
    public static final ConfigOptionList IME_RENDER_STYLE = new ConfigOptionList(
            "imeRenderStyle", ImeStyle.VANILLA).apply(PORTED_KEY);
    public static final ConfigBoolean IME_HIDE_VANILLA_PREEDIT = new ConfigBoolean(
            "imeHideVanillaPreedit", false).apply(PORTED_KEY);
    //#endif
    public static final ConfigBooleanHotkeyed CONDENSED_CREATIVE = new ConfigBooleanHotkeyed(
            "condensedCreative", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed KEEP_MOD_MENU_SCROLL = new ConfigBooleanHotkeyed(
            "keepModMenuScroll", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed ITEM_MANAGER_RECIPE_HISTORY = new ConfigBooleanHotkeyed(
            "itemManagerRecipeHistory", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean ITEM_MANAGER_RECIPE_HISTORY_EXPANDED = new ConfigBoolean(
            "itemManagerRecipeHistoryExpanded", false).apply(PORTED_KEY);
    public static final ConfigInteger ITEM_MANAGER_RECIPE_HISTORY_ROWS = new ConfigInteger(
            "itemManagerRecipeHistoryRows", 3, 1, 9, true).apply(PORTED_KEY);
    public static final ConfigOptionList ITEM_MANAGER_RECIPE_HISTORY_POSITION = new ConfigOptionList(
            "itemManagerRecipeHistoryPosition", ItemManagerHistoryPosition.BOTTOM_RIGHT).apply(PORTED_KEY);
    public static final ActionHotkey CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION = new ActionHotkey(
            "cycleItemManagerRecipeHistoryPosition", "", ANY_SCREEN_HOTKEY).applyTranslationKey(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_EXPANDED = new ConfigBoolean(
            "condensedCreativeExpanded", false).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_ENCHANTED_BOOKS = new ConfigBoolean(
            "condensedCreativeEnchantedBooks", true).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_TIPPED_ARROWS = new ConfigBoolean(
            "condensedCreativeTippedArrows", true).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_POTIONS = new ConfigBoolean(
            "condensedCreativePotions", true).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_ROTATING_PREVIEW = new ConfigBoolean(
            "condensedCreativeRotatingPreview", true).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_BACKGROUND = new ConfigBoolean(
            "condensedCreativeBackground", true).apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_BORDER = new ConfigBoolean(
            "condensedCreativeBorder", true).apply(PORTED_KEY);
    public static final ConfigColor CONDENSED_CREATIVE_BORDER_COLOR = new ConfigColor(
            "condensedCreativeBorderColor", "#C03EABF7").apply(PORTED_KEY);
    public static final ConfigBoolean CONDENSED_CREATIVE_TOOLTIP = new ConfigBoolean(
            "condensedCreativeTooltip", true).apply(PORTED_KEY);
    private static final List<IConfigBase> CONDENSED_CREATIVE_CHILDREN = List.of(
            CONDENSED_CREATIVE_ENCHANTED_BOOKS,
            CONDENSED_CREATIVE_TIPPED_ARROWS,
            CONDENSED_CREATIVE_POTIONS,
            CONDENSED_CREATIVE_ROTATING_PREVIEW,
            CONDENSED_CREATIVE_BACKGROUND,
            CONDENSED_CREATIVE_BORDER,
            CONDENSED_CREATIVE_BORDER_COLOR,
            CONDENSED_CREATIVE_TOOLTIP);
    private static final List<IConfigBase> ITEM_MANAGER_RECIPE_HISTORY_CHILDREN = List.of(
            ITEM_MANAGER_RECIPE_HISTORY_ROWS,
            ITEM_MANAGER_RECIPE_HISTORY_POSITION,
            CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION);
    private static final List<IConfigBase> ITEM_MANAGER_RECIPE_HISTORY_CONFIGS = List.of(
            ITEM_MANAGER_RECIPE_HISTORY,
            ITEM_MANAGER_RECIPE_HISTORY_ROWS,
            ITEM_MANAGER_RECIPE_HISTORY_POSITION,
            CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION,
            ITEM_MANAGER_RECIPE_HISTORY_EXPANDED);

    private static final List<IConfigBase> KEYBIND_PIE_SETTINGS = List.of(
            KEYBIND_REPEAT_COOLDOWN,
            KEYBIND_SELECTION_COOLDOWN,
            KEYBIND_ATTACK_WORKAROUND,
            KEYBIND_IGNORED_KEYS,
            KEYBIND_INVERT_IGNORED_KEYS,
            KEYBIND_CIRCLE_VERTICES,
            KEYBIND_BLEND,
            KEYBIND_DARKEN_BACKGROUND,
            KEYBIND_BLUR_BACKGROUND,
            KEYBIND_LABEL_SHADOW,
            KEYBIND_EXPANSION,
            KEYBIND_MARGIN,
            KEYBIND_LABEL_INSET,
            KEYBIND_SCALE,
            KEYBIND_CANCEL_ZONE,
            KEYBIND_MENU_COLOR,
            KEYBIND_SELECTED_COLOR,
            KEYBIND_HIGHLIGHT_COLOR,
            KEYBIND_ALTERNATE_LIGHTEN,
            KEYBIND_ALPHA,
            KEYBIND_GRADATION,
            KEYBIND_ANIMATE);
    private static final List<IConfigBase> KEYMAP_SETTINGS_CHILDREN = List.of(
            OPEN_KEYMAP_BROWSER,
            KEYMAP_DIRECT_REBIND,
            KEYMAP_RELEASE_CONFIRM,
            KEYMAP_CONFIRM_SETTING,
            OPEN_KEYBIND_EDITOR,
            RELOAD_KEYBIND_DATA);
    private static final List<IConfigBase> CONFIG_GROUPING_CHILDREN = List.of(
            TWEAKEROO_COLLAPSIBLE_CONFIG,
            CUSTOM_CONFIG_GROUPS,
            OPEN_CUSTOM_CONFIG_GROUPS);
    private static final List<IConfigBase> IME_CHILDREN = List.of(
            IME_DISABLE_IN_COMMAND_MODE,
            IME_AUTO_REPLACE_SLASH,
            IME_SLASH_CHARACTERS
//#if MC >= 26.1
            , IME_STATUS_INDICATOR
            , IME_RENDER_STYLE
            , IME_HIDE_VANILLA_PREEDIT
//#endif
            );
    private static final List<IConfigBase> FAST_SCROLLING_CHILDREN = List.of(
            FAST_SCROLLING_PRIMARY_ENABLED,
            FAST_SCROLLING_PRIMARY_HOTKEY,
            FAST_SCROLLING_PRIMARY_MULTIPLIER,
            FAST_SCROLLING_SECONDARY_ENABLED,
            FAST_SCROLLING_SECONDARY_HOTKEY,
            FAST_SCROLLING_SECONDARY_MULTIPLIER);
    private static final List<IConfigBase> CJK_LATIN_SPACING_CHILDREN = List.of(
            CJK_LATIN_SPACING_TRANSLATIONS,
            CJK_LATIN_SPACING_SIGNS,
            CJK_LATIN_SPACING_BOOKS);
    private static final List<IConfigBase> BRIDGING_CHILDREN = List.of(
            BRIDGING_MINIMUM_DISTANCE,
            BRIDGING_ONLY_WHEN_CROUCHING,
            BRIDGING_AXES,
            BRIDGING_CROUCHING_AXES,
            BRIDGING_PLACEMENT_DELAY,
            BRIDGING_SHOW_CROSSHAIR,
            BRIDGING_SHOW_OUTLINE,
            BRIDGING_OUTLINE_COLOR,
            BRIDGING_SLAB_ASSIST,
            BRIDGING_SKIP_TORCHES,
            BRIDGING_REPLACE_NON_SOLID,
            BRIDGING_PERSPECTIVE,
            BRIDGING_SNAP_STRENGTH,
            BRIDGING_ADJACENCY);
    private static final List<IConfigBase> PLANT_CENTERING_CHILDREN = PlantCenteringConfigs.CHILDREN;
    private static final List<IConfigBase> SAVES_RELATED_CONFIGS = List.of(
            ENABLE_WORLD_BINDING,
            CUSTOM_SAVES_PATHS,
            KEEP_WORLD_SELECTION_ON_EMPTY);
    private static final List<IConfigBase> RECORD_RELATED_CONFIGS = Stream.of(
            List.of(ITEM_SEARCH_HISTORY),
            ITEM_SEARCH_HISTORY_CHILDREN,
            List.of(ITEM_MANAGER_RECIPE_HISTORY),
            ITEM_MANAGER_RECIPE_HISTORY_CHILDREN,
            List.of(BETTER_SAVED_HOTBARS, KEEP_MOD_MENU_SCROLL))
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config))
            .toList();

    public static final List<IConfigBase> PORTED = Stream.of(List.of(
            CUSTOM_SAVES_PATHS,
            KEEP_WORLD_SELECTION_ON_EMPTY,
            SCREENSHOT_TO_CLIPBOARD,
            SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK,
            ELYTRA_TIME_TOOLTIP,
            REPORT_ELYTRA_TIME,
            NIGHT_VISION_FADE,
            NIGHT_VISION_FADE_SECONDS,
            NIGHT_VISION_FADE_EXPANDED,
            BOAT_VIEW_360,
            BOAT_ITEM_VIEW,
            VOID_TRADING,
            VOID_TRADING_EXPANDED,
            VOID_TRADING_FAKE_PLAYER_NAMES,
            VOID_TRADING_AUTO_DETECT_FAKE_PLAYERS,
            VOID_TRADING_RECOVERY_MODE,
            VOID_TRADING_AUTO_TRADE,
            VOID_TRADING_AUTO_OPEN,
            VOID_TRADING_AUTO_OPEN_CANCEL,
            VOID_TRADING_TRADE_INDICES,
            VOID_TRADING_TRADE_SPECIFIED_ITEMS,
            VOID_TRADING_TRADE_ITEMS,
            VOID_TRADING_AUTO_CLOSE,
            VOID_TRADING_DROP_TRADE_ITEMS,
            VOID_TRADING_AUTO_UNCRAFT_EMERALD_BLOCKS,
            VOID_TRADING_QUICK_SHULKER,
            CONFLUX_MAP_EXTENSIONS,
            CONFLUX_MAP_EXTENSIONS_EXPANDED,
            CONFLUX_MAP_ALL_DIMENSIONS,
            CONFLUX_MAP_PUBLIC_WAYPOINTS,
            CONFLUX_MAP_SHOW_BOTH_WAYPOINTS,
            CONFLUX_MAP_WAYPOINT_LIST_LAYOUT,
            CONFLUX_MAP_CLOSE_AFTER_TELEPORT,
            CONFLUX_MAP_UNKNOWN_HEIGHT,
            CONFLUX_MAP_TEMPORARY_WAYPOINT,

            INVENTORY_MOVE,
            FAST_WORLD_LOADING_SCREEN,
            FAST_RESOURCE_PACK_LOADING_SCREEN,
            BETTER_SAVED_HOTBARS,
            COOLDOWN_AUTO_ATTACK,
            DRAGGABLE_LISTS,
            DRAG_RESOURCE_MODE,
            DRAG_SERVER_MODE,
            DRAG_HIDE_RESOURCE_ARROWS,
            DRAG_HIDE_SERVER_ARROWS,
            DRAGGABLE_LISTS_EXPANDED,
            FAST_SCROLLING,
            FAST_SCROLLING_EXPANDED),
            FAST_SCROLLING_CHILDREN,
            List.of(
            PlantCenteringConfigs.CENTER_PLANT_MODELS,
            PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED),
            PLANT_CENTERING_CHILDREN,
            List.of(
            BRIDGING_ASSIST,
            BRIDGING_EXPANDED),
            BRIDGING_CHILDREN,
            List.of(
            KEYBIND_PIE_MENU,
            KEYBIND_WHEEL_EXPANDED),
            KEYBIND_PIE_SETTINGS,
            List.of(
            CONFIG_GROUPING_GROUP,
            CONFIG_GROUPING_EXPANDED),
            CONFIG_GROUPING_CHILDREN,
            List.of(KEYMAP_SETTINGS_GROUP, KEYMAP_SETTINGS_EXPANDED),
            KEYMAP_SETTINGS_CHILDREN,

            List.of(
            CLICK_AND_SEND,
            CJK_LATIN_SPACING,
            CJK_LATIN_SPACING_EXPANDED),
            CJK_LATIN_SPACING_CHILDREN,
            List.of(
            MAP_IN_SLOT,
            MAP_IN_HOTBAR,
            MAP_IN_INVENTORY,
            MAP_IN_SLOT_EXPANDED,
            SERVER_ICON_CACHE,
            SERVER_ICON_MATCH_MODE,
            SERVER_ICON_CACHE_LIMIT,
            CLEAR_SERVER_ICON_CACHE,
            TOAST_KILLER,
            SERVER_PINGER_FIX,
            CONTINGAME_IME,
            IME_SETTINGS_EXPANDED,
            CONDENSED_CREATIVE,
            KEEP_MOD_MENU_SCROLL,
            CONDENSED_CREATIVE_EXPANDED,
            ITEM_SEARCH_HISTORY,
            ITEM_SEARCH_HISTORY_EXPANDED),
            IME_CHILDREN,
            CONDENSED_CREATIVE_CHILDREN,
            ITEM_SEARCH_HISTORY_CHILDREN,
            List.of(
            ENABLE_GIVE_FULL_INVENTORY,
            GIVE_FULL_INVENTORY,
            BUNDLE_FILL,
            FILL_SAFETY,
            GIVE_FULL_INVENTORY_EXPANDED))
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config))
            .toList();

    private static boolean customSavesActivePathPersisted;

    private static final String CLIENT_KEY = "halfmasa.config.client";
    public static final ConfigBooleanHotkeyed DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION = new ConfigBooleanHotkeyed(
            "disablePausedItemTrajectoryPrediction", false, "").apply(CLIENT_KEY);
    public static final List<IConfigBase> CLIENT = Stream.of(
            List.of(
                    DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION,
                    FAST_WORLD_LOADING_SCREEN,
                    FAST_RESOURCE_PACK_LOADING_SCREEN,
                    KEYBIND_PIE_MENU,
                    KEYBIND_WHEEL_EXPANDED),
            KEYBIND_PIE_SETTINGS,
            List.of(
                    CONFIG_GROUPING_GROUP,
                    CONFIG_GROUPING_EXPANDED),
            CONFIG_GROUPING_CHILDREN,
            List.of(KEYMAP_SETTINGS_GROUP, KEYMAP_SETTINGS_EXPANDED),
            KEYMAP_SETTINGS_CHILDREN,

            List.of(
                    CLICK_AND_SEND,
                    MAP_IN_SLOT,
                    MAP_IN_HOTBAR,
                    MAP_IN_INVENTORY,
                    MAP_IN_SLOT_EXPANDED,
                    SERVER_ICON_CACHE,
                    SERVER_ICON_MATCH_MODE,
                    SERVER_ICON_CACHE_LIMIT,
                    CLEAR_SERVER_ICON_CACHE,
                    TOAST_KILLER,
                    SERVER_PINGER_FIX,
                    CONTINGAME_IME,
                    IME_SETTINGS_EXPANDED),
            IME_CHILDREN)
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config))
            .toList();

    public static final List<IConfigBase> EXTENSIONS = Stream.of(ITEM_MANAGER_RECIPE_HISTORY_CONFIGS
            , REFILL_EXTENSION_CONFIGS

            ).flatMap(List::stream).toList();

    private static final String DISABLED_KEY = "halfmasa.config.disabled";
    public static final ConfigBooleanHotkeyed DISABLE_FLUID_RENDERING = new ConfigBooleanHotkeyed(
            "disableFluidRendering", false, "").apply(DISABLED_KEY);
    public static final ConfigBooleanHotkeyed DISABLE_NON_SOURCE_FLUID_RENDERING = new ConfigBooleanHotkeyed(
            "disableNonSourceFluidRendering", false, "").apply(DISABLED_KEY);
    public static final ConfigBooleanHotkeyed ENTITY_RENDER_AGGREGATION = new ConfigBooleanHotkeyed(
            "entityRenderAggregation", false, "").apply(DISABLED_KEY);
//#if MC >= 26.3
    public static final ConfigBooleanHotkeyed ITEM_RENDER_AGGREGATION = new ConfigBooleanHotkeyed(
            "itemRenderAggregation", true, "").apply(DISABLED_KEY);
//#endif
    public static final ConfigBooleanHotkeyed ENTITY_AGGREGATION_COUNT_ONLY = new ConfigBooleanHotkeyed(
            "entityAggregationCountOnly", false, "").apply(DISABLED_KEY);
    public static final ConfigBoolean ENTITY_RENDER_AGGREGATION_EXPANDED = new ConfigBoolean(
            "entityRenderAggregationExpanded", false).apply(DISABLED_KEY);
    public static final ConfigDouble ENTITY_AGGREGATION_RADIUS = new ConfigDouble(
            "entityAggregationRadius", 1.0D, 0.25D, 64.0D, true).apply(DISABLED_KEY);
    public static final ConfigInteger ENTITY_AGGREGATION_THRESHOLD = new ConfigInteger(
            "entityAggregationThreshold", 10, 1, 10000, true).apply(DISABLED_KEY);
    public static final ConfigInteger ENTITY_AGGREGATION_SCAN_INTERVAL = new ConfigInteger(
            "entityAggregationScanInterval", 10, 1, 100, true).apply(DISABLED_KEY);
    public static final ConfigOptionList ENTITY_AGGREGATION_LABEL_POSITION = new ConfigOptionList(
            "entityAggregationLabelPosition", EntityLabelPosition.TOP).apply(DISABLED_KEY);
    public static final ConfigOptionList ENTITY_AGGREGATION_LIST_MODE = new ConfigOptionList(
            "entityAggregationListMode", EntityAggregationListMode.NONE).apply(DISABLED_KEY);
    public static final ConfigStringList ENTITY_AGGREGATION_WHITELIST = new ConfigStringList(
            "entityAggregationWhitelist", ImmutableList.of()).apply(DISABLED_KEY);
    public static final ConfigStringList ENTITY_AGGREGATION_BLACKLIST = new ConfigStringList(
            "entityAggregationBlacklist", ImmutableList.of()).apply(DISABLED_KEY);
    // Kept only to read configurations written before version 14
    private static final ConfigStringList LEGACY_ENTITY_AGGREGATION_ENTITY_LIST = new ConfigStringList(
            "entityAggregationEntityList", ImmutableList.of());
    public static final List<IConfigBase> DISABLED = List.of(
            DISABLE_FLUID_RENDERING,
            DISABLE_NON_SOURCE_FLUID_RENDERING,
            ENTITY_RENDER_AGGREGATION,
//#if MC >= 26.3
            ITEM_RENDER_AGGREGATION,
//#endif
            ENTITY_AGGREGATION_COUNT_ONLY,
            ENTITY_AGGREGATION_RADIUS,
            ENTITY_AGGREGATION_THRESHOLD,
            ENTITY_AGGREGATION_SCAN_INTERVAL,
            ENTITY_AGGREGATION_LABEL_POSITION,
            ENTITY_AGGREGATION_LIST_MODE,
            ENTITY_AGGREGATION_WHITELIST,
            ENTITY_AGGREGATION_BLACKLIST,
            ENTITY_RENDER_AGGREGATION_EXPANDED);

    public static final List<IConfigBase> RECOMMENDED = Stream.of(List.of(
            ELYTRA_TIME_TOOLTIP,
            REPORT_ELYTRA_TIME,
            NIGHT_VISION_FADE,
            NIGHT_VISION_FADE_SECONDS,
            NIGHT_VISION_FADE_EXPANDED,
            BOAT_VIEW_360,
            BOAT_ITEM_VIEW,
            CONFIG_GROUPING_GROUP,
            CONFIG_GROUPING_EXPANDED),
            CONFIG_GROUPING_CHILDREN,
            List.of(
            KEYBIND_PIE_MENU,
            KEYBIND_WHEEL_EXPANDED),
            KEYBIND_PIE_SETTINGS,
            List.of(KEYMAP_SETTINGS_GROUP, KEYMAP_SETTINGS_EXPANDED),
            KEYMAP_SETTINGS_CHILDREN,

            List.of(
            CLICK_AND_SEND,
            MAP_IN_SLOT,
            MAP_IN_HOTBAR,
            MAP_IN_INVENTORY,
            MAP_IN_SLOT_EXPANDED,
            SERVER_ICON_CACHE,
            SERVER_ICON_MATCH_MODE,
            SERVER_ICON_CACHE_LIMIT,
            CLEAR_SERVER_ICON_CACHE,
            TOAST_KILLER,
            SERVER_PINGER_FIX,
            CONTINGAME_IME,
            IME_SETTINGS_EXPANDED),
            IME_CHILDREN,
            List.of(
            DRAGGABLE_LISTS,
            DRAG_RESOURCE_MODE,
            DRAG_SERVER_MODE,
            DRAG_HIDE_RESOURCE_ARROWS,
            DRAG_HIDE_SERVER_ARROWS,
            DRAGGABLE_LISTS_EXPANDED,
            FAST_SCROLLING,
            FAST_SCROLLING_EXPANDED),
            FAST_SCROLLING_CHILDREN,
            List.of(
            SCREENSHOT_TO_CLIPBOARD,
            SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK))
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config))
            .toList();

    public static final List<IConfigBase> ALL = Stream.of(GENERIC, WAYPOINT, CREATIVE, PORTED, CLIENT, EXTENSIONS, DISABLED)
            .flatMap(List::stream)
            .distinct()
            .toList();

    public static final List<IHotkey> HOTKEYS = Stream.concat(
            Stream.of(
                    OPEN_TOOLS,
                    GIVE_FULL_INVENTORY,
                    REPORT_ELYTRA_TIME,
                    ENABLE_WORLD_BINDING,
                    ENABLE_GIVE_FULL_INVENTORY,
                    SCREENSHOT_TO_CLIPBOARD,
                    SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK,
                    ELYTRA_TIME_TOOLTIP,
                    BOAT_VIEW_360,
                    BOAT_ITEM_VIEW,
                    VOID_TRADING,
                    VOID_TRADING_EXPANDED,
                    VOID_TRADING_AUTO_DETECT_FAKE_PLAYERS,
                    VOID_TRADING_AUTO_TRADE,
                    VOID_TRADING_AUTO_OPEN,
                    VOID_TRADING_AUTO_OPEN_CANCEL,
                    VOID_TRADING_TRADE_SPECIFIED_ITEMS,
                    VOID_TRADING_AUTO_CLOSE,
                    VOID_TRADING_DROP_TRADE_ITEMS,
                    VOID_TRADING_AUTO_UNCRAFT_EMERALD_BLOCKS,
                    VOID_TRADING_QUICK_SHULKER,
                    LITEMATICA_AUTO_REFILL,
                    LITEMATICA_REFILL_SILENT,
                    LITEMATICA_REFILL_ALLOW_FAKE,
                    PRINTER_AUTO_REFILL,
                    PRINTER_REFILL_ALLOW_FAKE,
                    PRINTER_REFILL_SILENT,
                    CONFLUX_MAP_EXTENSIONS,
                    CONFLUX_MAP_EXTENSIONS_EXPANDED,
                    CONFLUX_MAP_ALL_DIMENSIONS,
                    CONFLUX_MAP_PUBLIC_WAYPOINTS,
                    CONFLUX_MAP_SHOW_BOTH_WAYPOINTS,
                    CONFLUX_MAP_CLOSE_AFTER_TELEPORT,
                    CONFLUX_MAP_TEMPORARY_WAYPOINT,

                    INVENTORY_MOVE,
                    DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION,
                    DISABLE_FLUID_RENDERING,
                    DISABLE_NON_SOURCE_FLUID_RENDERING,
                    ENTITY_RENDER_AGGREGATION,
//#if MC >= 26.3
                    ITEM_RENDER_AGGREGATION,
//#endif
                    ENTITY_AGGREGATION_COUNT_ONLY,
                    FAST_WORLD_LOADING_SCREEN,
                    FAST_RESOURCE_PACK_LOADING_SCREEN,
                    BETTER_SAVED_HOTBARS,
                    ITEM_SEARCH_HISTORY,
                    ITEM_MANAGER_RECIPE_HISTORY,
                    CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION,
                    KEEP_MOD_MENU_SCROLL,
                    COOLDOWN_AUTO_ATTACK,
                    DRAGGABLE_LISTS,
                    BRIDGING_ASSIST,
                    FAST_SCROLLING_PRIMARY_HOTKEY,
                    FAST_SCROLLING_SECONDARY_HOTKEY,
                    KEYBIND_PIE_MENU,
                    OPEN_KEYBIND_EDITOR,
                    RELOAD_KEYBIND_DATA,
                    OPEN_KEYMAP_BROWSER,

                    OPEN_CUSTOM_CONFIG_GROUPS,
                    CLICK_AND_SEND,
                    CJK_LATIN_SPACING,
                    MAP_IN_SLOT,
                    SERVER_ICON_CACHE,
                    CLEAR_SERVER_ICON_CACHE,
                    TOAST_KILLER,
                    SERVER_PINGER_FIX,
                    CONTINGAME_IME,
                    CONDENSED_CREATIVE,
                    FILL_SAFETY),
            PlantCenteringConfigs.HOTKEYS.stream()).toList();

    @Override
    public void load()
    {
        configLoaded = false;
        try
        {
            loadFromFile();
        }
        finally
        {
            configLoaded = true;
        }
    }

    private void loadFromFile()
    {
        customSavesActivePathPersisted = false;
        migrateLegacyConfig();
        Path file = getHalfMasaDirectory().resolve(CONFIG_FILE_NAME);
        if (!Files.isReadable(file))
        {
            CustomSavesPath.applyLoadedSelection();
            return;
        }

        JsonElement element = parseJsonFile(file);
        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
            JsonObject ported = root.getAsJsonObject("Ported");
            customSavesActivePathPersisted = ported != null && ported.has(CUSTOM_SAVES_ACTIVE_PATH.getName());
            int configVersion = root.has("ConfigVersion") ? root.get("ConfigVersion").getAsInt() : 1;
            if (configVersion < 5)
            {
                ConfigUtils.readConfigBase(root, "Generic", List.of(
                        ENABLE_WORLD_BINDING,
                    CUSTOM_SAVES_PATH,
                        OPEN_TOOLS));
                ConfigUtils.readConfigBase(root, "Client", List.of(
                        SCREENSHOT_TO_CLIPBOARD,
                        SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK,
                        ELYTRA_TIME_TOOLTIP,
                        REPORT_ELYTRA_TIME,
                        DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION));
            }

            ConfigUtils.readConfigBase(root, "Generic", GENERIC);
            ConfigUtils.readConfigBase(root, "Waypoint", WAYPOINT_PERSISTED);
            ConfigUtils.readConfigBase(root, "Creative", CREATIVE);
            ConfigUtils.readConfigBase(root, "Ported", PORTED);
            ConfigUtils.readConfigBase(root, "Ported", ITEM_MANAGER_RECIPE_HISTORY_CONFIGS);
            ConfigUtils.readConfigBase(root, "Ported", CUSTOM_SAVES_INTERNAL);
            if (configVersion < 36)
            {
                ConfigUtils.readConfigBase(root, "Ported", List.of(LEGACY_VOID_TRADING_FAKE_PLAYER_PREFIX));
                if (VOID_TRADING_FAKE_PLAYER_NAMES.getStringValue().isBlank() &&
                        !LEGACY_VOID_TRADING_FAKE_PLAYER_PREFIX.getStringValue().isBlank())
                {
                    VOID_TRADING_FAKE_PLAYER_NAMES.setValueFromString(
                            LEGACY_VOID_TRADING_FAKE_PLAYER_PREFIX.getStringValue().trim());
                }
            }
            ConfigUtils.readConfigBase(root, "Client", CLIENT);
            ConfigUtils.readConfigBase(root, "Ported", REFILL_EXTENSION_CONFIGS); // Preserve previous settings.

            ConfigUtils.readConfigBase(root, "Extensions", EXTENSIONS);
            ConfigUtils.readConfigBase(root, "Disabled", DISABLED);
            if (configVersion < 14)
            {
                ConfigUtils.readConfigBase(root, "Disabled", List.of(LEGACY_ENTITY_AGGREGATION_ENTITY_LIST));
                if (!LEGACY_ENTITY_AGGREGATION_ENTITY_LIST.getStrings().isEmpty())
                {
                    EntityAggregationListMode mode = (EntityAggregationListMode) ENTITY_AGGREGATION_LIST_MODE.getOptionListValue();
                    if (mode == EntityAggregationListMode.WHITELIST && ENTITY_AGGREGATION_WHITELIST.getStrings().isEmpty())
                    {
                        ENTITY_AGGREGATION_WHITELIST.setStrings(LEGACY_ENTITY_AGGREGATION_ENTITY_LIST.getStrings());
                    }
                    else if (mode == EntityAggregationListMode.BLACKLIST && ENTITY_AGGREGATION_BLACKLIST.getStrings().isEmpty())
                    {
                        ENTITY_AGGREGATION_BLACKLIST.setStrings(LEGACY_ENTITY_AGGREGATION_ENTITY_LIST.getStrings());
                    }
                }
            }

            if (configVersion < CONFIG_VERSION)
            {
                if (configVersion == 25)
                {
                    FAST_SCROLLING.setBooleanValue(false);
                    if (FAST_SCROLLING_PRIMARY_MULTIPLIER.getIntegerValue() == 3)
                    {
                        FAST_SCROLLING_PRIMARY_MULTIPLIER.setIntegerValue(2);
                    }
                    if (FAST_SCROLLING_SECONDARY_MULTIPLIER.getIntegerValue() == 8)
                    {
                        FAST_SCROLLING_SECONDARY_MULTIPLIER.setIntegerValue(6);
                    }
                }
                if (configVersion < 34)
                {
                    PlantCenteringConfigs.CENTER_PLANT_MODELS.setBooleanValue(false);
                    for (IConfigBase config : PlantCenteringConfigs.CHILDREN)
                    {
                        ((ConfigBooleanHotkeyed) config).setBooleanValue(true);
                    }
                }
                if (configVersion < 35)
                {
                    KEYBIND_IGNORED_KEYS.setValueFromString(migrateIgnoredKeyCodes(
                            KEYBIND_IGNORED_KEYS.getStringValue()));
                }
                if (configVersion < 21)
                {
                    KeybindSettings current = CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION.getKeybind().getSettings();
                    CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION.getKeybind().setSettings(KeybindSettings.create(
                            KeybindSettings.Context.ANY,
                            current.getActivateOn(),
                            current.getAllowExtraKeys(),
                            current.isOrderSensitive(),
                            current.isExclusive(),
                            current.shouldCancel(),
                            current.getAllowEmpty()));
                }
                if (configVersion < 20)
                {
                    ConfigUtils.readConfigBase(root, "Creative", List.of(LEGACY_ITEM_MANAGER_SEARCH_HISTORY));
                    ConfigUtils.readConfigBase(root, "Ported", List.of(LEGACY_ITEM_MANAGER_SEARCH_HISTORY));
                    if (LEGACY_ITEM_MANAGER_SEARCH_HISTORY.getBooleanValue())
                    {
                        ITEM_MANAGER_RECIPE_HISTORY.setBooleanValue(true);
                    }
                }
                if (configVersion < 4)
                {
                    disableNewFeaturesByDefault();
                }
                this.save();
                XaeroWorldBinding.LOGGER.info("Migrated halfmasa config to version {}", CONFIG_VERSION);
            }

            String configuredHotkey = OPEN_TOOLS.getStringValue().replace(" ", "");
            if (configuredHotkey.equalsIgnoreCase("LEFT_ALT,X"))
            {
                OPEN_TOOLS.setValueFromString("X,H");
            }
        }
        else
        {
            XaeroWorldBinding.LOGGER.error("Failed to parse config file {}", file.toAbsolutePath());
        }
        CustomSavesPath.applyLoadedSelection();
    }

    private static String migrateIgnoredKeyCodes(String value)
    {
        StringBuilder migrated = new StringBuilder();
        for (String token : value.split("[,;\\s]+"))
        {
            if (token.isBlank())
            {
                continue;
            }
            try
            {
                int code = InputCompat.layoutKeyCode(Integer.parseInt(token));
                if (migrated.length() > 0)
                {
                    migrated.append(',');
                }
                migrated.append(code);
            }
            catch (NumberFormatException ignored)
            {
                if (migrated.length() > 0)
                {
                    migrated.append(',');
                }
                migrated.append(token);
            }
        }
        return migrated.toString();
    }

    public static boolean isConfigLoaded()
    {
        return configLoaded;
    }

    @Override
    public void save()
    {
        Path directory = getHalfMasaDirectory();
        FileUtils.createDirectoriesIfMissing(directory);

        JsonObject root = new JsonObject();
        root.addProperty("ConfigVersion", CONFIG_VERSION);
        ConfigUtils.writeConfigBase(root, "Generic", GENERIC);
        ConfigUtils.writeConfigBase(root, "Waypoint", WAYPOINT_PERSISTED);
        ConfigUtils.writeConfigBase(root, "Creative", CREATIVE);
        ConfigUtils.writeConfigBase(root, "Ported", PORTED);
        ConfigUtils.writeConfigBase(root, "Ported", CUSTOM_SAVES_INTERNAL);
        ConfigUtils.writeConfigBase(root, "Client", CLIENT);
        ConfigUtils.writeConfigBase(root, "Extensions", EXTENSIONS);
        ConfigUtils.writeConfigBase(root, "Disabled", DISABLED);
        Path file = directory.resolve(CONFIG_FILE_NAME);
        if (!writeJsonToFile(root, file))
        {
            XaeroWorldBinding.LOGGER.error("Failed to write config file {}", file.toAbsolutePath());
        }
        customSavesActivePathPersisted = true;
    }

    public static boolean hasPersistedCustomSavesActivePath()
    {
        return customSavesActivePathPersisted;
    }

    private static Path getConfigDirectory()
    {
        //#if MC >= 1.21.11
        return FileUtils.getConfigDirectory();
        //#else
        //$$ return FileUtils.getConfigDirectoryAsPath();
        //#endif
    }

    public static Path getHalfMasaDirectory()
    {
        return getConfigDirectory().resolve(CONFIG_DIRECTORY_NAME);
    }

    public static List<IConfigBase> getPortedView()
    {
        return keepRelatedConfigsTogether(groupedPortedView());
    }

    public static List<IConfigBase> getClientView()
    {
        return keepRelatedConfigsTogether(visibleConfigs(CLIENT));
    }

    public static List<IConfigBase> getAllView()
    {
        List<IConfigBase> generic = CUSTOM_CONFIG_GROUPS.getBooleanValue()
                ? visibleConfigs(GENERIC) : GENERIC;
        List<IConfigBase> configs = Stream.of(generic, getWaypointView(), getCreativeView(), getPortedView(), getClientView(), getExtensionsView(), getDisabledView())
                .flatMap(List::stream)
                .distinct()
                .toList();
        return keepRelatedConfigsTogether(configs);
    }

    public static List<IConfigBase> getCustomGroupCandidates()
    {
        return ALL.stream()
                .filter(config -> !(config instanceof ConfigGroupHeader))
                .filter(config -> !config.getName().endsWith("Expanded"))
                .toList();
    }

    public static List<IConfigBase> getBuiltInExpansionParents()
    {
        java.util.LinkedHashSet<IConfigBase> parents = new java.util.LinkedHashSet<>();
        for (IConfigBase config : ALL)
        {
            IConfigBase parent = getExpansionParent(config);
            if (parent != null)
            {
                parents.add(parent);
            }
        }
        return List.copyOf(parents);
    }

    public static List<IConfigBase> getRecommendedView()
    {
        return keepRelatedConfigsTogether(visibleConfigs(RECOMMENDED));
    }

    public static List<IConfigBase> getExtensionsView()
    {
        return keepRelatedConfigsTogether(visibleConfigs(EXTENSIONS));
    }

    public static List<IConfigBase> getDisabledView()
    {
        return keepRelatedConfigsTogether(visibleConfigs(DISABLED));
    }

    public static List<IConfigBase> getWaypointView()
    {
        return keepRelatedConfigsTogether(
                groupedView(WAYPOINT, WAYPOINT_SHARING_GROUP, Configs::isWaypointSharingChild));
    }

    public static List<IConfigBase> getCreativeView()
    {
        return keepRelatedConfigsTogether(visibleConfigs(Stream.concat(
                CREATIVE.stream(),
                Stream.concat(
                        Stream.of(CONDENSED_CREATIVE, CONDENSED_CREATIVE_EXPANDED),
                        CONDENSED_CREATIVE_CHILDREN.stream()))
                .toList()));
    }

    private static List<IConfigBase> keepSavesRelatedConfigsTogether(List<IConfigBase> configs)
    {
        return keepConfigsTogether(configs, SAVES_RELATED_CONFIGS);
    }

    private static List<IConfigBase> keepRelatedConfigsTogether(List<IConfigBase> configs)
    {
        return keepConfigsTogether(keepSavesRelatedConfigsTogether(configs), RECORD_RELATED_CONFIGS);
    }

    private static List<IConfigBase> keepConfigsTogether(
            List<IConfigBase> configs, List<IConfigBase> relatedConfigs)
    {
        java.util.ArrayList<IConfigBase> result = new java.util.ArrayList<>();
        boolean inserted = false;
        for (IConfigBase config : configs)
        {
            if (relatedConfigs.contains(config))
            {
                if (!inserted)
                {
                    relatedConfigs.stream()
                            .filter(configs::contains)
                            .forEach(result::add);
                    inserted = true;
                }
            }
            else
            {
                result.add(config);
            }
        }
        return List.copyOf(result);
    }

    public static ConfigBoolean getExpansionConfig(IConfigBase config)
    {
        if (config == KEYBIND_PIE_MENU) return KEYBIND_WHEEL_EXPANDED;
        if (config == CONFIG_GROUPING_GROUP) return CONFIG_GROUPING_EXPANDED;
        if (config == KEYMAP_SETTINGS_GROUP) return KEYMAP_SETTINGS_EXPANDED;
        if (config == NIGHT_VISION_FADE) return NIGHT_VISION_FADE_EXPANDED;
        if (config == VOID_TRADING) return VOID_TRADING_EXPANDED;
        if (config == LITEMATICA_AUTO_REFILL) return LITEMATICA_REFILL_EXPANDED;
        if (config == PRINTER_AUTO_REFILL) return PRINTER_REFILL_EXPANDED;
        if (config == CONFLUX_MAP_EXTENSIONS) return CONFLUX_MAP_EXTENSIONS_EXPANDED;

        if (config == FAST_SCROLLING) return FAST_SCROLLING_EXPANDED;
        if (config == CJK_LATIN_SPACING) return CJK_LATIN_SPACING_EXPANDED;
        if (config == BRIDGING_ASSIST) return BRIDGING_EXPANDED;
        if (config == CONTINGAME_IME) return IME_SETTINGS_EXPANDED;
        if (config == ENABLE_GIVE_FULL_INVENTORY) return GIVE_FULL_INVENTORY_EXPANDED;
        if (config == WAYPOINT_SHARING_GROUP) return WAYPOINT_SHARING_EXPANDED;
        if (config == MAP_IN_SLOT) return MAP_IN_SLOT_EXPANDED;
        if (config == DRAGGABLE_LISTS) return DRAGGABLE_LISTS_EXPANDED;
        if (config == ENTITY_RENDER_AGGREGATION) return ENTITY_RENDER_AGGREGATION_EXPANDED;
        if (config == CONDENSED_CREATIVE) return CONDENSED_CREATIVE_EXPANDED;
        if (config == ITEM_SEARCH_HISTORY) return ITEM_SEARCH_HISTORY_EXPANDED;
        if (config == ITEM_MANAGER_RECIPE_HISTORY) return ITEM_MANAGER_RECIPE_HISTORY_EXPANDED;
        if (config == PlantCenteringConfigs.CENTER_PLANT_MODELS) return PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED;
        return null;
    }

    public static boolean isExpandedChild(IConfigBase config)
    {
        return KEYBIND_PIE_SETTINGS.contains(config) ||
                CONFIG_GROUPING_CHILDREN.contains(config) ||
                KEYMAP_SETTINGS_CHILDREN.contains(config) ||
                config == NIGHT_VISION_FADE_SECONDS ||
                isVoidTradingChild(config) ||
                LITEMATICA_REFILL_CHILDREN.contains(config) || PRINTER_REFILL_CHILDREN.contains(config) ||
                isConfluxMapExtensionChild(config) ||

                FAST_SCROLLING_CHILDREN.contains(config) ||
                CJK_LATIN_SPACING_CHILDREN.contains(config) ||
                BRIDGING_CHILDREN.contains(config) ||
                IME_CHILDREN.contains(config) ||
                isMapServerChild(config) ||
                config == GIVE_FULL_INVENTORY || config == BUNDLE_FILL || config == FILL_SAFETY ||
                isWaypointSharingChild(config) || isDraggableChild(config) ||
                isEntityAggregationChild(config) || CONDENSED_CREATIVE_CHILDREN.contains(config) ||
                ITEM_SEARCH_HISTORY_CHILDREN.contains(config) ||
                ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config) ||
                PLANT_CENTERING_CHILDREN.contains(config);
    }

    public static IConfigBase getExpansionParent(IConfigBase config)
    {
        if (KEYBIND_PIE_SETTINGS.contains(config)) return KEYBIND_PIE_MENU;
        if (CONFIG_GROUPING_CHILDREN.contains(config)) return CONFIG_GROUPING_GROUP;
        if (KEYMAP_SETTINGS_CHILDREN.contains(config)) return KEYMAP_SETTINGS_GROUP;
        if (config == NIGHT_VISION_FADE_SECONDS) return NIGHT_VISION_FADE;
        if (isVoidTradingChild(config)) return VOID_TRADING;
        if (LITEMATICA_REFILL_CHILDREN.contains(config)) return LITEMATICA_AUTO_REFILL;
        if (PRINTER_REFILL_CHILDREN.contains(config)) return PRINTER_AUTO_REFILL;
        if (isConfluxMapExtensionChild(config)) return CONFLUX_MAP_EXTENSIONS;

        if (FAST_SCROLLING_CHILDREN.contains(config)) return FAST_SCROLLING;
        if (CJK_LATIN_SPACING_CHILDREN.contains(config)) return CJK_LATIN_SPACING;
        if (BRIDGING_CHILDREN.contains(config)) return BRIDGING_ASSIST;
        if (IME_CHILDREN.contains(config)) return CONTINGAME_IME;
        if (isMapServerChild(config)) return MAP_IN_SLOT;
        if (config == GIVE_FULL_INVENTORY || config == BUNDLE_FILL || config == FILL_SAFETY) return ENABLE_GIVE_FULL_INVENTORY;
        if (isWaypointSharingChild(config)) return WAYPOINT_SHARING_GROUP;
        if (isDraggableChild(config)) return DRAGGABLE_LISTS;
        if (isEntityAggregationChild(config)) return ENTITY_RENDER_AGGREGATION;
        if (CONDENSED_CREATIVE_CHILDREN.contains(config)) return CONDENSED_CREATIVE;
        if (ITEM_SEARCH_HISTORY_CHILDREN.contains(config)) return ITEM_SEARCH_HISTORY;
        if (ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config)) return ITEM_MANAGER_RECIPE_HISTORY;
        if (PLANT_CENTERING_CHILDREN.contains(config)) return PlantCenteringConfigs.CENTER_PLANT_MODELS;
        return null;
    }

    public static List<IConfigBase> getExpansionChildren(IConfigBase config)
    {
        return ALL.stream()
                .filter(candidate -> getExpansionParent(candidate) == config)
                .toList();
    }

    private static List<IConfigBase> visibleConfigs(List<IConfigBase> configs)
    {
        if (CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return configs.stream()
                    .filter(config -> !(config instanceof ConfigGroupHeader))
                    .filter(config -> !config.getName().endsWith("Expanded"))
                    .toList();
        }
        return configs.stream()
                .filter(config -> config != IME_SETTINGS_EXPANDED &&
                        config != KEYBIND_WHEEL_EXPANDED &&
                        config != CONFIG_GROUPING_EXPANDED &&
                        config != KEYMAP_SETTINGS_EXPANDED &&
                        config != NIGHT_VISION_FADE_EXPANDED &&
                        config != VOID_TRADING_EXPANDED &&
                        config != FAST_SCROLLING_EXPANDED &&
                        config != CJK_LATIN_SPACING_EXPANDED &&
                        config != BRIDGING_EXPANDED &&
                        config != MAP_IN_SLOT_EXPANDED &&
                        config != DRAGGABLE_LISTS_EXPANDED &&
                        config != GIVE_FULL_INVENTORY_EXPANDED &&
                        config != WAYPOINT_SHARING_EXPANDED &&
                        config != CONDENSED_CREATIVE_EXPANDED &&
                        config != ITEM_SEARCH_HISTORY_EXPANDED &&
                        config != ITEM_MANAGER_RECIPE_HISTORY_EXPANDED &&
                        config != PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED)
                .filter(config -> config != CONFLUX_MAP_EXTENSIONS_EXPANDED && config != LITEMATICA_REFILL_EXPANDED && config != PRINTER_REFILL_EXPANDED)

                .filter(config -> !KEYBIND_PIE_SETTINGS.contains(config) || KEYBIND_WHEEL_EXPANDED.getBooleanValue())
                .filter(config -> !CONFIG_GROUPING_CHILDREN.contains(config) || CONFIG_GROUPING_EXPANDED.getBooleanValue())
                .filter(config -> !KEYMAP_SETTINGS_CHILDREN.contains(config) || KEYMAP_SETTINGS_EXPANDED.getBooleanValue())
                .filter(config -> config != NIGHT_VISION_FADE_SECONDS || NIGHT_VISION_FADE_EXPANDED.getBooleanValue())
                .filter(config -> !isVoidTradingChild(config) || VOID_TRADING_EXPANDED.getBooleanValue())
                .filter(config -> !isConfluxMapExtensionChild(config) || CONFLUX_MAP_EXTENSIONS_EXPANDED.getBooleanValue())
                .filter(config -> !LITEMATICA_REFILL_CHILDREN.contains(config) || LITEMATICA_REFILL_EXPANDED.getBooleanValue())
                .filter(config -> !PRINTER_REFILL_CHILDREN.contains(config) || PRINTER_REFILL_EXPANDED.getBooleanValue())

                .filter(config -> !FAST_SCROLLING_CHILDREN.contains(config) || FAST_SCROLLING_EXPANDED.getBooleanValue())
                .filter(config -> !CJK_LATIN_SPACING_CHILDREN.contains(config) || CJK_LATIN_SPACING_EXPANDED.getBooleanValue())
                .filter(config -> !BRIDGING_CHILDREN.contains(config) || BRIDGING_EXPANDED.getBooleanValue())
                .filter(config -> !IME_CHILDREN.contains(config) || IME_SETTINGS_EXPANDED.getBooleanValue())
                .filter(config -> !isMapServerChild(config) || MAP_IN_SLOT_EXPANDED.getBooleanValue())
                .filter(config -> (config != GIVE_FULL_INVENTORY && config != BUNDLE_FILL && config != FILL_SAFETY) || GIVE_FULL_INVENTORY_EXPANDED.getBooleanValue())
                .filter(config -> !isWaypointSharingChild(config) || WAYPOINT_SHARING_EXPANDED.getBooleanValue())
                .filter(config -> !isDraggableChild(config) || DRAGGABLE_LISTS_EXPANDED.getBooleanValue())
                .filter(config -> config != ENTITY_RENDER_AGGREGATION_EXPANDED)
                .filter(config -> !isEntityAggregationChild(config) || ENTITY_RENDER_AGGREGATION_EXPANDED.getBooleanValue())
                .filter(config -> !CONDENSED_CREATIVE_CHILDREN.contains(config) || CONDENSED_CREATIVE_EXPANDED.getBooleanValue())
                .filter(config -> !ITEM_SEARCH_HISTORY_CHILDREN.contains(config) || ITEM_SEARCH_HISTORY_EXPANDED.getBooleanValue())
                .filter(config -> !ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config) || ITEM_MANAGER_RECIPE_HISTORY_EXPANDED.getBooleanValue())
                .filter(config -> !PLANT_CENTERING_CHILDREN.contains(config) || PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED.getBooleanValue())
                .toList();
    }

    private static boolean isDraggableChild(IConfigBase config)
    {
        return config == DRAG_RESOURCE_MODE || config == DRAG_SERVER_MODE ||
                config == DRAG_HIDE_RESOURCE_ARROWS || config == DRAG_HIDE_SERVER_ARROWS;
    }

    private static boolean isEntityAggregationChild(IConfigBase config)
    {
        return config == ENTITY_AGGREGATION_COUNT_ONLY ||
//#if MC >= 26.3
                config == ITEM_RENDER_AGGREGATION ||
//#endif
                config == ENTITY_AGGREGATION_RADIUS || config == ENTITY_AGGREGATION_THRESHOLD ||
                config == ENTITY_AGGREGATION_SCAN_INTERVAL ||
                config == ENTITY_AGGREGATION_LABEL_POSITION || config == ENTITY_AGGREGATION_LIST_MODE ||
                config == ENTITY_AGGREGATION_WHITELIST || config == ENTITY_AGGREGATION_BLACKLIST;
    }

    private static boolean isVoidTradingChild(IConfigBase config)
    {
        return config == VOID_TRADING_FAKE_PLAYER_NAMES ||
                config == VOID_TRADING_AUTO_DETECT_FAKE_PLAYERS ||
                config == VOID_TRADING_RECOVERY_MODE ||
                config == VOID_TRADING_AUTO_TRADE ||
                config == VOID_TRADING_AUTO_OPEN ||
                config == VOID_TRADING_AUTO_OPEN_CANCEL ||
                config == VOID_TRADING_TRADE_INDICES ||
                config == VOID_TRADING_TRADE_SPECIFIED_ITEMS ||
                config == VOID_TRADING_TRADE_ITEMS ||
                config == VOID_TRADING_AUTO_CLOSE ||
                config == VOID_TRADING_DROP_TRADE_ITEMS ||
                config == VOID_TRADING_AUTO_UNCRAFT_EMERALD_BLOCKS ||
                config == VOID_TRADING_QUICK_SHULKER;
    }

    private static boolean isConfluxMapExtensionChild(IConfigBase config)
    {
        return config == CONFLUX_MAP_ALL_DIMENSIONS || config == CONFLUX_MAP_PUBLIC_WAYPOINTS ||
                config == CONFLUX_MAP_SHOW_BOTH_WAYPOINTS || config == CONFLUX_MAP_WAYPOINT_LIST_LAYOUT ||
                config == CONFLUX_MAP_CLOSE_AFTER_TELEPORT || config == CONFLUX_MAP_UNKNOWN_HEIGHT ||
                config == CONFLUX_MAP_TEMPORARY_WAYPOINT;
    }


    private static List<IConfigBase> groupedPortedView()
    {
        if (CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return visibleConfigs(PORTED);
        }
        java.util.ArrayList<IConfigBase> result = new java.util.ArrayList<>();
        for (IConfigBase config : PORTED)
        {
            if (config == CONFLUX_MAP_EXTENSIONS_EXPANDED)
            {
                continue;
            }

            if (config == MAP_IN_SLOT_EXPANDED || config == DRAGGABLE_LISTS_EXPANDED ||
                    config == NIGHT_VISION_FADE_EXPANDED ||
                    config == VOID_TRADING_EXPANDED ||
                    config == FAST_SCROLLING_EXPANDED ||
                    config == CJK_LATIN_SPACING_EXPANDED ||
                    config == BRIDGING_EXPANDED ||
                    config == KEYBIND_WHEEL_EXPANDED || config == IME_SETTINGS_EXPANDED ||
                    config == CONFIG_GROUPING_EXPANDED ||
                    config == KEYMAP_SETTINGS_EXPANDED ||
                    config == GIVE_FULL_INVENTORY_EXPANDED || config == CUSTOM_SAVES_PATHS_EXPANDED ||
                    config == CONDENSED_CREATIVE_EXPANDED || config == ITEM_SEARCH_HISTORY_EXPANDED ||
                    config == PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED)
            {
                continue;
            }
            if (config == ITEM_SEARCH_HISTORY)
            {
                result.add(config);
                if (ITEM_SEARCH_HISTORY_EXPANDED.getBooleanValue()) result.addAll(ITEM_SEARCH_HISTORY_CHILDREN);
                continue;
            }
            if (ITEM_SEARCH_HISTORY_CHILDREN.contains(config))
            {
                continue;
            }
            if (config == CONFIG_GROUPING_GROUP)
            {
                result.add(config);
                if (CONFIG_GROUPING_EXPANDED.getBooleanValue()) result.addAll(CONFIG_GROUPING_CHILDREN);
                continue;
            }
            if (CONFIG_GROUPING_CHILDREN.contains(config))
            {
                continue;
            }
            if (config == KEYMAP_SETTINGS_GROUP)
            {
                result.add(config);
                if (KEYMAP_SETTINGS_EXPANDED.getBooleanValue()) result.addAll(KEYMAP_SETTINGS_CHILDREN);
                continue;
            }
            if (KEYMAP_SETTINGS_CHILDREN.contains(config))
            {
                continue;
            }
            if (isMapServerChild(config))
            {
                if (MAP_IN_SLOT_EXPANDED.getBooleanValue()) result.add(config);
            }
            else if (isDraggableChild(config))
            {
                if (DRAGGABLE_LISTS_EXPANDED.getBooleanValue()) result.add(config);
            }
            else if (!isExpandedChild(config) || isGenericExpandedChildVisible(config))
            {
                result.add(config);
            }
        }
        return result;
    }

    private static boolean isMapServerChild(IConfigBase config)
    {
        return config == MAP_IN_HOTBAR || config == MAP_IN_INVENTORY ||
                config == SERVER_ICON_CACHE || config == SERVER_ICON_MATCH_MODE ||
                config == SERVER_ICON_CACHE_LIMIT || config == CLEAR_SERVER_ICON_CACHE;
    }

    private static boolean isWaypointSharingChild(IConfigBase config)
    {
        return config == IMPORT_WAYPOINT_BUNDLE || config == EXPORT_ALL_DIMENSIONS ||
                config == EXPORT_CURRENT_DIMENSION || config == EXPORT_CURRENT_SET ||
                config == DEDUPE_WAYPOINTS || config == WAYPOINT_HISTORY;
    }

    private static List<IConfigBase> groupedView(
            List<IConfigBase> configs,
            ConfigGroupHeader header,
            java.util.function.Predicate<IConfigBase> childPredicate)
    {
        if (CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return visibleConfigs(configs);
        }
        java.util.ArrayList<IConfigBase> result = new java.util.ArrayList<>();
        boolean inserted = false;
        for (IConfigBase config : configs)
        {
            if (config == IME_SETTINGS_EXPANDED || config == KEYBIND_WHEEL_EXPANDED ||
                    config == MAP_IN_SLOT_EXPANDED || config == WAYPOINT_SHARING_EXPANDED ||
                    config == GIVE_FULL_INVENTORY_EXPANDED)
            {
                continue;
            }
            if (childPredicate.test(config))
            {
                if (!inserted)
                {
                    result.add(header);
                    inserted = true;
                }
                if (header.getExpansion().getBooleanValue())
                {
                    result.add(config);
                }
            }
            else if (!isExpandedChild(config) || isGenericExpandedChildVisible(config))
            {
                result.add(config);
            }
        }
        return result;
    }

    private static boolean isGenericExpandedChildVisible(IConfigBase config)
    {
        if (config == NIGHT_VISION_FADE_SECONDS)
        {
            return NIGHT_VISION_FADE_EXPANDED.getBooleanValue();
        }
        if (isVoidTradingChild(config))
        {
            return VOID_TRADING_EXPANDED.getBooleanValue();
        }
        if (LITEMATICA_REFILL_CHILDREN.contains(config)) return LITEMATICA_REFILL_EXPANDED.getBooleanValue();
        if (PRINTER_REFILL_CHILDREN.contains(config)) return PRINTER_REFILL_EXPANDED.getBooleanValue();
        if (isConfluxMapExtensionChild(config))
        {
            return CONFLUX_MAP_EXTENSIONS_EXPANDED.getBooleanValue();
        }

        if (FAST_SCROLLING_CHILDREN.contains(config))
        {
            return FAST_SCROLLING_EXPANDED.getBooleanValue();
        }
        if (CJK_LATIN_SPACING_CHILDREN.contains(config))
        {
            return CJK_LATIN_SPACING_EXPANDED.getBooleanValue();
        }
        if (BRIDGING_CHILDREN.contains(config))
        {
            return BRIDGING_EXPANDED.getBooleanValue();
        }
        if (KEYBIND_PIE_SETTINGS.contains(config))
        {
            return KEYBIND_WHEEL_EXPANDED.getBooleanValue();
        }
        if (CONFIG_GROUPING_CHILDREN.contains(config))
        {
            return CONFIG_GROUPING_EXPANDED.getBooleanValue();
        }
        if (KEYMAP_SETTINGS_CHILDREN.contains(config))
        {
            return KEYMAP_SETTINGS_EXPANDED.getBooleanValue();
        }
        if (IME_CHILDREN.contains(config))
        {
            return IME_SETTINGS_EXPANDED.getBooleanValue();
        }
        if (config == GIVE_FULL_INVENTORY || config == BUNDLE_FILL || config == FILL_SAFETY)
        {
            return GIVE_FULL_INVENTORY_EXPANDED.getBooleanValue();
        }
        if (CONDENSED_CREATIVE_CHILDREN.contains(config))
        {
            return CONDENSED_CREATIVE_EXPANDED.getBooleanValue();
        }
        if (ITEM_SEARCH_HISTORY_CHILDREN.contains(config))
        {
            return ITEM_SEARCH_HISTORY_EXPANDED.getBooleanValue();
        }
        if (ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config))
        {
            return ITEM_MANAGER_RECIPE_HISTORY_EXPANDED.getBooleanValue();
        }
        if (PLANT_CENTERING_CHILDREN.contains(config))
        {
            return PlantCenteringConfigs.CENTER_PLANT_MODELS_EXPANDED.getBooleanValue();
        }
        return true;
    }

    private static void migrateLegacyConfig()
    {
        Path oldFile = getConfigDirectory().resolve(CONFIG_FILE_NAME);
        Path directory = getHalfMasaDirectory();
        Path newFile = directory.resolve(CONFIG_FILE_NAME);
        if (!Files.isReadable(oldFile))
        {
            return;
        }

        try
        {
            Files.createDirectories(directory);
            if (!Files.exists(newFile))
            {
                Files.copy(oldFile, newFile, StandardCopyOption.COPY_ATTRIBUTES);
            }

            Path legacy = directory.resolve("legacy");
            Files.createDirectories(legacy);
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            Files.move(oldFile, legacy.resolve("halfmasa-" + timestamp + ".json.bak"));
        }
        catch (Exception exception)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to migrate the legacy halfmasa config", exception);
        }
    }

    private static JsonElement parseJsonFile(Path file)
    {
        //#if MC >= 1.21.11
        return JsonUtils.parseJsonFile(file);
        //#else
        //$$ return JsonUtils.parseJsonFileAsPath(file);
        //#endif
    }

    private static boolean writeJsonToFile(JsonObject root, Path file)
    {
        //#if MC >= 1.21.11
        return JsonUtils.writeJsonToFile(root, file);
        //#else
        //$$ return JsonUtils.writeJsonToFileAsPath(root, file);
        //#endif
    }

    private static void disableNewFeaturesByDefault()
    {
        ENABLE_GIVE_FULL_INVENTORY.setBooleanValue(false);
        SCREENSHOT_TO_CLIPBOARD.setBooleanValue(false);
        SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK.setBooleanValue(false);
        ELYTRA_TIME_TOOLTIP.setBooleanValue(false);
        REPORT_ELYTRA_TIME.setValueFromString("");
        DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION.setBooleanValue(false);
        BOAT_VIEW_360.setBooleanValue(false);
        BOAT_ITEM_VIEW.setBooleanValue(false);
        INVENTORY_MOVE.setBooleanValue(false);
        DISABLE_FLUID_RENDERING.setBooleanValue(false);
        DISABLE_NON_SOURCE_FLUID_RENDERING.setBooleanValue(false);
        ENTITY_RENDER_AGGREGATION.setBooleanValue(false);
        FAST_WORLD_LOADING_SCREEN.setBooleanValue(false);
        FAST_RESOURCE_PACK_LOADING_SCREEN.setBooleanValue(false);
        BETTER_SAVED_HOTBARS.setBooleanValue(false);
        COOLDOWN_AUTO_ATTACK.setBooleanValue(false);
        DRAGGABLE_LISTS.setBooleanValue(false);
        BRIDGING_ASSIST.setBooleanValue(false);
        KEYBIND_PIE_MENU.setBooleanValue(false);
        TWEAKEROO_COLLAPSIBLE_CONFIG.setBooleanValue(false);
        CUSTOM_CONFIG_GROUPS.setBooleanValue(false);
        KEYMAP_DIRECT_REBIND.setBooleanValue(false);
        KEYMAP_RELEASE_CONFIRM.setBooleanValue(false);
        CLICK_AND_SEND.setBooleanValue(false);
        CJK_LATIN_SPACING.setBooleanValue(false);
        MAP_IN_SLOT.setBooleanValue(false);
        SERVER_ICON_CACHE.setBooleanValue(true);
        TOAST_KILLER.setBooleanValue(false);
        SERVER_PINGER_FIX.setBooleanValue(false);
        CONTINGAME_IME.setBooleanValue(false);
        CONDENSED_CREATIVE.setBooleanValue(false);
        KEEP_MOD_MENU_SCROLL.setBooleanValue(false);
        ITEM_SEARCH_HISTORY.setBooleanValue(false);
        ITEM_MANAGER_RECIPE_HISTORY.setBooleanValue(false);
    }
}
