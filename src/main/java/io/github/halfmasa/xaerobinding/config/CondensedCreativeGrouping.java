package io.github.halfmasa.xaerobinding.config;

//#if MC >= 26.3
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CondensedCreativeGrouping implements IConfigOptionListEntry
{
    DEFAULT("default"), MATERIAL("material");

    private final String value;

    CondensedCreativeGrouping(String value) { this.value = value; }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.condensed_creative_grouping." + this.value);
    }
    @Override public CondensedCreativeGrouping cycle(boolean forward)
    {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }
    @Override public CondensedCreativeGrouping fromString(String value)
    {
        for (var entry : values()) if (entry.value.equalsIgnoreCase(value)) return entry;
        return DEFAULT;
    }
}
//#endif
