package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ImeStyle implements IConfigOptionListEntry
{
    MOD("mod"),
    VANILLA("vanilla");

    private final String value;

    ImeStyle(String value) { this.value = value; }

    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.ime_style." + value); }
    @Override public ImeStyle cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(ordinal() + offset) % values().length];
    }
    @Override public ImeStyle fromString(String value)
    {
        for (ImeStyle style : values()) if (style.value.equalsIgnoreCase(value)) return style;
        return VANILLA;
    }
}
