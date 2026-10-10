//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum SchematicBlockFilterMode implements IConfigOptionListEntry
{
    BLACKLIST("blacklist"), WHITELIST("whitelist");

    private final String value;
    SchematicBlockFilterMode(String value) { this.value = value; }
    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.schematic_block_filter." + value); }
    @Override public SchematicBlockFilterMode cycle(boolean forward)
    {
        return this == BLACKLIST ? WHITELIST : BLACKLIST;
    }
    @Override public SchematicBlockFilterMode fromString(String value)
    {
        return "whitelist".equalsIgnoreCase(value) ? WHITELIST : BLACKLIST;
    }
}
//#endif
