package io.github.halfmasa.xaerobinding.better_multiplayer;

import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class BetterMultiplayerKeybinds {
    public static void register() {
        for (var config : BetterMultiplayerConfig.HOTKEYS) {
            config.getKeybind().setCallback((action, key) -> {
                if (action != KeyAction.PRESS) return false;
                if (config instanceof ConfigBooleanHotkeyed flag) flag.setBooleanValue(!flag.getBooleanValue());
                else if (config == BetterMultiplayerConfig.CYCLE_DETECTION) cycle(BetterMultiplayerConfig.DETECTION_MODE);
                else if (config == BetterMultiplayerConfig.CYCLE_POSITION) cycle(BetterMultiplayerConfig.ADDRESS_POSITION);
                new Configs().save();
                return true;
            });
        }
        BetterMultiplayerConfig.CYCLE_DETECTION.setAction(() -> { cycle(BetterMultiplayerConfig.DETECTION_MODE); return true; });
        BetterMultiplayerConfig.CYCLE_POSITION.setAction(() -> { cycle(BetterMultiplayerConfig.ADDRESS_POSITION); return true; });
    }
    private static void cycle(ConfigOptionList option) { option.setOptionListValue(option.getOptionListValue().cycle(true)); }
}
