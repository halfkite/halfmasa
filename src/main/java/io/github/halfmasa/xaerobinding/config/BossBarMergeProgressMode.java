package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BossBarMergeProgressMode implements IConfigOptionListEntry
{
    AVERAGE("average"), MIN("min"), MAX("max");

    private final String value;
    BossBarMergeProgressMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.boss_bar_progress." + this.value); }
    @Override public BossBarMergeProgressMode cycle(boolean forward)
    {
        return values()[(ordinal() + (forward ? 1 : values().length - 1)) % values().length];
    }
    @Override public BossBarMergeProgressMode fromString(String value)
    {
        for (var mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return AVERAGE;
    }
}
//#endif
