package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BridgingAdjacencyMode implements IConfigOptionListEntry
{
    STRICT("strict", 0),
    FACES("faces", 1),
    EDGES("edges", 2),
    CORNERS("corners", 3);

    private final String value;
    private final int maximumChangedAxes;

    BridgingAdjacencyMode(String value, int maximumChangedAxes)
    {
        this.value = value;
        this.maximumChangedAxes = maximumChangedAxes;
    }

    public int getMaximumChangedAxes()
    {
        return this.maximumChangedAxes;
    }

    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.bridging_adjacency." + this.value); }
    @Override public BridgingAdjacencyMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }
    @Override public BridgingAdjacencyMode fromString(String value)
    {
        for (BridgingAdjacencyMode mode : values()) if (mode.value.equalsIgnoreCase(value)) return mode;
        return CORNERS;
    }
}
