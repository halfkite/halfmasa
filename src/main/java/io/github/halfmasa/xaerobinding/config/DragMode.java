package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum DragMode implements IConfigOptionListEntry
{
    DISABLED("disabled"),
    REQUIRES_MODIFIER("requires_modifier"),
    ENABLED("enabled");

    private final String value;

    DragMode(String value) { this.value = value; }

    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.drag_mode." + value); }
    @Override public DragMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(ordinal() + offset) % values().length];
    }
    @Override public DragMode fromString(String value)
    {
        for (DragMode mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return DISABLED;
    }
}
