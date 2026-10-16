package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AutoEatFoodPriority implements IConfigOptionListEntry
{
    SMART("smart"), LEFT_TO_RIGHT("left_to_right"), HIGH_NUTRITION("high_nutrition");
    private final String value;
    AutoEatFoodPriority(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.auto_eat_priority." + this.value); }
    @Override public AutoEatFoodPriority cycle(boolean forward) { return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)]; }
    @Override public AutoEatFoodPriority fromString(String value)
    {
        for (var mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return SMART;
    }
}
//#endif
