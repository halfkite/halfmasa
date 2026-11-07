package io.github.halfmasa.xaerobinding.better_multiplayer;

import java.util.List;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.config.ActionHotkey;

/** The feature's configuration and empty-by-default bindings share one registry. */
public final class BetterMultiplayerConfig {
    private static final String KEY = "halfmasa.config.multiplayer";
    private static final KeybindSettings MENU_KEYS = KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, false, true, false, true);
    public static final ConfigBooleanHotkeyed ENABLED = flag("betterMultiplayer", true);
    public static final ConfigBoolean EXPANDED = new ConfigBoolean("betterMultiplayerExpanded", false).apply(KEY);
    public static final ConfigBooleanHotkeyed ADDRESS = flag("multiplayerAddress", true);
    public static final ConfigBooleanHotkeyed AUTO_HIDE = flag("multiplayerAutoHide", true);
    public static final ConfigBooleanHotkeyed HIDE_UNKNOWN = flag("multiplayerHideUnknown", true);
    public static final ConfigBooleanHotkeyed AUTO_RESTORE = flag("multiplayerAutoRestore", true);
    public static final ConfigBooleanHotkeyed MANUAL_PRIVACY = flag("multiplayerManualPrivacy", false);
    public static final ConfigBooleanHotkeyed LATENCY = flag("multiplayerLatency", true);
    public static final ConfigBooleanHotkeyed FOLDERS = flag("multiplayerFolders", true);
    public static final ConfigBooleanHotkeyed COLLAPSED_ICONS = flag("multiplayerCollapsedIcons", true);
    public static final ConfigOptionList DETECTION_MODE = new ConfigOptionList("multiplayerDetectionMode", Mode.HYBRID).apply(KEY);
    public static final ConfigOptionList ADDRESS_POSITION = new ConfigOptionList("multiplayerAddressPosition", Position.BELOW_NAME).apply(KEY);
    public static final ActionHotkey CYCLE_DETECTION = new ActionHotkey("multiplayerCycleDetection", "", MENU_KEYS).applyTranslationKey(KEY);
    public static final ActionHotkey CYCLE_POSITION = new ActionHotkey("multiplayerCyclePosition", "", MENU_KEYS).applyTranslationKey(KEY);
    public static final ConfigString OBS_URL = new ConfigString("multiplayerObsUrl", "ws://127.0.0.1:4455").apply(KEY);
    public static final ConfigString OBS_PASSWORD = new ConfigString("multiplayerObsPassword", "").apply(KEY);
    public static final ConfigInteger GREEN_LIMIT = new ConfigInteger("multiplayerGreenLimit", 100, 0, 60000).apply(KEY);
    public static final ConfigInteger YELLOW_LIMIT = new ConfigInteger("multiplayerYellowLimit", 200, 0, 60000).apply(KEY);
    public static final ConfigInteger DETECTION_INTERVAL = new ConfigInteger("multiplayerDetectionInterval", 1000, 250, 10000).apply(KEY);
    public static final ConfigInteger PING_TIMEOUT = new ConfigInteger("multiplayerPingTimeout", 10000, 1000, 60000).apply(KEY);
    public static final List<IConfigBase> CHILDREN = List.of(ADDRESS, ADDRESS_POSITION, CYCLE_POSITION,
            AUTO_HIDE, DETECTION_MODE, CYCLE_DETECTION, HIDE_UNKNOWN, AUTO_RESTORE, MANUAL_PRIVACY,
            LATENCY, GREEN_LIMIT, YELLOW_LIMIT, FOLDERS,
            COLLAPSED_ICONS,
            OBS_URL, OBS_PASSWORD, DETECTION_INTERVAL, PING_TIMEOUT);
    public static final List<IConfigBase> ALL = java.util.stream.Stream.concat(
            java.util.stream.Stream.of(ENABLED, EXPANDED), CHILDREN.stream()).toList();
    public static final List<IHotkey> HOTKEYS = ALL.stream().filter(IHotkey.class::isInstance).map(IHotkey.class::cast).toList();
    private static ConfigBooleanHotkeyed flag(String name, boolean value) {
        return new ConfigBooleanHotkeyed(name, value, "", MENU_KEYS).apply(KEY);
    }
    public static boolean active() { return ENABLED.getBooleanValue(); }
    public static boolean folders() { return active() && FOLDERS.getBooleanValue(); }
    public enum Mode implements IConfigOptionListEntry {
        HYBRID, OBS, PROCESS;
        public String getStringValue() { return name().toLowerCase(java.util.Locale.ROOT); }
        public String getDisplayName() { return StringUtils.translate("halfmasa.multiplayer.mode." + getStringValue()); }
        public Mode cycle(boolean forward) { return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)]; }
        public Mode fromString(String text) { for (Mode m : values()) if (m.getStringValue().equals(text)) return m; return HYBRID; }
    }
    public enum Position implements IConfigOptionListEntry {
        BELOW_NAME, BELOW_MOTD;
        public String getStringValue() { return name().toLowerCase(java.util.Locale.ROOT); }
        public String getDisplayName() { return StringUtils.translate("halfmasa.multiplayer.position." + getStringValue()); }
        public Position cycle(boolean forward) { return this == BELOW_NAME ? BELOW_MOTD : BELOW_NAME; }
        public Position fromString(String text) { return BELOW_MOTD.getStringValue().equals(text) ? BELOW_MOTD : BELOW_NAME; }
    }
}
