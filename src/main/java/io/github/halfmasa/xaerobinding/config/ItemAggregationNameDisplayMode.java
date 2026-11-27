package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ItemAggregationNameDisplayMode implements IConfigOptionListEntry
{
    TARGETED("targeted"), HIDDEN("hidden"), ALWAYS("always");

    private final String value;

    ItemAggregationNameDisplayMode(String value) { this.value = value; }

    public boolean visible(boolean pointed) { return this == ALWAYS || this == TARGETED && pointed; }

    public static ItemAggregationNameDisplayMode fromCount(AggregationCountDisplayMode mode)
    {
        return switch (mode)
        {
            case ALWAYS -> ALWAYS;
            case TARGETED -> TARGETED;
            case HIDDEN -> HIDDEN;
        };
    }

    @Override public String getStringValue() { return this.value; }

    @Override public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.item_aggregation_name_display." + this.value);
    }

    @Override public ItemAggregationNameDisplayMode cycle(boolean forward)
    {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }

    @Override public ItemAggregationNameDisplayMode fromString(String value)
    {
        for (var mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return ALWAYS;
    }
}
