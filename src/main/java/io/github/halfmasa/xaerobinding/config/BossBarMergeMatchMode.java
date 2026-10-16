package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BossBarMergeMatchMode implements IConfigOptionListEntry
{
    NAME_STYLE("name_style"), NAME("name");

    private final String value;
    BossBarMergeMatchMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.boss_bar_match." + this.value); }
    @Override public BossBarMergeMatchMode cycle(boolean forward) { return this == NAME_STYLE ? NAME : NAME_STYLE; }
    @Override public BossBarMergeMatchMode fromString(String value)
    {
        return "name".equalsIgnoreCase(value) ? NAME : NAME_STYLE;
    }
}
//#endif
