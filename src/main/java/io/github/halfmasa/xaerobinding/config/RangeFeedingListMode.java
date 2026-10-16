package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum RangeFeedingListMode implements IConfigOptionListEntry
{
    NONE("none"), BLACKLIST("blacklist"), WHITELIST("whitelist");

    private final String value;

    RangeFeedingListMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.range_feeding_list." + this.value); }
    @Override public RangeFeedingListMode cycle(boolean forward)
    {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }
    @Override public RangeFeedingListMode fromString(String value)
    {
        for (var mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return NONE;
    }
}
//#endif
