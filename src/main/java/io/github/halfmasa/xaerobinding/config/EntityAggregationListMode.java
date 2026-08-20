package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum EntityAggregationListMode implements IConfigOptionListEntry
{
    NONE("none"),
    WHITELIST("whitelist"),
    BLACKLIST("blacklist");

    private final String value;

    EntityAggregationListMode(String value) { this.value = value; }

    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.entity_aggregation_list_mode." + value); }
    @Override public EntityAggregationListMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(ordinal() + offset) % values().length];
    }
    @Override public EntityAggregationListMode fromString(String value)
    {
        for (EntityAggregationListMode mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return NONE;
    }
}
