package io.github.halfmasa.xaerobinding.config;

//#if MC >= 26.3
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum KeymapLayout implements IConfigOptionListEntry
{
    STANDARD("104"), EXTENDED("122");
    private final String value;
    KeymapLayout(String value) { this.value = value; }
    public boolean isExtended() { return this == EXTENDED; }
    public static KeymapLayout of(boolean extended) { return extended ? EXTENDED : STANDARD; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.keymap_layout." + this.value); }
    @Override public KeymapLayout cycle(boolean forward) { return of(!this.isExtended()); }
    @Override public KeymapLayout fromString(String value) { return "122".equals(value) ? EXTENDED : STANDARD; }
}
//#endif
