package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum KeybindSelectionLayout implements IConfigOptionListEntry
{
    WHEEL("wheel"),
    LIST("list");

    private final String value;

    KeybindSelectionLayout(String value)
    {
        this.value = value;
    }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.keybind_selection_layout." + this.value);
    }
    @Override public KeybindSelectionLayout cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }
    @Override public KeybindSelectionLayout fromString(String value)
    {
        for (KeybindSelectionLayout layout : values())
        {
            if (layout.value.equalsIgnoreCase(value)) return layout;
        }
        return WHEEL;
    }
}
