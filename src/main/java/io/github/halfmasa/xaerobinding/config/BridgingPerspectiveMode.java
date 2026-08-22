package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BridgingPerspectiveMode implements IConfigOptionListEntry
{
    AUTO("auto"),
    PLAYER_EYES("player_eyes"),
    CAMERA("camera");

    private final String value;

    BridgingPerspectiveMode(String value)
    {
        this.value = value;
    }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.bridging_perspective." + this.value); }
    @Override public BridgingPerspectiveMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }
    @Override public BridgingPerspectiveMode fromString(String value)
    {
        for (BridgingPerspectiveMode mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return AUTO;
    }
}
