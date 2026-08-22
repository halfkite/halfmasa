package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BridgingAxisOverride implements IConfigOptionListEntry
{
    SAME_AS_DEFAULT("same_as_default", null),
    HORIZONTAL("horizontal", BridgingAxisMode.HORIZONTAL),
    VERTICAL("vertical", BridgingAxisMode.VERTICAL),
    BOTH("both", BridgingAxisMode.BOTH);

    private final String value;
    private final BridgingAxisMode mode;

    BridgingAxisOverride(String value, BridgingAxisMode mode)
    {
        this.value = value;
        this.mode = mode;
    }

    public BridgingAxisMode resolve(BridgingAxisMode fallback)
    {
        return this.mode != null ? this.mode : fallback;
    }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.bridging_axis." + this.value); }
    @Override public BridgingAxisOverride cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }
    @Override public BridgingAxisOverride fromString(String value)
    {
        for (BridgingAxisOverride mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return SAME_AS_DEFAULT;
    }
}
