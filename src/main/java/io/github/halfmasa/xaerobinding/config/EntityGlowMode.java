package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum EntityGlowMode implements IConfigOptionListEntry
{
    ALL("all"), SINGLE("single");

    private final String value;

    EntityGlowMode(String value) { this.value = value; }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.entity_glow_mode." + this.value); }
    @Override public EntityGlowMode cycle(boolean forward) { return this == ALL ? SINGLE : ALL; }
    @Override public EntityGlowMode fromString(String value)
    {
        return "single".equalsIgnoreCase(value) ? SINGLE : ALL;
    }
}
//#endif
