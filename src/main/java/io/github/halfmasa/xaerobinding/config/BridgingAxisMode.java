package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.core.Direction;

public enum BridgingAxisMode implements IConfigOptionListEntry
{
    HORIZONTAL("horizontal"),
    VERTICAL("vertical"),
    BOTH("both");

    private final String value;

    BridgingAxisMode(String value)
    {
        this.value = value;
    }

    public boolean allows(Direction direction)
    {
        return this == BOTH || (direction.getAxis().isVertical() ? this == VERTICAL : this == HORIZONTAL);
    }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.bridging_axis." + this.value); }
    @Override public BridgingAxisMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }
    @Override public BridgingAxisMode fromString(String value)
    {
        for (BridgingAxisMode mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return BOTH;
    }
}
