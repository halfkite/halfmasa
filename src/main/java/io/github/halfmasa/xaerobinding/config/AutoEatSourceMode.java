package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AutoEatSourceMode implements IConfigOptionListEntry
{
    HOTBAR("hotbar"), INVENTORY("inventory"), QUICK_SHULKER("quick_shulker");
    private final String value;
    AutoEatSourceMode(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.auto_eat_source." + this.value); }
    @Override public AutoEatSourceMode cycle(boolean forward) { return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)]; }
    @Override public AutoEatSourceMode fromString(String value)
    {
        for (var mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return HOTBAR;
    }
}
//#endif
