package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AutoEatFilterMode implements IConfigOptionListEntry
{
    WHITELIST("whitelist"), BLACKLIST("blacklist");
    private final String value;
    AutoEatFilterMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.auto_eat_filter." + this.value); }
    @Override public AutoEatFilterMode cycle(boolean forward) { return this == WHITELIST ? BLACKLIST : WHITELIST; }
    @Override public AutoEatFilterMode fromString(String value) { return "blacklist".equalsIgnoreCase(value) ? BLACKLIST : WHITELIST; }
}
//#endif
