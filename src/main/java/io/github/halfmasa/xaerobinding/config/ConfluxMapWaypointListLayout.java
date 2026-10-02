package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ConfluxMapWaypointListLayout implements IConfigOptionListEntry
{
    SIDE_BY_SIDE("side_by_side"),
    STACKED("stacked");

    private final String value;

    ConfluxMapWaypointListLayout(String value)
    {
        this.value = value;
    }

    @Override
    public String getStringValue()
    {
        return this.value;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.conflux_map_waypoint_list_layout." + this.value);
    }

    @Override
    public ConfluxMapWaypointListLayout cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }

    @Override
    public ConfluxMapWaypointListLayout fromString(String value)
    {
        for (ConfluxMapWaypointListLayout layout : values())
        {
            if (layout.value.equalsIgnoreCase(value))
            {
                return layout;
            }
        }

        return SIDE_BY_SIDE;
    }
}
