package io.github.halfmasa.xaerobinding.config;

//#if MC >= 26.3
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum KeymapBindingMode implements IConfigOptionListEntry
{
    VIRTUAL("virtual"), PHYSICAL("physical");
    private final String value;
    KeymapBindingMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.keymap_binding_mode." + this.value); }
    @Override public KeymapBindingMode cycle(boolean forward) { return this == VIRTUAL ? PHYSICAL : VIRTUAL; }
    @Override public KeymapBindingMode fromString(String value) { return "physical".equalsIgnoreCase(value) ? PHYSICAL : VIRTUAL; }
}
//#endif
