package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AggregationCountDisplayMode implements IConfigOptionListEntry
{
    ALWAYS("always"),
    TARGETED("targeted"),
    HIDDEN("hidden");

    private final String value;

    AggregationCountDisplayMode(String value) { this.value = value; }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.aggregation_count_display." + this.value);
    }

    @Override public AggregationCountDisplayMode cycle(boolean forward)
    {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }

    @Override public AggregationCountDisplayMode fromString(String value)
    {
        for (AggregationCountDisplayMode mode : values())
        {
            if (mode.value.equalsIgnoreCase(value)) return mode;
        }
        return ALWAYS;
    }
}
