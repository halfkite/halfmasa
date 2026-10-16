package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ItemDespawnDataSource implements IConfigOptionListEntry
{
    AUTO("auto"), SERVUX("servux"), ESTIMATE("estimate");

    private final String value;
    ItemDespawnDataSource(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.item_despawn_source." + this.value); }
    @Override public ItemDespawnDataSource cycle(boolean forward)
    {
        return values()[(ordinal() + (forward ? 1 : values().length - 1)) % values().length];
    }
    @Override public ItemDespawnDataSource fromString(String value)
    {
        for (var entry : values()) if (entry.value.equalsIgnoreCase(value)) return entry;
        return AUTO;
    }
}
//#endif
