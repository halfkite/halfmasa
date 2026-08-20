package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum EntityLabelPosition implements IConfigOptionListEntry
{
    TOP("top"),
    SIDE("side");

    private final String value;

    EntityLabelPosition(String value) { this.value = value; }

    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.entity_label_position." + value); }
    @Override public EntityLabelPosition cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(ordinal() + offset) % values().length];
    }
    @Override public EntityLabelPosition fromString(String value)
    {
        for (EntityLabelPosition position : values()) if (position.value.equalsIgnoreCase(value)) return position;
        return TOP;
    }
}
