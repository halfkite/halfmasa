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

public final class Configs implements IConfigHandler
{
    private static final String CONFIG_DIRECTORY_NAME = "halfmasa";
    private static final String CONFIG_FILE_NAME = "halfmasa.json";
    private static final int CONFIG_VERSION = 21;
    private static final String GENERIC_KEY = "halfmasa.config.generic";

    public static final ConfigHotkey OPEN_TOOLS = new ConfigHotkey(
            "openWaypointTools",
            "X,H").apply(GENERIC_KEY);

    public static final List<IConfigBase> GENERIC = List.of(OPEN_TOOLS);

    private static final String WAYPOINT_KEY = "halfmasa.config.waypoint";
    public static final ConfigBooleanHotkeyed ENABLE_WORLD_BINDING = new ConfigBooleanHotkeyed(
            "enableWorldBinding",
            false,
            "").apply(WAYPOINT_KEY);
    public static final ActionHotkey COPY_WAYPOINT_BUNDLE = new ActionHotkey("copyWaypointBundle", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionHotkey IMPORT_WAYPOINT_BUNDLE = new ActionHotkey("importWaypointBundle", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionHotkey SHARE_CURRENT_SET = new ActionHotkey("shareCurrentWaypointSet", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionHotkey SHARE_ALL_WAYPOINTS = new ActionHotkey("shareAllWaypoints", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionHotkey DEDUPE_CURRENT_SET = new ActionHotkey("dedupeCurrentWaypointSet", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ActionHotkey DEDUPE_ALL_WAYPOINTS = new ActionHotkey("dedupeAllWaypoints", "").applyTranslationKey(WAYPOINT_KEY);
    public static final ConfigInteger CHAT_SEND_INTERVAL = new ConfigInteger(
            "waypointChatInterval", 10, 1, 100, true).apply(WAYPOINT_KEY);
    public static final ConfigBoolean WAYPOINT_SHARING_EXPANDED = new ConfigBoolean(
            "waypointSharingExpanded", false).apply(WAYPOINT_KEY);
    public static final ConfigGroupHeader WAYPOINT_SHARING_GROUP = new ConfigGroupHeader(
            "waypointSharingGroup", "halfmasa.config.waypoint", WAYPOINT_SHARING_EXPANDED);
    public static final List<IConfigBase> WAYPOINT = List.of(
            ENABLE_WORLD_BINDING,
            COPY_WAYPOINT_BUNDLE,
            IMPORT_WAYPOINT_BUNDLE,
            SHARE_ALL_WAYPOINTS,
            SHARE_CURRENT_SET,
            CHAT_SEND_INTERVAL,
            DEDUPE_CURRENT_SET,
            DEDUPE_ALL_WAYPOINTS,
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
    public static final ConfigBooleanHotkeyed BOAT_VIEW_360 = new ConfigBooleanHotkeyed(
            "boatView360", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed BOAT_ITEM_VIEW = new ConfigBooleanHotkeyed(
            "boatItemView", false, "").apply(PORTED_KEY);
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
    public static final ConfigGroupHeader DRAGGABLE_LISTS_GROUP = new ConfigGroupHeader(
            "draggableListsGroup", PORTED_KEY, DRAGGABLE_LISTS_EXPANDED);
    public static final ConfigBooleanHotkeyed KEYBIND_PIE_MENU = new ConfigBooleanHotkeyed(
            "keybindPieMenu", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean KEYBIND_WHEEL_EXPANDED = new ConfigBoolean(
            "keybindWheelExpanded", false).apply(PORTED_KEY);
    public static final ConfigInteger KEYBIND_REPEAT_COOLDOWN = new ConfigInteger(
            "keybindPieRepeatCooldown", 20, 0, 200, true).apply(PORTED_KEY);
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
    public static final ConfigBooleanHotkeyed CLICK_AND_SEND = new ConfigBooleanHotkeyed(
            "clickAndSend", false, "").apply(PORTED_KEY);
    public static final ConfigBooleanHotkeyed MAP_IN_SLOT = new ConfigBooleanHotkeyed(
            "mapInSlot", false, "").apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_HOTBAR = new ConfigBoolean(
            "mapInHotbar", true).apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_INVENTORY = new ConfigBoolean(
            "mapInInventory", true).apply(PORTED_KEY);
    public static final ConfigBoolean MAP_IN_SLOT_EXPANDED = new ConfigBoolean(
            "mapInSlotExpanded", false).apply(PORTED_KEY);
    public static final ConfigGroupHeader MAP_SERVER_GROUP = new ConfigGroupHeader(
            "mapServerGroup", "halfmasa.config.map_server", MAP_IN_SLOT_EXPANDED);
    public static final ConfigBooleanHotkeyed SERVER_ICON_CACHE = new ConfigBooleanHotkeyed(
            "serverIconCache", false, "").apply(PORTED_KEY);
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

    private static final List<IConfigBase> KEYBIND_PIE_SETTINGS = List.of(
            KEYBIND_REPEAT_COOLDOWN,
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
            KEYBIND_ANIMATE,
            OPEN_KEYBIND_EDITOR,
            RELOAD_KEYBIND_DATA);
    private static final List<IConfigBase> IME_CHILDREN = List.of(
            IME_DISABLE_IN_COMMAND_MODE,
            IME_AUTO_REPLACE_SLASH,
            IME_SLASH_CHARACTERS);

    public static final List<IConfigBase> PORTED = Stream.of(List.of(
            CUSTOM_SAVES_PATHS,
            SCREENSHOT_TO_CLIPBOARD,
            SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK,
            ELYTRA_TIME_TOOLTIP,
            REPORT_ELYTRA_TIME,
            BOAT_VIEW_360,
            BOAT_ITEM_VIEW,
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
            KEYBIND_PIE_MENU,
            KEYBIND_WHEEL_EXPANDED),
            KEYBIND_PIE_SETTINGS,
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
            IME_SETTINGS_EXPANDED,
            CONDENSED_CREATIVE,
            KEEP_MOD_MENU_SCROLL,
            CONDENSED_CREATIVE_EXPANDED,
            ITEM_SEARCH_HISTORY,
            ITEM_SEARCH_HISTORY_EXPANDED,
            ITEM_MANAGER_RECIPE_HISTORY,
            ITEM_MANAGER_RECIPE_HISTORY_EXPANDED),
            IME_CHILDREN,
            CONDENSED_CREATIVE_CHILDREN,
            ITEM_SEARCH_HISTORY_CHILDREN,
            ITEM_MANAGER_RECIPE_HISTORY_CHILDREN,
            List.of(
            ENABLE_GIVE_FULL_INVENTORY,
            GIVE_FULL_INVENTORY,
            BUNDLE_FILL,
            FILL_SAFETY,
            GIVE_FULL_INVENTORY_EXPANDED))
            .flatMap(list -> list.stream().map(config -> (IConfigBase) config))
            .toList();

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

    private static final String EXTENSIONS_KEY = "halfmasa.config.extensions";
    public static final ConfigBooleanHotkeyed FREE_CAMERA_INTERACTION = new ConfigBooleanHotkeyed(
            "freeCameraInteraction", false, "").apply(EXTENSIONS_KEY);
    public static final ConfigBoolean FREE_CAMERA_INTERACTION_EXPANDED = new ConfigBoolean(
            "freeCameraInteractionExpanded", false).apply(EXTENSIONS_KEY);
    public static final ConfigBooleanHotkeyed FREE_CAMERA_CONTAINER_INTERACTION = new ConfigBooleanHotkeyed(
            "freeCameraContainerInteraction", false, "").apply(EXTENSIONS_KEY);
    public static final ConfigBooleanHotkeyed FREE_CAMERA_ITEM_USE = new ConfigBooleanHotkeyed(
            "freeCameraItemUse", false, "").apply(EXTENSIONS_KEY);
    public static final ConfigBooleanHotkeyed FREE_CAMERA_MINING = new ConfigBooleanHotkeyed(
            "freeCameraMining", false, "").apply(EXTENSIONS_KEY);
    public static final ConfigBooleanHotkeyed FREE_CAMERA_LITEMATICA_EASY_PLACE = new ConfigBooleanHotkeyed(
            "freeCameraLitematicaEasyPlace", false, "").apply(EXTENSIONS_KEY);
    public static final List<IConfigBase> EXTENSIONS = List.of(
            FREE_CAMERA_INTERACTION,
            FREE_CAMERA_CONTAINER_INTERACTION,
            FREE_CAMERA_ITEM_USE,
            FREE_CAMERA_MINING,
            FREE_CAMERA_LITEMATICA_EASY_PLACE,
            FREE_CAMERA_INTERACTION_EXPANDED);

    private static final String DISABLED_KEY = "halfmasa.config.disabled";
    public static final ConfigBooleanHotkeyed DISABLE_FLUID_RENDERING = new ConfigBooleanHotkeyed(
            "disableFluidRendering", false, "").apply(DISABLED_KEY);
    public static final ConfigBooleanHotkeyed DISABLE_NON_SOURCE_FLUID_RENDERING = new ConfigBooleanHotkeyed(
            "disableNonSourceFluidRendering", false, "").apply(DISABLED_KEY);
    public static final ConfigBooleanHotkeyed ENTITY_RENDER_AGGREGATION = new ConfigBooleanHotkeyed(
            "entityRenderAggregation", false, "").apply(DISABLED_KEY);
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
            ENTITY_AGGREGATION_COUNT_ONLY,
            ENTITY_AGGREGATION_RADIUS,
            ENTITY_AGGREGATION_THRESHOLD,
            ENTITY_AGGREGATION_SCAN_INTERVAL,
            ENTITY_AGGREGATION_LABEL_POSITION,
            ENTITY_AGGREGATION_LIST_MODE,
            ENTITY_AGGREGATION_WHITELIST,
            ENTITY_AGGREGATION_BLACKLIST,
            ENTITY_RENDER_AGGREGATION_EXPANDED);

    public static final List<IConfigBase> RECOMMENDED = List.of(
            ELYTRA_TIME_TOOLTIP,
            REPORT_ELYTRA_TIME,
            BOAT_VIEW_360,
            BOAT_ITEM_VIEW,
            KEYBIND_PIE_MENU,
            CLICK_AND_SEND,
            MAP_IN_SLOT,
            TOAST_KILLER,
            SERVER_PINGER_FIX,
            CONTINGAME_IME,
            DRAGGABLE_LISTS,
            SCREENSHOT_TO_CLIPBOARD,
            SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK);

    public static final List<IConfigBase> ALL = Stream.of(GENERIC, WAYPOINT, CREATIVE, PORTED, CLIENT, EXTENSIONS, DISABLED)
            .flatMap(List::stream)
            .distinct()
            .toList();

    public static final List<IHotkey> HOTKEYS = List.of(
            OPEN_TOOLS,
            COPY_WAYPOINT_BUNDLE,
            IMPORT_WAYPOINT_BUNDLE,
            SHARE_CURRENT_SET,
            SHARE_ALL_WAYPOINTS,
            DEDUPE_CURRENT_SET,
            DEDUPE_ALL_WAYPOINTS,
            GIVE_FULL_INVENTORY,
            REPORT_ELYTRA_TIME,
            ENABLE_WORLD_BINDING,
            ENABLE_GIVE_FULL_INVENTORY,
            SCREENSHOT_TO_CLIPBOARD,
            SKIP_RESOURCE_PACK_COMPATIBILITY_CHECK,
            ELYTRA_TIME_TOOLTIP,
            BOAT_VIEW_360,
            BOAT_ITEM_VIEW,
            INVENTORY_MOVE,
            DISABLE_PAUSED_ITEM_TRAJECTORY_PREDICTION,
            FREE_CAMERA_CONTAINER_INTERACTION,
            FREE_CAMERA_ITEM_USE,
            FREE_CAMERA_MINING,
            FREE_CAMERA_LITEMATICA_EASY_PLACE,
            FREE_CAMERA_INTERACTION,
            DISABLE_FLUID_RENDERING,
            DISABLE_NON_SOURCE_FLUID_RENDERING,
            ENTITY_RENDER_AGGREGATION,
            ENTITY_AGGREGATION_COUNT_ONLY,
            FAST_WORLD_LOADING_SCREEN,
            FAST_RESOURCE_PACK_LOADING_SCREEN,
            BETTER_SAVED_HOTBARS,
            COOLDOWN_AUTO_ATTACK,
            DRAGGABLE_LISTS,
            KEYBIND_PIE_MENU,
            OPEN_KEYBIND_EDITOR,
            RELOAD_KEYBIND_DATA,
            CLICK_AND_SEND,
            MAP_IN_SLOT,
            SERVER_ICON_CACHE,
            CLEAR_SERVER_ICON_CACHE,
            TOAST_KILLER,
            SERVER_PINGER_FIX,
            CONTINGAME_IME,
            CONDENSED_CREATIVE,
            KEEP_MOD_MENU_SCROLL,
            ITEM_SEARCH_HISTORY,
            ITEM_MANAGER_RECIPE_HISTORY,
            CYCLE_ITEM_MANAGER_RECIPE_HISTORY_POSITION,
            FILL_SAFETY);

    @Override
    public void load()
    {
        migrateLegacyConfig();
        Path file = getHalfMasaDirectory().resolve(CONFIG_FILE_NAME);
        if (!Files.isReadable(file))
        {
            return;
        }

        JsonElement element = parseJsonFile(file);
        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
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
            ConfigUtils.readConfigBase(root, "Waypoint", WAYPOINT);
            ConfigUtils.readConfigBase(root, "Creative", CREATIVE);
            ConfigUtils.readConfigBase(root, "Ported", PORTED);
            ConfigUtils.readConfigBase(root, "Client", CLIENT);
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
    }

    @Override
    public void save()
    {
        Path directory = getHalfMasaDirectory();
        FileUtils.createDirectoriesIfMissing(directory);

        JsonObject root = new JsonObject();
        root.addProperty("ConfigVersion", CONFIG_VERSION);
        ConfigUtils.writeConfigBase(root, "Generic", GENERIC);
        ConfigUtils.writeConfigBase(root, "Waypoint", WAYPOINT);
        ConfigUtils.writeConfigBase(root, "Creative", CREATIVE);
        ConfigUtils.writeConfigBase(root, "Ported", PORTED);
        ConfigUtils.writeConfigBase(root, "Client", CLIENT);
        ConfigUtils.writeConfigBase(root, "Extensions", EXTENSIONS);
        ConfigUtils.writeConfigBase(root, "Disabled", DISABLED);
        Path file = directory.resolve(CONFIG_FILE_NAME);
        if (!writeJsonToFile(root, file))
        {
            XaeroWorldBinding.LOGGER.error("Failed to write config file {}", file.toAbsolutePath());
        }
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
        return groupedPortedView();
    }

    public static List<IConfigBase> getClientView()
    {
        return groupedView(CLIENT, MAP_SERVER_GROUP, Configs::isMapServerChild);
    }

    public static List<IConfigBase> getAllView()
    {
        return Stream.of(GENERIC, getWaypointView(), getCreativeView(), getPortedView(), getClientView(), getExtensionsView(), getDisabledView())
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    public static List<IConfigBase> getRecommendedView()
    {
        return RECOMMENDED;
    }

    public static List<IConfigBase> getExtensionsView()
    {
        return visibleConfigs(EXTENSIONS);
    }

    public static List<IConfigBase> getDisabledView()
    {
        return visibleConfigs(DISABLED);
    }

    public static List<IConfigBase> getWaypointView()
    {
        return groupedView(WAYPOINT, WAYPOINT_SHARING_GROUP, Configs::isWaypointSharingChild);
    }

    public static List<IConfigBase> getCreativeView()
    {
        return visibleConfigs(Stream.concat(
                CREATIVE.stream(),
                Stream.concat(
                        Stream.of(CONDENSED_CREATIVE, CONDENSED_CREATIVE_EXPANDED),
                        CONDENSED_CREATIVE_CHILDREN.stream()))
                .toList());
    }

    public static ConfigBoolean getExpansionConfig(IConfigBase config)
    {
        if (config == KEYBIND_PIE_MENU) return KEYBIND_WHEEL_EXPANDED;
        if (config == CONTINGAME_IME) return IME_SETTINGS_EXPANDED;
        if (config == ENABLE_GIVE_FULL_INVENTORY) return GIVE_FULL_INVENTORY_EXPANDED;
        if (config == WAYPOINT_SHARING_GROUP) return WAYPOINT_SHARING_EXPANDED;
        if (config == MAP_SERVER_GROUP) return MAP_IN_SLOT_EXPANDED;
        if (config == DRAGGABLE_LISTS_GROUP) return DRAGGABLE_LISTS_EXPANDED;
        if (config == FREE_CAMERA_INTERACTION) return FREE_CAMERA_INTERACTION_EXPANDED;
        if (config == ENTITY_RENDER_AGGREGATION) return ENTITY_RENDER_AGGREGATION_EXPANDED;
        if (config == CONDENSED_CREATIVE) return CONDENSED_CREATIVE_EXPANDED;
        if (config == ITEM_SEARCH_HISTORY) return ITEM_SEARCH_HISTORY_EXPANDED;
        if (config == ITEM_MANAGER_RECIPE_HISTORY) return ITEM_MANAGER_RECIPE_HISTORY_EXPANDED;
        return null;
    }

    public static boolean isExpandedChild(IConfigBase config)
    {
        return KEYBIND_PIE_SETTINGS.contains(config) ||
                IME_CHILDREN.contains(config) ||
                isMapServerChild(config) ||
                config == GIVE_FULL_INVENTORY || config == BUNDLE_FILL || config == FILL_SAFETY ||
                isWaypointSharingChild(config) || isDraggableChild(config) || isFreeCameraChild(config) ||
                isEntityAggregationChild(config) || CONDENSED_CREATIVE_CHILDREN.contains(config) ||
                ITEM_SEARCH_HISTORY_CHILDREN.contains(config) ||
                ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config);
    }

    private static List<IConfigBase> visibleConfigs(List<IConfigBase> configs)
    {
        return configs.stream()
                .filter(config -> config != IME_SETTINGS_EXPANDED &&
                        config != KEYBIND_WHEEL_EXPANDED &&
                        config != MAP_IN_SLOT_EXPANDED &&
                        config != DRAGGABLE_LISTS_EXPANDED &&
                        config != GIVE_FULL_INVENTORY_EXPANDED &&
                        config != WAYPOINT_SHARING_EXPANDED &&
                        config != CONDENSED_CREATIVE_EXPANDED &&
                        config != ITEM_SEARCH_HISTORY_EXPANDED &&
                        config != ITEM_MANAGER_RECIPE_HISTORY_EXPANDED)
                .filter(config -> !KEYBIND_PIE_SETTINGS.contains(config) || KEYBIND_WHEEL_EXPANDED.getBooleanValue())
                .filter(config -> !IME_CHILDREN.contains(config) || IME_SETTINGS_EXPANDED.getBooleanValue())
                .filter(config -> !isMapServerChild(config) || MAP_IN_SLOT_EXPANDED.getBooleanValue())
                .filter(config -> (config != GIVE_FULL_INVENTORY && config != BUNDLE_FILL && config != FILL_SAFETY) || GIVE_FULL_INVENTORY_EXPANDED.getBooleanValue())
                .filter(config -> !isWaypointSharingChild(config) || WAYPOINT_SHARING_EXPANDED.getBooleanValue())
                .filter(config -> !isDraggableChild(config) || DRAGGABLE_LISTS_EXPANDED.getBooleanValue())
                .filter(config -> config != FREE_CAMERA_INTERACTION_EXPANDED)
                .filter(config -> !isFreeCameraChild(config) || FREE_CAMERA_INTERACTION_EXPANDED.getBooleanValue())
                .filter(config -> config != ENTITY_RENDER_AGGREGATION_EXPANDED)
                .filter(config -> !isEntityAggregationChild(config) || ENTITY_RENDER_AGGREGATION_EXPANDED.getBooleanValue())
                .filter(config -> !CONDENSED_CREATIVE_CHILDREN.contains(config) || CONDENSED_CREATIVE_EXPANDED.getBooleanValue())
                .filter(config -> !ITEM_SEARCH_HISTORY_CHILDREN.contains(config) || ITEM_SEARCH_HISTORY_EXPANDED.getBooleanValue())
                .filter(config -> !ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config) || ITEM_MANAGER_RECIPE_HISTORY_EXPANDED.getBooleanValue())
                .toList();
    }

    private static boolean isFreeCameraChild(IConfigBase config)
    {
        return config == FREE_CAMERA_CONTAINER_INTERACTION || config == FREE_CAMERA_ITEM_USE ||
                config == FREE_CAMERA_MINING || config == FREE_CAMERA_LITEMATICA_EASY_PLACE;
    }

    private static boolean isDraggableChild(IConfigBase config)
    {
        return config == DRAG_RESOURCE_MODE || config == DRAG_SERVER_MODE ||
                config == DRAG_HIDE_RESOURCE_ARROWS || config == DRAG_HIDE_SERVER_ARROWS;
    }

    private static boolean isEntityAggregationChild(IConfigBase config)
    {
        return config == ENTITY_AGGREGATION_COUNT_ONLY || config == ENTITY_AGGREGATION_RADIUS || config == ENTITY_AGGREGATION_THRESHOLD ||
                config == ENTITY_AGGREGATION_SCAN_INTERVAL ||
                config == ENTITY_AGGREGATION_LABEL_POSITION || config == ENTITY_AGGREGATION_LIST_MODE ||
                config == ENTITY_AGGREGATION_WHITELIST || config == ENTITY_AGGREGATION_BLACKLIST;
    }

    private static List<IConfigBase> groupedPortedView()
    {
        java.util.ArrayList<IConfigBase> result = new java.util.ArrayList<>();
        boolean mapInserted = false;
        boolean dragInserted = false;
        for (IConfigBase config : PORTED)
        {
            if (config == MAP_IN_SLOT_EXPANDED || config == DRAGGABLE_LISTS_EXPANDED ||
                    config == KEYBIND_WHEEL_EXPANDED || config == IME_SETTINGS_EXPANDED ||
                    config == GIVE_FULL_INVENTORY_EXPANDED || config == CUSTOM_SAVES_PATHS_EXPANDED ||
                    config == CONDENSED_CREATIVE_EXPANDED || config == ITEM_SEARCH_HISTORY_EXPANDED ||
                    config == ITEM_MANAGER_RECIPE_HISTORY_EXPANDED)
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
            if (config == ITEM_MANAGER_RECIPE_HISTORY)
            {
                result.add(config);
                if (ITEM_MANAGER_RECIPE_HISTORY_EXPANDED.getBooleanValue()) result.addAll(ITEM_MANAGER_RECIPE_HISTORY_CHILDREN);
                continue;
            }
            if (ITEM_MANAGER_RECIPE_HISTORY_CHILDREN.contains(config))
            {
                continue;
            }
            if (isMapServerChild(config))
            {
                if (!mapInserted) { result.add(MAP_SERVER_GROUP); mapInserted = true; }
                if (MAP_IN_SLOT_EXPANDED.getBooleanValue()) result.add(config);
            }
            else if (isDraggableChild(config))
            {
                if (!dragInserted) { result.add(DRAGGABLE_LISTS_GROUP); dragInserted = true; }
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
        return config == MAP_IN_SLOT || config == MAP_IN_HOTBAR || config == MAP_IN_INVENTORY ||
                config == SERVER_ICON_CACHE || config == SERVER_ICON_MATCH_MODE ||
                config == SERVER_ICON_CACHE_LIMIT || config == CLEAR_SERVER_ICON_CACHE;
    }

    private static boolean isWaypointSharingChild(IConfigBase config)
    {
        return config == COPY_WAYPOINT_BUNDLE || config == IMPORT_WAYPOINT_BUNDLE ||
                config == SHARE_ALL_WAYPOINTS || config == SHARE_CURRENT_SET ||
                config == CHAT_SEND_INTERVAL || config == DEDUPE_CURRENT_SET ||
                config == DEDUPE_ALL_WAYPOINTS;
    }

    private static List<IConfigBase> groupedView(
            List<IConfigBase> configs,
            ConfigGroupHeader header,
            java.util.function.Predicate<IConfigBase> childPredicate)
    {
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
        if (KEYBIND_PIE_SETTINGS.contains(config))
        {
            return KEYBIND_WHEEL_EXPANDED.getBooleanValue();
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
        FREE_CAMERA_CONTAINER_INTERACTION.setBooleanValue(false);
        FREE_CAMERA_ITEM_USE.setBooleanValue(false);
        FREE_CAMERA_MINING.setBooleanValue(false);
        FREE_CAMERA_LITEMATICA_EASY_PLACE.setBooleanValue(false);
        FREE_CAMERA_INTERACTION.setBooleanValue(false);
        DISABLE_FLUID_RENDERING.setBooleanValue(false);
        DISABLE_NON_SOURCE_FLUID_RENDERING.setBooleanValue(false);
        ENTITY_RENDER_AGGREGATION.setBooleanValue(false);
        FAST_WORLD_LOADING_SCREEN.setBooleanValue(false);
        FAST_RESOURCE_PACK_LOADING_SCREEN.setBooleanValue(false);
        BETTER_SAVED_HOTBARS.setBooleanValue(false);
        COOLDOWN_AUTO_ATTACK.setBooleanValue(false);
        DRAGGABLE_LISTS.setBooleanValue(false);
        KEYBIND_PIE_MENU.setBooleanValue(false);
        CLICK_AND_SEND.setBooleanValue(false);
        MAP_IN_SLOT.setBooleanValue(false);
        SERVER_ICON_CACHE.setBooleanValue(false);
        TOAST_KILLER.setBooleanValue(false);
        SERVER_PINGER_FIX.setBooleanValue(false);
        CONTINGAME_IME.setBooleanValue(false);
        CONDENSED_CREATIVE.setBooleanValue(false);
        KEEP_MOD_MENU_SCROLL.setBooleanValue(false);
        ITEM_SEARCH_HISTORY.setBooleanValue(false);
        ITEM_MANAGER_RECIPE_HISTORY.setBooleanValue(false);
    }
}
